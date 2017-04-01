package com.cloudzone.cloudlimiter.rate;

import java.util.concurrent.TimeUnit;

/**
 * CloudRateLimiter 令牌桶（线程安全的）
 *
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/1
 */
public abstract class CloudRateLimiter {
    /**
     * The underlying timer; used both to measure elapsed time and sleep as necessary. A separate
     * object to facilitate testing.
     */
    private final CloudTicker.SleepingTicker ticker;

    /**
     * The timestamp when the RateLimiter was created; used to avoid possible overflow/time-wrapping
     * errors.
     */
    private final long offsetNanos;

    /**
     * The currently stored permits.
     */
    double storedPermits;

    /**
     * The maximum number of stored permits.
     */
    double maxPermits;

    /**
     * The interval between two unit requests, at our stable rate. E.g., a stable rate of 5 permits
     * per second has a stable interval of 200ms.
     */
    volatile double stableIntervalMicros;

    private final Object mutex = new Object();

    /**
     * The time when the next request (no matter its size) will be granted. After granting a request,
     * this is pushed further in the future. Large requests push this further than small requests.
     */
    private long nextFreeTicketMicros = 0L; // could be either in the past or future

    private CloudRateLimiter(CloudTicker.SleepingTicker ticker) {
        this.ticker = ticker;
        this.offsetNanos = ticker.read();
    }


    /**
     * 创建一个Bursty类型的令牌桶
     *
     * @author tantexian, <my.oschina.net/tantexian>
     * @params permitsPerSecond 每一秒钟可以被获取的令牌许可数
     * @since 2017/4/1
     */
    public static CloudRateLimiter create(double permitsPerSecond) {
     /*
       * 默认的CloudRateLimiter令牌桶，可以保存在一秒钟未使用的令牌许可。
       * 这样可以避免如下请求分布不均匀的使用情景：
       * 如果每一秒钟的QPS限制为1，当前具有4个线程，同时调用acquire()；
       * T0 在第0秒
       * T1 在第1.05秒
       * T2 在第2秒
       * T3 在第3秒
       * 由于T1轻微的延迟（延迟了0.05秒），因此T2将不得不等到2.05秒时刻，T3也不得不等到底3.05秒钟。
     */
        return create(CloudTicker.SleepingTicker.SYSTEM_TICKER, permitsPerSecond);
    }

    /**
     * 此处函数不对外开放，创建一个Bursty类型的CloudRateLimiter
     *
     * @author tantexian, <my.oschina.net/tantexian>
     * @since 2017/4/1
     */
    static CloudRateLimiter create(CloudTicker.SleepingTicker ticker, double permitsPerSecond) {
        // 创建一个Bursty类型的CloudRateLimiter
        CloudRateLimiter cloudRateLimiter = new Bursty(ticker, 1.0 /* maxBurstSeconds */);
        // 设置每秒钟令牌许可发放数量
        cloudRateLimiter.setRate(permitsPerSecond);
        return cloudRateLimiter;
    }

    /**
     * Updates the stable rate of this {@code RateLimiter}, that is, the
     * {@code permitsPerSecond} argument provided in the factory method that
     * constructed the {@code RateLimiter}. Currently throttled threads will <b>not</b>
     * be awakened as a result of this invocation, thus they do not observe the new rate;
     * only subsequent requests will.
     * <p>
     * <p>Note though that, since each request repays (by waiting, if necessary) the cost
     * of the <i>previous</i> request, this means that the very next request
     * after an invocation to {@code setRate} will not be affected by the new rate;
     * it will pay the cost of the previous request, which is in terms of the previous rate.
     * <p>
     * <p>The behavior of the {@code RateLimiter} is not modified in any other way,
     * e.g. if the {@code RateLimiter} was configured with a warmup period of 20 seconds,
     * it still has a warmup period of 20 seconds after this method invocation.
     *
     * @param permitsPerSecond the new stable rate of this {@code RateLimiter}. Must be positive
     */
    public final void setRate(double permitsPerSecond) {
        if (permitsPerSecond <= 0.0 || Double.isNaN(permitsPerSecond)) {
            throw new RuntimeException("\"rate must be positive\"");
        }

        synchronized (mutex) {
            resync(readSafeMicros());
            double stableIntervalMicros = TimeUnit.SECONDS.toMicros(1L) / permitsPerSecond;
            this.stableIntervalMicros = stableIntervalMicros;
            doSetRate(permitsPerSecond, stableIntervalMicros);
        }
    }

    private void resync(long nowMicros) {
        // if nextFreeTicket is in the past, resync to now
        if (nowMicros > nextFreeTicketMicros) {
            storedPermits = Math.min(maxPermits,
                    storedPermits + (nowMicros - nextFreeTicketMicros) / stableIntervalMicros);
            nextFreeTicketMicros = nowMicros;
        }
    }

    private long readSafeMicros() {
        return TimeUnit.NANOSECONDS.toMicros(ticker.read() - offsetNanos);
    }

