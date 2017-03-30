package com.cloudzone.cloudlimiter;

import com.google.common.util.concurrent.RateLimiter;

/**
 * @author tantexian
 * @since 2017/3/30
 */
public class RateLimiterTest {
    final RateLimiter rateLimiter = RateLimiter.create(1000);

    public static void main(String[] args) {

    }
}
