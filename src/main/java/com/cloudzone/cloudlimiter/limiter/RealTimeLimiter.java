package com.cloudzone.cloudlimiter.limiter;

/**
 * 创建RealTimeLimiter类型的CloudRateLimiter
 * RealTime允许保留maxBurstSeconds秒未消费的令牌许可。
 * 暂存一段时间内没有使用的令牌（即可以应对突发的令牌数）。
 * 通过平均速率和最后一次新增令牌的时间计算出下次新增令牌的时间的，
 * 另外CloudRateLimiter还提供了tryAcquire方法来进行无阻塞或可超时的令牌消费。
 *
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/1
 */
public class RealTimeLimiter extends CloudLimiter {
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

    // 可以保存未被消费的令牌许可个数的时间（单位：秒）默认值为：1.0
    final double maxBurstSeconds;

    public RealTimeLimiter(double permitsPerSecond) {
        super(CloudTicker.SleepingTicker.SYSTEM_TICKER);
        this.maxBurstSeconds = 1;
        super.setRate(permitsPerSecond);
    }

    @Override
    void doSetRate(double permitsPerSecond, double stableIntervalMicros) {
        /**
         * 由于maxBurstSeconds默认为1，因此只能保存一秒钟未被消费的令牌
         * 因此最大许可等于当前申请许可(如果该值maxBurstSeconds=5，那么峰值最大许可则为5倍申请许可)
         */
        double oldMaxPermits = this.maxPermits;
        maxPermits = maxBurstSeconds * permitsPerSecond;
        storedPermits = (oldMaxPermits == 0.0)
                ? 0.0 // initial state
                : storedPermits * maxPermits / oldMaxPermits;
        // 因为oldMaxPermits = this.maxPermits，所以storedPermits  ==  storedPermits
    }

    @Override
    long storedPermitsToWaitTime(double storedPermits, double permitsToTake) {
        return 0;
    }

    @Override
    public double acquire() {
        return super.acquire(1);
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
    @Override
    public double acquire(int permits) {
        return super.acquire(permits);
    }
}

