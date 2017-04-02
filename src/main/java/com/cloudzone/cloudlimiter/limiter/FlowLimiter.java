package com.cloudzone.cloudlimiter.limiter;


/**
 * 创建FlowLimiter流量控制类型的CloudRateLimiter
 *
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/2
 */
public class FlowLimiter extends CloudLimiter {

    // 可以保存未被消费的令牌许可个数的时间（单位：秒
    final double maxBurstSeconds;


    public FlowLimiter(long size, FlowUnit flowUnit, double maxBurstSeconds) {
        super(CloudTicker.SleepingTicker.SYSTEM_TICKER);
        this.maxBurstSeconds = maxBurstSeconds;
        this.setRate(size, flowUnit);
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

    public double acquire(long size, FlowUnit flowUnit) {
        return super.acquire((int) flowUnit.toByte(size));
    }

    public void setRate(long size, FlowUnit flowUnit) {
        super.setRate(flowUnit.toByte(size));
    }

}
