package com.cloudzone.cloudlimiter.base;

import com.cloudzone.cloudlimiter.meter.Meterinfo;

import java.util.List;

/**
 * MeterListenner 用于异步接收Meter统计数据.
 *
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/5
 */
public interface MeterListenner {
    AcquireStatus acquireStats(List<Meterinfo> meterinfos);
}
