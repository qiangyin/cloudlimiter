package com.cloudzone.cloudlimiter.meter;

import com.cloudzone.cloudlimiter.base.IntervalModel;
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
public class MultiThreadTpsTest {
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
                    cloudMeter.setIntervalModel(IntervalModel.ALL);
                    // cloudMeter.setAcquireMeterTopic("topicTag1");
                    while (true) {
                        //                        limiter.acquire();
                        CloudTicker.sleepMillis(100);
                        cloudMeter.registerListener(new MeterListenerIpml());

                        int index = (new Random()).nextInt(3);
                        String type = "producer";
                        if (index == 2) {
                            type = "consumer";
                        }
                        cloudMeter.request("topicTag" /*+ index*/, "producer");
                        cloudMeter.request("topicTag" /*+ index*/, "consumer");
                    }
                }
            });
        }

    }
}
