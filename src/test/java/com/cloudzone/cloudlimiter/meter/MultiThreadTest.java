package com.cloudzone.cloudlimiter.meter;

import com.cloudzone.cloudlimiter.factory.CloudFactory;
import com.cloudzone.cloudlimiter.limiter.CloudTicker;
import com.cloudzone.cloudlimiter.limiter.RealTimeLimiter;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author tantexian<my.oschina.net/tantexian>
 * @since 2017/4/12
 */
public class MultiThreadTest {
    final static int threadNums = 10;
    final static ExecutorService executorService = Executors.newFixedThreadPool(threadNums);
    final static RealTimeLimiter limiter = CloudFactory.createRealTimeLimiter(100);
    // final static CloudMeter cloudMeter = CloudFactory.createCloudMeter();

    public static void main(String[] args) {
        for (int i = 0; i < threadNums; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    CloudMeter cloudMeter = CloudFactory.createCloudMeter();
                    while (true) {
//                        limiter.acquire();
                        CloudTicker.sleepMillis(10);
                        cloudMeter.registerListener(new MeterListenerIpml());

                        int index = (new Random()).nextInt(3);
                        cloudMeter.request("topicTag" + index);
                    }
                }
            });
        }

    }
}
