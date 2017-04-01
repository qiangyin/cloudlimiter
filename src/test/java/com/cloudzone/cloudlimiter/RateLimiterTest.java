package com.cloudzone.cloudlimiter;

import com.cloudzone.cloudlimiter.meter.BenchMark;
import com.cloudzone.cloudlimiter.rate.CloudRateLimiter;
import com.cloudzone.cloudlimiter.rate.CloudTicker;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author tantexian
 * @since 2017/3/30
 */
public class RateLimiterTest {
    final int threadCount = 10;
    final ExecutorService sendThreadPool = Executors.newFixedThreadPool(threadCount);
    static AtomicLong atoNum = new AtomicLong(0);
    //final static GoogleCloudLimiter rateLimiter1 = GoogleCloudLimiter.create(1000);
    //final static GoogleCloudLimiter rateLimiter1 = GoogleCloudLimiter.create(1000);
    final static CloudRateLimiter rateLimiter1 = CloudRateLimiter.create(1000);
    final long start = System.currentTimeMillis();

    @Test
    public void testLimiter() {

        final BenchMark benchMark10 = new BenchMark();


        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    // rateLimiter1.acquire(1);
                    rateLimiter1.acquire();
                    //Double sec = cloudLimiter.acquire(1);
                    //System.out.println("sec == " + sec);
                    benchMark10.statisticsStart();
                    send();
                    benchMark10.statisticsEnd();

                }
            }
        }).start();

        while (true) {
            CloudTicker.sleepSeconds(1);
            benchMark10.getStats();
            long sec = (System.currentTimeMillis() - start) / 1000;
            long tps = 0;
            if (sec > 0) {
                tps = atoNum.get() / sec;

            }
            System.out.println("cloudLimiter.getRate() == " + rateLimiter1.getRate() + " TPS == " + tps + " num == " + atoNum.get() + " time == " + sec + "\n\n");
        }

    }

    @Test
    public void testCurrentLimiter() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            sendThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    testLimiter();
                }
            });
        }

        while (true) {
            long sec = (System.currentTimeMillis() - start) / 1000;
            long tps = 0;
            if (sec > 0) {
                tps = atoNum.get() / sec;

            }
            System.out.println("MyTPS == " + tps + " num == " + atoNum.get() + " time == " + sec);
            CloudTicker.sleepSeconds(1);
        }
    }

    public static void send() {
        long l = atoNum.addAndGet(1);
        if (l < 10) {

           /* try {
                System.out.println("--------------");
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
        } else if (l == 10000) {
            /*System.out.println("setRate(1000)------------------\n\n\n\n");
            rateLimiter1.setRate(10);*/
        }

    }
}
