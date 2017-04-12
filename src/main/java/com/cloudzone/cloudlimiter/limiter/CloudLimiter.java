package com.cloudzone.cloudlimiter.limiter;

import java.util.concurrent.TimeUnit;

/**
 * CloudLimiter 令牌桶（线程安全的）
 *
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/1
 */
public abstract class CloudLimiter {
    /**
     * 用于获取当前花费时间及需要阻塞睡眠的时间。
     * The underlying timer; used both to measure elapsed time and sleep as necessary. A separate
     * object to facilitate testing.
     */
    private final CloudTicker.SleepingTicker ticker;

    /**
     * CloudRateLimiter创建开始到当前的时间戳
     * The timestamp when the CloudLimiter was created; used to avoid possible overflow/time-wrapping
     * errors.
     */
    private final long offsetNanos;

    /**
     * 当前保留的未消费完的令牌许可数。The currently stored permits.
     */
    double storedPermits;

    /**
     * 可以保留的最大令牌许可数
     * The maximum number of stored permits.
     */
    double maxPermits;

    /**
     * 两个令牌请求之间的固定时间间隔。例如：每秒申请5个许可，那么固定时间间隔200ms
     * The interval between two unit requests, at our stable rate. E.g., a stable rate of 5 permits
     * per second has a stable interval of 200ms.
     */
    volatile double stableIntervalMicros;

    // 全局对象锁
    private final Object mutex = new Object();

    /**
     * 下一次请求获取的时间（请求可以是包括一个令牌或者多个令牌）。
     * 该时间为与当前启动CloudRateLimiter的时间间隔
     * The time when the next request (no matter its size) will be granted. After granting a request,
     * this is pushed further in the future. Large requests push this further than small requests.
     */
    private long nextFreeTicketMicros = 0L; // could be either in the past or future

    /**
     * 私有构造函数，只允许当前静态子类调用
     */
    protected CloudLimiter(CloudTicker.SleepingTicker ticker) {
        this.ticker = ticker;
        // 获取当前时间戳
        this.offsetNanos = ticker.read();
    }

    /**
     * 设置新的令牌许可数发放比率（即每秒钟允许消费个数）
     * Updates the stable rate of this {@code CloudLimiter}, that is, the
     * {@code permitsPerSecond} argument provided in the factory method that
     * constructed the {@code CloudLimiter}. Currently throttled threads will <b>not</b>
     * be awakened as a result of this invocation, thus they do not observe the new rate;
     * only subsequent requests will.
     * <p>
     * <p>Note though that, since each request repays (by waiting, if necessary) the cost
     * of the <i>previous</i> request, this means that the very next request
     * after an invocation to {@code setRate} will not be affected by the new rate;
     * it will pay the cost of the previous request, which is in terms of the previous rate.
     * <p>
     * <p>The behavior of the {@code CloudLimiter} is not modified in any other way,
     * e.g. if the {@code RateLimiter} was configured with a warmup period of 20 seconds,
     * it still has a warmup period of 20 seconds after this method invocation.
     *
     * @param permitsPerSecond the new stable rate of this {@code CloudLimiter}. Must be positive
     */
    protected void setRate(double permitsPerSecond) {
        if (permitsPerSecond <= 0.0 || Double.isNaN(permitsPerSecond)) {
            throw new RuntimeException("\"rate must be positive\"");
        }

        synchronized (mutex) {
            // 同步下一次获取令牌时刻nextFreeTicketMicros值及同步计算可保留的剩余令牌数
            resync(readSafeMicros()); // readSafeMicros为获取当前时刻与CloudRateLimiter启动时刻的间隔时间微秒数
            // 计算两个令牌请求之间的固定时间间隔，并赋值
            double stableIntervalMicros = TimeUnit.SECONDS.toMicros(1L) / permitsPerSecond;
            this.stableIntervalMicros = stableIntervalMicros;
            // 调用子类实现
            doSetRate(permitsPerSecond, stableIntervalMicros);
        }
    }

    /**
     * 同步下一次获取令牌时刻nextFreeTicketMicros值及同步计算可保留的剩余令牌数
     * 如果当前时刻大于下一次获取令牌时刻nextFreeTicketMicros，
     * 则重新计算剩余令牌许可数，及赋值当前时刻给nextFreeTicketMicros
     */
    private void resync(long nowMicros) {
        // 如果nextFreeTicket是在过去时科，那么resync重新同步到当前时刻
        // nextFreeTicketMicros该时间为与当前启动CloudRateLimiter的时间间隔
        // if nextFreeTicket is in the past, resync to now
        if (nowMicros > nextFreeTicketMicros) {
            // 如果下一次可以获取令牌许可时间比当前时刻还早，那么则有多于未消费令牌
            // 则保存在storedPermits中，然后赋值当前时刻给nextFreeTicketMicros
            // storedPermits的值最大保留令牌许可数为maxPermits
            storedPermits = Math.min(maxPermits,
                    storedPermits + (nowMicros - nextFreeTicketMicros) / stableIntervalMicros);
            nextFreeTicketMicros = nowMicros;
        }
    }

