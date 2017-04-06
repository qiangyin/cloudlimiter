package com.cloudzone.cloudlimiter.meter;

import com.cloudzone.cloudlimiter.base.AcquireStatus;
import com.cloudzone.cloudlimiter.base.IntervalModel;
import com.cloudzone.cloudlimiter.base.MeterListenner;
import com.cloudzone.cloudlimiter.factory.CloudFactory;
import com.cloudzone.cloudlimiter.limiter.CloudTicker;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/6
 */
public class CloudFactoryTest {
    @Test
    public void createCloudMeter() throws Exception {
        CloudMeterService cloudMeter = CloudFactory.createCloudMeter();
        cloudMeter.setIntervalModel(IntervalModel.ALL);
        cloudMeter.registerListener(new MeterListenner() {
            @Override
            public AcquireStatus acquireStats(List<Meterinfo> meterinfos) {
                if (meterinfos.size() > 0) {
                    System.out.println(meterinfos);
                }
                return AcquireStatus.ACQUIRE_SUCCESS;
            }
        });

        for (int i = 0; i < 10000; i++) {
            CloudTicker.sleepMicros(1);
            cloudMeter.request();
        }
    }

    @Test
    public void createCloudMeterAcquireLater() throws Exception {
        CloudMeterService cloudMeter = CloudFactory.createCloudMeter();
        cloudMeter.setIntervalModel(IntervalModel.ALL);
        cloudMeter.registerListener(new MeterListenner() {
            final AtomicInteger receiveNum = new AtomicInteger(0);

            @Override
            public AcquireStatus acquireStats(List<Meterinfo> meterinfos) {
                receiveNum.addAndGet(meterinfos.size());
                if (meterinfos.size() > 0) {
                    // 模拟第10-20条数据重复消费
                    if (receiveNum.get() > 10 && receiveNum.get() < 20) {
                        for (Meterinfo info : meterinfos) {
                            System.out.println("REACQUIRE_LATER" + info);
                        }
                        return AcquireStatus.REACQUIRE_LATER;
                    }
                    for (Meterinfo info : meterinfos) {
                        System.out.println(info);
                    }
                }

                return AcquireStatus.ACQUIRE_SUCCESS;
            }
        });
        for (int i = 0; i < 10000; i++) {
            CloudTicker.sleepSeconds(1);
            cloudMeter.request();
        }
    }
}