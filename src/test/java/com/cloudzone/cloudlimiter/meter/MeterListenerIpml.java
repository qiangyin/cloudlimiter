package com.cloudzone.cloudlimiter.meter;

import com.cloudzone.cloudlimiter.base.AcquireStatus;
import com.cloudzone.cloudlimiter.base.MeterListener;

import java.util.List;

/**
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/12
 */
public class MeterListenerIpml implements MeterListener {
    @Override
    public AcquireStatus acquireStats(List<Meterinfo> meterinfos) {
        for (Meterinfo info : meterinfos) {
            System.out.println(info);
        }
        return AcquireStatus.ACQUIRE_SUCCESS;
    }
}
