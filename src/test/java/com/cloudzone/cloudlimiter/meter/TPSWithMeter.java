package com.cloudzone.cloudlimiter.meter;

import com.cloudzone.cloudlimiter.base.AcquireStatus;
import com.cloudzone.cloudlimiter.base.IntervalModel;
import com.cloudzone.cloudlimiter.base.MeterListener;
import com.cloudzone.cloudlimiter.factory.CloudFactory;
import com.cloudzone.cloudlimiter.limiter.RealTimeLimiter;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.cloudzone.cloudlimiter.base.AcquireStatus.ACQUIRE_SUCCESS;

/**
 * @author tantexian<my.oschina.net/tantexian>
 * @since 2017/4/27
 */
public class TPSWithMeter {

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
                for (Meterinfo meterInfo : meterinfos) {
                    System.out.println(meterInfo);
                }
                return ACQUIRE_SUCCESS;
            }
        });

        CloudMeter cloudMeter2 = CloudFactory.createCloudMeter();
        cloudMeter2.setIntervalModel(IntervalModel.ALL);
        cloudMeter2.registerListener(new MeterListener() {
            @Override
            public AcquireStatus acquireStats(List<Meterinfo> meterinfos) {
                for (Meterinfo meterInfo : meterinfos) {
                    System.out.println("2:" + meterInfo);
                }
                return ACQUIRE_SUCCESS;
            }
        });

        for (int i = 0; i < 10000000; i++) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            int min = calendar.get(Calendar.MINUTE);
            int minMod = min % 10;
            double rate;
            // 1,2,3 设置为200/min
            // 4,5,6 设置为500/min
            // 7,8,9,0 设置为1000/min
            // 由于TPS限制时间，和meter统计时间不在同一个时间点取值，因此会出现某些数据误差
            // 此处设置可以保证，第0,1,2,4,5,6,8 结尾的秒的数据一定是准确的
            double howManySeconds = 60D;
            if (minMod >= 0 && minMod <= 3) {
                rate = 200D / howManySeconds;
            } else if (minMod >= 4 && minMod <= 7) {
                rate = 500D / howManySeconds;
            } else {
                rate = 1000D / howManySeconds;
            }

            if (rate != realTimeLimiter.getRate()) {
                realTimeLimiter.setRate(rate);
                System.out.println(min + "s -> setRate == " + rate + " " + (rate * 60L) + "/minute");
            }
            realTimeLimiter.acquire();
            cloudMeter.request();
            cloudMeter2.request();
            input.addAndGet(1);
        }

        cloudMeter.shutdown();

        System.out.println("input == " + input + " meterResultSec == " + meterResultSec + "  meterResultMin == " + meterResultMin);
    }
}
