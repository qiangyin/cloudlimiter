package com.cloudzone.cloudlimiter;

import com.cloudzone.cloudlimiter.benchmark.BenchMark;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author tantexian
 * @since 2017/3/30
 */
public class TPSTest {
    final int threadCount = 10;
    final ExecutorService sendThreadPool = Executors.newFixedThreadPool(threadCount);

    @Test
    public void testBenchMark() {
        final BenchMark benchMark = new BenchMark();


        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    for (int i = 0; i < threadCount; i++) {
                        sendThreadPool.execute(new Runnable() {
                            @Override
                            public void run() {
                                benchMark.statisticsStart();
                                timesOfsecond(1000); // send()
                                benchMark.statisticsEnd();

                            }
                        });

                    }
                }
            }
        }).start();


        while (true) {
            benchMark.getStats();
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }

    public static void timesOfsecond(int times) {
        try {
            Thread.sleep(1000 / times);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

