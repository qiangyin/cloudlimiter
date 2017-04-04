package com.cloudzone.cloudlimiter.meter;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author tantexian<my.oschina.net/tantexian>
 * @since 2017/4/2
 */
public class CloudMeter {
    private static final AtomicLong requestNum = new AtomicLong(0L);

    // 队列中保存最近的180秒的TPS值
    private static int secondQueueSize = 60 * 3;
    private static final Queue<Meterinfo> secondQueues = new LinkedBlockingQueue<Meterinfo>(secondQueueSize);

    // 队列中保存最近的10分钟的TPS值
    private static int minuteQueueSize = 10;
    private static final Queue<Meterinfo> minuteQueues = new LinkedBlockingQueue<Meterinfo>(minuteQueueSize);

    final static LinkedList<Long[]> snapshotSecondList = new LinkedList<Long[]>();
    final static LinkedList<Long[]> snapshotMinuteList = new LinkedList<Long[]>();

    final static int SECOND = 1000;
    final static int MINUTE = 1000 * 60;


    final static Timer timer = new Timer("CloudMeter", true);

    CloudMeter() {
        start();
    }

    public static void start() {
        meterPerSecond();
        meterPerMinute();
    }

    private static void meterPerSecond() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                snapshotSecondList.addLast(CloudMeter.createSnapshot());

                if (snapshotSecondList.size() > 2) {
                    Long[] firstSnap = snapshotSecondList.removeFirst();
                    Long[] secondSnap = snapshotSecondList.getFirst();
                    long requestNum = (secondSnap[1] - firstSnap[1]);
                    Meterinfo meterinfo = new Meterinfo();
                    meterinfo.setRequestNum(requestNum);
                    meterinfo.setNowDate(new Date(firstSnap[0]));
                    meterinfo.setType(TimeUnit.SECONDS);
                    if (secondQueues.size() > secondQueueSize) {
                        // 超出队列长度未处理，则丢弃最开始采集的数据
                        secondQueues.poll();
                    }
                    secondQueues.add(meterinfo);
                }

            }
        }, 1000, SECOND);
    }

    private static void meterPerMinute() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                snapshotMinuteList.addLast(CloudMeter.createSnapshot());

                if (snapshotMinuteList.size() > 2) {
                    Long[] firstSnap = snapshotMinuteList.removeFirst();
                    Long[] secondSnap = snapshotMinuteList.getFirst();
                    long requestNum = (secondSnap[1] - firstSnap[1]);
                    Meterinfo meterinfo = new Meterinfo();
                    meterinfo.setRequestNum(requestNum);
                    meterinfo.setNowDate(new Date(firstSnap[0]));
                    meterinfo.setType(TimeUnit.MINUTES);
                    if (minuteQueues.size() > minuteQueueSize) {
                        // 超出队列长度未处理，则丢弃最开始采集的数据
                        minuteQueues.poll();
                    }
                    minuteQueues.add(meterinfo);
                }

            }
        }, 1000, MINUTE);
    }

    private static Long[] createSnapshot() {
        Long[] snap = new Long[]{//
                System.currentTimeMillis(),// 产生记录时间
                requestNum.get(),// 获取当前请求数量
        };

        return snap;
    }

    private static Queue<Meterinfo> acquireFormSecondQueues() {
        Queue<Meterinfo> nowQueues = new LinkedBlockingQueue<Meterinfo>();
        for (Meterinfo info : secondQueues) {
            nowQueues.add(secondQueues.poll());
        }
        return nowQueues;
    }

    private static Queue<Meterinfo> acquireFormMinuteQueues() {
        Queue<Meterinfo> nowQueues = new LinkedBlockingQueue<Meterinfo>();
        for (Meterinfo info : minuteQueues) {
            nowQueues.add(minuteQueues.poll());
        }
        return nowQueues;
    }

    public static void request() {
        requestNum.addAndGet(1);
    }


    public static void printStats() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (Meterinfo meterinfo : acquireFormSecondQueues()) {
                    System.out.println(meterinfo);
                }
                for (Meterinfo meterinfo : acquireFormMinuteQueues()) {
                    System.out.println("--------------------" + meterinfo);
                }
            }
        }, 100, 100);
    }

}
