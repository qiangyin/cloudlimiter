package com.cloudzone.cloudlimiter.base;

/**
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/5
 */
public enum AcquireStatus {
    /**
     * Success Acquire
     */
    ACQUIRE_SUCCESS,
    /**
     * Failure consumption,later try to Acquire
     */
    REACQUIRE_LATER;
}
