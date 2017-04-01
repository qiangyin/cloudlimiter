package com.cloudzone.cloudlimiter;

import com.cloudzone.cloudlimiter.base.GoogleCloudLimiter;
import com.cloudzone.cloudlimiter.benchmark.BenchMark;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author tantexian
 * @since 2017/3/30
 */
public class TPSTest {
    final int threadCount = 1;
    final ExecutorService sendThreadPool = Executors.newFixedThreadPool(threadCount);
    static AtomicLong atoNum = new AtomicLong(0);
    final static GoogleCloudLimiter cloudLimiter = GoogleCloudLimiter.create(500);

    @Test
    public void testSingleBenchMark() {
        final BenchMark benchMark1 = new BenchMark();
        long starttime = System.currentTimeMillis();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    benchMark1.statisticsStart();
                    cloudLimiter.acquire();
                    atoNum.addAndGet(1);
                    //tpsOfsecond(1000); // send()
                    benchMark1.statisticsEnd();
                }
            }
        }).start();


        while (true) {
            benchMark1.getStats();
            long sec = (System.currentTimeMillis() - starttime) / 1000;
            long tps = 0;
            if (sec > 0) {
                tps = atoNum.get() / sec;
            }

            System.out.println("TPS ==  " + tps + " num == " + atoNum.get() + " time == " + sec);
            sleepMillis(1000);
        }


    }

    @Test
    public void testBenchMark() {
        final BenchMark benchMark = new BenchMark();
        long starttime = System.currentTimeMillis();

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < threadCount; i++) {
                    sendThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            while (true) {
                                benchMark.statisticsStart();
                                tpsOfsecond(10000); // send()
                                benchMark.statisticsEnd();
                            }

                        }
                    });


                }
            }
        }).start();


        while (true) {
            benchMark.getStats();
            long sec = (System.currentTimeMillis() - starttime) / 1000;
            long tps = 0;
            if (sec > 0) {
                tps = atoNum.get() / sec;
            }

            System.out.println("TPS == " + tps + " num == " + atoNum.get() + " time == " + sec);
            sleepMillis(1000);
        }


    }

    public static void tpsOfsecond(int times) {
        sleepMillis(1000 / times);
        atoNum.addAndGet(1);
    }

    public static void sleepMillis(long millis) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < millis) {
        }
    }
}
