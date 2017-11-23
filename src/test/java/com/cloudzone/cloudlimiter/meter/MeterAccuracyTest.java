package com.cloudzone.cloudlimiter.meter;

import com.cloudzone.cloudlimiter.base.AcquireStatus;
import com.cloudzone.cloudlimiter.base.IntervalModel;
import com.cloudzone.cloudlimiter.base.MeterListener;
import com.cloudzone.cloudlimiter.factory.CloudFactory;
import com.cloudzone.cloudlimiter.limiter.RealTimeLimiter;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.cloudzone.cloudlimiter.base.AcquireStatus.ACQUIRE_SUCCESS;

/**
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/14
 */
public class MeterAccuracyTest {

    public static void main(String[] args) {
        AtomicInteger input = new AtomicInteger(0);
        final AtomicLong meterResultSec = new AtomicLong(0);
        final AtomicLong meterResultMin = new AtomicLong(0);
        RealTimeLimiter realTimeLimiter = CloudFactory.createRealTimeLimiter(10);
        CloudMeter cloudMeter = CloudFactory.createCloudMeter();
        cloudMeter.setIntervalModel(IntervalModel.ALL);
        cloudMeter.registerListener(new MeterListener() {
            @Override
            public AcquireStatus acquireStats(List<Meterinfo> meterinfos) {
                for (Meterinfo info : meterinfos) {
                    if (info.getTimeUnitType().equals(TimeUnit.SECONDS)) {
                        meterResultSec.addAndGet(info.getRequestNum());
                    } else {
                        meterResultMin.addAndGet(info.getRequestNum());
                    }
                    System.out.println(info);
                }
                return ACQUIRE_SUCCESS;
            }
        });

        for (int i = 0; i < 1888; i++) {
            realTimeLimiter.acquire();
            cloudMeter.request();
            input.addAndGet(1);
        }

        cloudMeter.shutdown();

        System.out.println("input == " + input + " meterResultSec == " + meterResultSec + "  meterResultMin == " + meterResultMin);
    }
}
