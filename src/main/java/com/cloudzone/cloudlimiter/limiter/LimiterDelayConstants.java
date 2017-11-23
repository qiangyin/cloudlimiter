package com.cloudzone.cloudlimiter.limiter;

/**
 * LimiterDelayConstants 执行频率
 * ONCE_PER_MINUTE：表示一分钟执行一次
 *
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2016/6/28
 */
public final class LimiterDelayConstants {
    private LimiterDelayConstants() {
    }

    public static final Double ONCE_PER_SECOND = 1D;
    public static final Double ONCE_PER_MINUTE = 1D / 60;
    public static final Double ONCE_PER_HOUR = 1D / 60 / 60;
    public static final Double ONCE_PER_DAY = 1D / 6 / 24;
    public static final Double ONCE_PER_WEEK = 1D / 6 / 24 / 7;
    public static final Double ONCE_PER_MONTH = 1D / 6 / 24 / 30;
}