    // 获取当前时刻与CloudRateLimiter启动时刻的间隔时间微秒数
    private long readSafeMicros() {
        return TimeUnit.NANOSECONDS.toMicros(ticker.read() - offsetNanos);
    }

    /**
     * 获取一个许可令牌（时间未到则阻塞）
     * Acquires a single permit from this {@code CloudLimiter}, blocking until the
     * request can be granted. Tells the amount of time slept, if any.
     * <p>
     * <p>This method is equivalent to {@code acquire(1)}.
     *
     * @return time spent sleeping to enforce rate, in seconds; 0.0 if not rate-limited
     * @since 16.0 (present in 13.0 with {@code void} return type})
     */
    protected double acquire() {
        return acquire(1);
    }

    /**
     * 获取指定数量的许可令牌（时间未到则阻塞）
     * Acquires the given number of permits from this {@code CloudLimiter}, blocking until the
     * request can be granted. Tells the amount of time slept, if any.
     *
     * @param permits the number of permits to acquire
     * @return time spent sleeping to enforce rate, in seconds; 0.0 if not rate-limited
     * @since 16.0 (present in 13.0 with {@code void} return type})
     */
    protected double acquire(int permits) {
        // 计算需要等待的微秒数
        long microsToWait = reserve(permits);
        // 阻塞等待microsToWait微秒
        ticker.sleepMicros(microsToWait);
        // 返回本次等待的时间（单位秒）
        return 1.0 * microsToWait / TimeUnit.SECONDS.toMicros(1L);
    }

    /**
     * 计算获取许可令牌需要等待的微秒数
     * Reserves the given number of permits from this {@code RateLimiter} for future use, returning
     * the number of microseconds until the reservation can be consumed.
     *
     * @return time in microseconds to wait until the resource can be acquired.
     */
    long reserve(int permits) {
        // 检测许可令牌数必须为正数
        checkPermits(permits);
        synchronized (mutex) {
            // 返回获取许可令牌需要等待的微秒数
            return reserveNextTicket(permits, readSafeMicros());// readSafeMicros为获取当前时刻与CloudRateLimiter启动时刻的间隔时间微秒数
        }
    }

    // 检测permits必须为正数
    private static void checkPermits(int permits) {
        if (permits < 0) {
            throw new RuntimeException("Requested permits must be positive");
        }
    }

    /**
     * 计算下一次请求调用必须等待的获取时间
     * Reserves next ticket and returns the wait time that the caller must wait for.
     * <p>
     * <p>The return value is guaranteed to be non-negative.
     */
    private long reserveNextTicket(double requiredPermits, long nowMicros) {
        // 同步下一次获取令牌时刻nextFreeTicketMicros值及同步计算可保留的剩余令牌数
        resync(nowMicros);
        // 获取未消费许可剩余时间间隔
        long microsToNextFreeTicket = Math.max(0, nextFreeTicketMicros - nowMicros);
        // 获取请求令牌许可数与保留许可数两者之间的最小值
        double storedPermitsToSpend = Math.min(requiredPermits, this.storedPermits);
        // 计算需要消费掉的新的令牌数（如果之前保留令牌数大于当前请求令牌数，那么则不需要使用新的令牌数，因此freshPermits=0）
        double freshPermits = requiredPermits - storedPermitsToSpend;

        // 如果为RealTime，那么storedPermitsToWaitTime==0
        // 因此waitMicros=freshPermits * stableIntervalMicros即需要消费掉的新的令牌数*令牌消费间隔
        long waitMicros = storedPermitsToWaitTime(this.storedPermits, storedPermitsToSpend)
                + (long) (freshPermits * stableIntervalMicros);

        // 计算下一次请求获取的时间，为当前此次与等待时间之和
        this.nextFreeTicketMicros = nextFreeTicketMicros + waitMicros;
        // 保留令牌数为保留数与本次消费数量之差
        this.storedPermits -= storedPermitsToSpend;
        // 返回下一次请求获取的时间
        return microsToNextFreeTicket;
    }

    /**
     * 计算每一秒钟发放的令牌许可数
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
}
