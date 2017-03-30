package com.cloudzone.cloudlimiter;

import com.cloudzone.cloudlimiter.base.CloudLimiter;
import com.cloudzone.cloudlimiter.flow.FlowLimiter;
import com.cloudzone.cloudlimiter.flow.FlowType;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.cloudzone.cloudlimiter.TPSTest.sleepMillis;

/**
 * @author tantexian
 * @since 2017/3/30
 */
public class RateLimiterTest {
    final int threadCount = 10;
    final ExecutorService sendThreadPool = Executors.newFixedThreadPool(threadCount);
    static AtomicLong atoNum = new AtomicLong(0);
    final static RateLimiter rateLimiter = RateLimiter.create(1, 10, TimeUnit.SECONDS);
    final static CloudLimiter cloudLimiter = CloudLimiter.create(1000);


    @Test
    public void testLimiter() {
        long start = System.currentTimeMillis();

        while (true) {
            // rateLimiter.acquire(100);
            cloudLimiter.acquire();
            send();
            long sec = (System.currentTimeMillis() - start) / 1000;
            long tps = 0;
            if (sec > 0) {
                tps = atoNum.get() / sec;

            }
            System.out.println("cloudLimiter.getRate() == " + cloudLimiter.getRate() + " TPS == " + tps + " num == " + atoNum.get() + " time == " + sec);
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
            System.out.println("TPS == " + tps + " num == " + atoNum.get() + " time == " + sec);
            sleepMillis(1000);
        }
    }

    public static void send() {
        atoNum.addAndGet(1);
    }
}
