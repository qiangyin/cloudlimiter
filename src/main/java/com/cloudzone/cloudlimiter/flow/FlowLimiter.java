package com.cloudzone.cloudlimiter.flow;


import com.cloudzone.cloudlimiter.base.GoogleCloudLimiter;

/**
 * @author tantexian<my.oschina.net/tantexian>
 * @since 2017/3/30
 */
public class FlowLimiter {

    static GoogleCloudLimiter cloudLimiter;

    public static void limiterPerSecond(FlowType flowLimit) {
        cloudLimiter = GoogleCloudLimiter.create(flowLimit.value);
    }

    public static void limiterPerMinute(FlowType flowLimit) {
        cloudLimiter = GoogleCloudLimiter.create(flowLimit.value * Time.MINUTE);
    }

    public static void limiterPerHour(FlowType flowLimit) {
        cloudLimiter = GoogleCloudLimiter.create(flowLimit.value * Time.HOUR);
    }

    public static void limiterPerDay(FlowType flowLimit) {
        cloudLimiter = GoogleCloudLimiter.create(flowLimit.value * Time.DAY);
    }

    public static void limiterPerMonth(FlowType flowLimit) {
        cloudLimiter = GoogleCloudLimiter.create(flowLimit.value * Time.YEAT);
    }

    public void resetRatePerSecond(FlowType flowLimit) {
        cloudLimiter.setRate(flowLimit.value);
    }

    public void resetRatePerMinute(FlowType flowLimit) {
        cloudLimiter.setRate(flowLimit.value * Time.MINUTE);
    }

    public void resetRatePerHour(FlowType flowLimit) {
        cloudLimiter.setRate(flowLimit.value * Time.HOUR);
    }

    public void resetRatePerDay(FlowType flowLimit) {
        cloudLimiter.setRate(flowLimit.value * Time.DAY);
    }

    public void resetRatePerMonth(FlowType flowLimit) {
        cloudLimiter.setRate(flowLimit.value * Time.MONTH);
    }

    public void resetRatePerYear(FlowType flowLimit) {
        cloudLimiter.setRate(flowLimit.value * Time.YEAT);
    }

    private class Time {
        final static int SECOND = 1;
        final static int MINUTE = 60 * SECOND;
        final static int HOUR = 60 * MINUTE;
        final static int DAY = 24 * HOUR;
        final static int MONTH = 30 * DAY;
        final static int YEAT = 365 * MONTH;
    }
}
