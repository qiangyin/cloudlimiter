package com.cloudzone.cloudlimiter.factory;

import com.cloudzone.cloudlimiter.limiter.FlowLimiter;
import com.cloudzone.cloudlimiter.limiter.FlowUnit;
import com.cloudzone.cloudlimiter.limiter.RealTimeLimiter;

import java.util.concurrent.TimeUnit;

/**
 * @author tantexian<my.oschina.net/tantexian>
 * @since 2017/4/2
 */
public class CloudFactory {

    /**
     * 实时限制获取令牌许可个数（固定最多保留1秒钟内未及时消费的令牌许可数）
     */
    public static RealTimeLimiter createRealTimeLimiter(double permitsPerSecond) {
        return new RealTimeLimiter(permitsPerSecond);
    }

    /**
     * 默认保留1小时内未及时消费的令牌许可数
     * 考虑到真实业务场景，避免过大压力，最多只能支持保留1小时未及时消费的令牌许可数
     *
     * @param size:     限流数值大小
     * @param flowUnit: 限流单位BYTE/KB/MB/GB/TB/PB
     */
    public static FlowLimiter createFlowLimiter(long size, FlowUnit flowUnit) {
        return createFlowLimiterPerHour(size, flowUnit);
    }

    /**
     * 限制获取令牌许可个数（能够保留1秒钟内未及时消费的令牌许可数）
     *
     * @param size:     限流数值大小
     * @param flowUnit: 限流单位BYTE/KB/MB/GB/TB/PB
     */
    public static FlowLimiter createFlowLimiterPerSecond(long size, FlowUnit flowUnit) {
        return new FlowLimiter(size, flowUnit, TimeUnit.SECONDS.toSeconds(1));
    }

    /**
     * 限制获取令牌许可个数（能够保留1分钟内未及时消费的令牌许可数）
     *
     * @param size:     限流数值大小
     * @param flowUnit: 限流单位BYTE/KB/MB/GB/TB/PB
     */
    public static FlowLimiter createFlowLimiterPerMinute(long size, FlowUnit flowUnit) {
        return new FlowLimiter(size, flowUnit, TimeUnit.MINUTES.toSeconds(1));
    }

    /**
     * 限制获取令牌许可个数（能够保留1分钟内未及时消费的令牌许可数）
     * 考虑到真实业务场景，避免过大压力，最多只能支持保留1小时未及时消费的令牌许可数
     *
     * @param size:     限流数值大小
     * @param flowUnit: 限流单位BYTE/KB/MB/GB/TB/PB
     */
    public static FlowLimiter createFlowLimiterPerHour(long size, FlowUnit flowUnit) {
        return new FlowLimiter(size, flowUnit, TimeUnit.DAYS.toSeconds(1));
    }
}
