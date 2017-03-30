package com.cloudzone.cloudlimiter.benchmark;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.System.currentTimeMillis;

/**
 * @author tantexian
 * @since 2017/3/30
 */
public class BenchMark {
    final static Timer timer = new Timer("BenchMark", true);
    final static LinkedList<Long[]> snapshotList = new LinkedList<Long[]>();
    final static StatsBenchmark statsBenchmark = new StatsBenchmark();


    public BenchMark() {
        start();
    }

    private static void start() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                snapshotList.addLast(statsBenchmark.createSnapshot());
                if (snapshotList.size() > 10) {
                    snapshotList.removeFirst();
                }
            }
        }, 1000, 1000);
    }

    public void getStats() {
        timer.scheduleAtFixedRate(new TimerTask() {
            private void printStats() {
                if (snapshotList.size() >= 10) {
                    Long[] begin = snapshotList.getFirst();
                    Long[] end = snapshotList.getLast();

                    // index含义：0-代表当前时间，1-发送成功数量 2-发送失败数量 3-接收成功数量 4-接收失败数量 5-发送消息成功总耗时
                    final long sendTps = (long) (((end[3] - begin[3]) / (double) (end[0] - begin[0])) * 1000L);
                    final double averageRT = ((end[5] - begin[5]) / (double) (end[3] - begin[3]));

                    System.out.printf(
                            "Send TPS: %d Max RT: %d Average RT: %7.3f Send Failed: %d Response Failed: %d\n"//
                            , sendTps//
                            , statsBenchmark.getSendMaxRT().get()//
                            , averageRT//
                            , end[2]//
                            , end[4]//
                    );
                }
            }


            @Override
            public void run() {
                try {
                    this.printStats();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 10000, 10000);
    }

    long beginTimestamp;

    public void statisticsStart() {
        beginTimestamp = System.currentTimeMillis();
    }

    public void statisticsEnd() {
        statsBenchmark.getSendRequestSuccessCount().incrementAndGet();
        statsBenchmark.getReceiveResponseSuccessCount().incrementAndGet();
        final long currentRT = currentTimeMillis() - beginTimestamp;
        statsBenchmark.getSendSuccessTimeTotal().addAndGet(currentRT);
        long prevMaxRT = statsBenchmark.getSendMaxRT().get();
        while (currentRT > prevMaxRT) {
            boolean updated =
                    statsBenchmark.getSendMaxRT().compareAndSet(prevMaxRT,
                            currentRT);
            if (updated)
                break;

            prevMaxRT = statsBenchmark.getSendMaxRT().get();
        }

    }
}
