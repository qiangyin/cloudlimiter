package com.cloudzone.cloudlimiter.meter;

import com.cloudzone.cloudlimiter.base.AcquireStatus;
import com.cloudzone.cloudlimiter.base.IntervalModel;
import com.cloudzone.cloudlimiter.base.MeterListenner;
import com.cloudzone.cloudlimiter.factory.CloudFactory;
import com.cloudzone.cloudlimiter.limiter.CloudTicker;
import com.cloudzone.cloudlimiter.limiter.RealTimeLimiter;
import org.junit.Test;

import java.util.List;

/**
 * @author tantexian<my.oschina.net/tantexian>
 * @since 2017/4/4
 */
public class CloudMeterServiceTest {
    final static private RealTimeLimiter realTimeLimiter = CloudFactory.createRealTimeLimiter(1000);
    CloudMeterService cloudMeterService = new CloudMeterService();

    @Test
    public void printStats() throws Exception {

        cloudMeterService.setIntervalModel(IntervalModel.ALL);
        cloudMeterService.registerListener(new MeterListenner() {
            @Override
            public AcquireStatus acquireStats(List<Meterinfo> meterinfos) {
                for (Meterinfo info : meterinfos) {
                    System.out.println(info);
                }
                return AcquireStatus.ACQUIRE_SUCCESS;
            }
        });

        for (int i = 0; i < 1000000; i++) {
            /*if (i == 100) {
                realTimeLimiter.setRate(10);
                System.out.println("setRate(10)");
            } else if (i == 600) {
                realTimeLimiter.setRate(100);
                System.out.println("setRate(100)");
            } else if (i == 6000) {
                realTimeLimiter.setRate(1000);
                System.out.println("setRate(1000)");
            }
            realTimeLimiter.acquire();*/
            CloudTicker.sleepSeconds(1);
            cloudMeterService.request();

        }
    }

}