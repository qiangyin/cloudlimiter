package com.cloudzone.cloudlimiter.meter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author tantexian<my.oschina.net/tantexian>
 * @since 2017/4/2
 */
public class CloudMeter {
    private final AtomicLong Tps = new AtomicLong(0L);
}
