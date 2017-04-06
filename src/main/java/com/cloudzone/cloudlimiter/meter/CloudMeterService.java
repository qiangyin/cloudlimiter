package com.cloudzone.cloudlimiter.meter;

import com.cloudzone.cloudlimiter.base.AcquireStatus;
import com.cloudzone.cloudlimiter.base.IntervalModel;
import com.cloudzone.cloudlimiter.base.MeterListenner;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/5
 */
public class CloudMeterService {
    // 最大推送时间间隔
    private final static long MaxTimeAcquireInterval = 60 * 1000;

    private static final AtomicLong requestNum = new AtomicLong(0L);

    // 队列中保存最近的300秒的TPS值
    private static int secondQueueSize = 60 * 5;
    private static final Queue<Meterinfo> secondQueues = new LinkedBlockingQueue<Meterinfo>(secondQueueSize);

    // 队列中保存最近的10分钟的TPS值
    private static int minuteQueueSize = 10;
    private static final Queue<Meterinfo> minuteQueues = new LinkedBlockingQueue<Meterinfo>(minuteQueueSize);

    final static LinkedList<Long[]> periodSecondList = new LinkedList<Long[]>();
    final static LinkedList<Long[]> periodMinuteList = new LinkedList<Long[]>();

    final static int SECOND = 1000;
    final static int MINUTE = 1000 * 60;


    final static Timer timer = new Timer("CloudMeterTimer", true);

    private MeterListenner meterListenner;

    public IntervalModel getIntervalModel() {
        return intervalModel;
    }

    public void setIntervalModel(IntervalModel intervalModel) {
        this.intervalModel = intervalModel;
    }

    // 默认推送秒间隔统计的数据
    private IntervalModel intervalModel = IntervalModel.SECOND;


    public CloudMeterService() {
        start();
    }

    public void registerListener(MeterListenner meterListenner) {
        this.meterListenner = meterListenner;
    }

    private void start() {
        this.meterPerSecond();
        this.meterPerMinute();
        this.pushAcquireMeterinfo();
    }

    private void meterPerSecond() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                periodSecondList.addLast(CloudMeterService.createperiod());

                if (periodSecondList.size() > 2) {
                    Long[] firstSnap = periodSecondList.removeFirst();
                    Long[] secondSnap = periodSecondList.getFirst();
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

    private void meterPerMinute() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                periodMinuteList.addLast(CloudMeterService.createperiod());

                if (periodMinuteList.size() > 2) {
                    Long[] firstSnap = periodMinuteList.removeFirst();
                    Long[] secondSnap = periodMinuteList.getFirst();
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

    private static Long[] createperiod() {
        Long[] snap = new Long[]{//
                System.currentTimeMillis(),// 产生记录时间
                requestNum.get(),// 获取当前请求数量
        };

        return snap;
    }

    // 统计一次成功请求
    public void request() {
        requestNum.addAndGet(1);
    }

    // 统计nums次成功请求
    public void request(long nums) {
        requestNum.addAndGet(nums);
    }

    /**
     * 推送统计信息
     *
     * @author tantexian, <my.oschina.net/tantexian>
     * @params
     * @since 2017/4/6
     */
    private void pushAcquireMeterinfo() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                switch (intervalModel) {
                    case ALL:
                        processMeterQueue(IntervalModel.SECOND);
                        processMeterQueue(IntervalModel.MINUTE);
                        break;
                    default:
                        processMeterQueue(intervalModel);
                        break;
                }
            }
        }, 1000, 500);
    }

    private void processMeterQueue(IntervalModel model) {
        List<Meterinfo> meterList = new ArrayList<Meterinfo>();
        Queue<Meterinfo> meterinfoQueue = secondQueues;
        switch (model) {
            case SECOND:
                meterinfoQueue = secondQueues;
                break;
            case MINUTE:
                meterinfoQueue = minuteQueues;
                break;
        }
        for (Meterinfo info : meterinfoQueue) {
            meterList.add(meterinfoQueue.peek());
        }
        AcquireStatus acquireStatus = this.meterListenner.acquireStats(meterList);
        switch (acquireStatus) {
            case ACQUIRE_SUCCESS:
                for (Meterinfo info : meterList) {
                    meterinfoQueue.remove(info);
                }
                break;
            case REACQUIRE_LATER:
                break;
        }
    }

}
