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

    /**
     * 推送统计信息
     * 注意：如果订阅了分钟模型的推送，则需要再2分钟之后才能打印出第一分钟的统计数值
     */
    AcquireStatus acquireStats(List<Meterinfo> meterinfos);
}
