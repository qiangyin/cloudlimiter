package com.cloudzone.cloudlimiter.meter;

import com.cloudzone.cloudlimiter.base.AcquireStatus;
import com.cloudzone.cloudlimiter.base.FlowUnit;
import com.cloudzone.cloudlimiter.base.IntervalModel;
import com.cloudzone.cloudlimiter.base.MeterListener;
import com.cloudzone.cloudlimiter.factory.CloudFactory;
import com.cloudzone.cloudlimiter.limiter.FlowLimiter;
import com.cloudzone.cloudlimiter.limiter.LimiterDelayConstants;
import com.cloudzone.cloudlimiter.limiter.RealTimeLimiter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author yintongjiang
 * @params
 * @since 2017/4/11
 */
public class CLoudMeterMain {
    final static private RealTimeLimiter realTimeLimiter = CloudFactory.createRealTimeLimiter(10);
    final static private FlowLimiter realTimeLimiter1 = CloudFactory.createFlowLimiterPerSecond(100, FlowUnit.BYTE);
    final static CloudMeter cloudMeter = CloudFactory.createCloudMeter();


    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(100);
        cloudMeter.setIntervalModel(IntervalModel.MINUTE);
        cloudMeter.registerListener(new MeterListener() {
            @Override
            public AcquireStatus acquireStats(List<Meterinfo> meterinfos) {
                for (Meterinfo info : meterinfos) {
                    System.out.println(info);
                }
                return AcquireStatus.ACQUIRE_SUCCESS;
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                while (i < 10) {
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    i++;
                    if (i == 2) {
                        System.out.println("##########");
                        realTimeLimiter.setRate(LimiterDelayConstants.ONCE_PER_SECOND);
                    } else {
                        if (realTimeLimiter.getRate() == LimiterDelayConstants.ONCE_PER_SECOND) {
                            System.out.println("----------------");
                            realTimeLimiter.setRate(10);
                        }
                    }
                }
            }
        }).start();
        for (int i = 0; i < 1; i++) {
//                realTimeLimiter1.acquire(100,FlowUnit.BYTE);
            executor.submit(new TestThread());

        }
//        ExecutorService executor= Executors.newFixedThreadPool(10);
//        realTimeLimiter.setRate(10);
//        System.out.println("setRate(10)");
//        for (int i = 0; i < 600; i++) {
//            try {
//                TimeUnit.MILLISECONDS.sleep(700);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            executor.submit(new Runnable() {
//                @Override
//                public void run() {
//                    realTimeLimiter.acquire();
//                    cloudMeter.request();
//                    cloudMeter.request("mytag4");
//                    cloudMeter.request("mytag66");
//                    cloudMeter.request("mytag888");
//                }
//            });
//        }
//        executor.shutdown();
        cloudMeter.shutdown();
        cloudMeter.shutdown();
        cloudMeter.shutdown();
        cloudMeter.shutdown();
    }


    static class TestThread extends Thread {
        @Override
        public void run() {
            realTimeLimiter.acquire();
            realTimeLimiter1.acquire(10, FlowUnit.BYTE);
            cloudMeter.request("test", "tps");
            cloudMeter.request("test", "flow", 10);
        }
    }
}