    /**
     * Acquires a single permit from this {@code RateLimiter}, blocking until the
     * request can be granted. Tells the amount of time slept, if any.
     * <p>
     * <p>This method is equivalent to {@code acquire(1)}.
     *
     * @return time spent sleeping to enforce rate, in seconds; 0.0 if not rate-limited
     * @since 16.0 (present in 13.0 with {@code void} return type})
     */
    public double acquire() {
        return acquire(1);
    }

    /**
     * Acquires the given number of permits from this {@code RateLimiter}, blocking until the
     * request can be granted. Tells the amount of time slept, if any.
     *
     * @param permits the number of permits to acquire
     * @return time spent sleeping to enforce rate, in seconds; 0.0 if not rate-limited
     * @since 16.0 (present in 13.0 with {@code void} return type})
     */
    public double acquire(int permits) {
        long microsToWait = reserve(permits);
        ticker.sleepMicros(microsToWait);
        System.out.println("storedPermits == " + this.storedPermits + "  maxPermits == " + this.maxPermits);
        return 1.0 * microsToWait / TimeUnit.SECONDS.toMicros(1L);
    }

    /**
     * Reserves the given number of permits from this {@code RateLimiter} for future use, returning
     * the number of microseconds until the reservation can be consumed.
     *
     * @return time in microseconds to wait until the resource can be acquired.
     */
    long reserve(int permits) {
        checkPermits(permits);
        synchronized (mutex) {
            return reserveNextTicket(permits, readSafeMicros());
        }
    }

    private static void checkPermits(int permits) {
        if (permits <= 0) {
            throw new RuntimeException("Requested permits must be positive");
        }
    }

    /**
     * Reserves next ticket and returns the wait time that the caller must wait for.
     * <p>
     * <p>The return value is guaranteed to be non-negative.
     */
    private long reserveNextTicket(double requiredPermits, long nowMicros) {
        resync(nowMicros);
        long microsToNextFreeTicket = Math.max(0, nextFreeTicketMicros - nowMicros);
        double storedPermitsToSpend = Math.min(requiredPermits, this.storedPermits);
        double freshPermits = requiredPermits - storedPermitsToSpend;

        long waitMicros = storedPermitsToWaitTime(this.storedPermits, storedPermitsToSpend)
                + (long) (freshPermits * stableIntervalMicros);

        this.nextFreeTicketMicros = nextFreeTicketMicros + waitMicros;
        this.storedPermits -= storedPermitsToSpend;
        return microsToNextFreeTicket;
    }

    /**
     * Returns the stable rate (as {@code permits per seconds}) with which this
     * {@code RateLimiter} is configured with. The initial value of this is the same as
     * the {@code permitsPerSecond} argument passed in the factory method that produced
     * this {@code RateLimiter}, and it is only updated after invocations
     * to {@linkplain #setRate}.
     */
    public final double getRate() {
        return TimeUnit.SECONDS.toMicros(1L) / stableIntervalMicros;
    }

    abstract void doSetRate(double permitsPerSecond, double stableIntervalMicros);

    /**
     * Translates a specified portion of our currently stored permits which we want to
     * spend/acquire, into a throttling time. Conceptually, this evaluates the integral
     * of the underlying function we use, for the range of
     * [(storedPermits - permitsToTake), storedPermits].
     * <p>
     * This always holds: {@code 0 <= permitsToTake <= storedPermits}
     */
    abstract long storedPermitsToWaitTime(double storedPermits, double permitsToTake);

    /**
     * 创建Bursty类型的CloudRateLimiter
     *
     * @author tantexian, <my.oschina.net/tantexian>
     * @since 2017/4/1
     */
    private static class Bursty extends CloudRateLimiter {
        /**
         * http://blog.csdn.net/g_hongjin/article/details/51649246
         * SmoothBursty通过平均速率和最后一次新增令牌的时间计算出下次新增令牌的时间的，
         * 另外需要一个桶暂存一段时间内没有使用的令牌（即可以突发的令牌数）。
         * 另外RateLimiter还提供了tryAcquire方法来进行无阻塞或可超时的令牌消费。
         *
         */

        /**
         * 可以保存未被消费的令牌许可个数的时间（单位：秒）
         * 默认值为：1.0
         */
        final double maxBurstSeconds;

        Bursty(CloudTicker.SleepingTicker ticker, double maxBurstSeconds) {
            super(ticker);
            this.maxBurstSeconds = maxBurstSeconds;
        }

        @Override
        void doSetRate(double permitsPerSecond, double stableIntervalMicros) {
            /**
             * 由于maxBurstSeconds默认为1，因此只能保存一秒钟未被消费的令牌
             * 因此最大许可等于当前申请许可
             */
            double oldMaxPermits = this.maxPermits;
            maxPermits = maxBurstSeconds * permitsPerSecond;
            storedPermits = (oldMaxPermits == 0.0)
                    ? 0.0 // initial state
                    : storedPermits * maxPermits / oldMaxPermits;
        }

        @Override
        long storedPermitsToWaitTime(double storedPermits, double permitsToTake) {
            return 0;
        }
    }

    // warmupPeriod:从冷启动速率过渡到平均速率的时间间隔
}
