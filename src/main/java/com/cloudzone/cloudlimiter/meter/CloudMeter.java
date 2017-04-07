package com.cloudzone.cloudlimiter.meter;

import com.cloudzone.cloudlimiter.base.AcquireStatus;
import com.cloudzone.cloudlimiter.base.IntervalModel;
import com.cloudzone.cloudlimiter.base.MeterListenner;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/5
 */
public class CloudMeter {
    private static final AtomicLong requestNum = new AtomicLong(0L);

    private static final String DEFAUTTAG = "DefaultTag";
    private static final Map<String, AtomicLong> GlobalrequestTagMap = new ConcurrentHashMap<String, AtomicLong>();

    // 队列中保存每个tag最近的60秒(最少)的TPS值
    private final int SECNUM = 60;
    private AtomicInteger secondQueueSize = new AtomicInteger(0);

    // 队列中保存每个tag最近的10分钟(最少)的TPS值
    private final int MINNUM = 10;
    private AtomicInteger minuteQueueSize = new AtomicInteger(0);


    private static final Queue<Meterinfo> GlobalSecondQueues = new LinkedBlockingQueue<Meterinfo>();


    private static final Queue<Meterinfo> GlobalMinuteQueues = new LinkedBlockingQueue<Meterinfo>();


    final static Map<String, LinkedList<Long[]>> GlobalPeriodSecondTagMap = new ConcurrentHashMap<String, LinkedList<Long[]>>();
    final static Map<String, LinkedList<Long[]>> GlobalPeriodMinuteTagMap = new ConcurrentHashMap<String, LinkedList<Long[]>>();


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

    public String getAcquireTag() {
        return acquireTag;
    }

    public void setAcquireTag(String acquireTag) {
        this.acquireTag = acquireTag;
    }

    // 推送统计信息的对应tag(默认为推送所有tag信息)
    private String acquireTag = "*";

    public CloudMeter() {
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
                for (Map.Entry<String, LinkedList<Long[]>> entry : GlobalPeriodSecondTagMap.entrySet()) {
                    String tag = entry.getKey();
                    LinkedList<Long[]> secondList = entry.getValue();
                    Long[] snap = CloudMeter.createPeriodTagMap().get(tag);
                    secondList.addLast(snap);

                    if (secondList.size() > 2) {
                        Long[] firstSnap = secondList.removeFirst();
                        Long[] secondSnap = secondList.getFirst();
                        long requestNum = (secondSnap[1] - firstSnap[1]);
                        Meterinfo meterinfo = new Meterinfo();
                        meterinfo.setRequestNum(requestNum);
                        meterinfo.setNowDate(new Date(firstSnap[0]));
                        meterinfo.setType(TimeUnit.SECONDS);
                        meterinfo.setTag(tag);
                        if (GlobalSecondQueues.size() > secondQueueSize.get()) {
                            // 超出队列长度未处理，则丢弃最开始采集的数据
                            GlobalSecondQueues.poll();
                        }
                        GlobalSecondQueues.add(meterinfo);
                    }

                }
            }
        }, 0, SECOND);
    }


    private void meterPerMinute() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (Map.Entry<String, LinkedList<Long[]>> entry : GlobalPeriodMinuteTagMap.entrySet()) {
                    String tag = entry.getKey();
                    LinkedList<Long[]> minuteList = entry.getValue();
                    Long[] snap = CloudMeter.createPeriodTagMap().get(tag);
                    minuteList.addLast(snap);

                    if (minuteList.size() > 2) {
                        Long[] firstSnap = minuteList.removeFirst();
                        Long[] secondSnap = minuteList.getFirst();
                        long requestNum = (secondSnap[1] - firstSnap[1]);
                        Meterinfo meterinfo = new Meterinfo();
                        meterinfo.setRequestNum(requestNum);
                        meterinfo.setNowDate(new Date(firstSnap[0]));
                        meterinfo.setType(TimeUnit.MINUTES);
                        meterinfo.setTag(tag);
                        if (GlobalMinuteQueues.size() > secondQueueSize.get()) {
                            // 超出队列长度未处理，则丢弃最开始采集的数据
                            GlobalMinuteQueues.poll();
                        }
                        GlobalMinuteQueues.add(meterinfo);
                    }

                }
            }
        }, 0, MINUTE);
    }

    private static Map<String, Long[]> createPeriodTagMap() {
        ConcurrentHashMap<String, Long[]> PeriodTagMap = new ConcurrentHashMap<String, Long[]>();
        for (Map.Entry<String, AtomicLong> entry : GlobalrequestTagMap.entrySet()) {
            String tag = entry.getKey();
            AtomicLong num = entry.getValue();
            Long[] snap = new Long[]{
                    System.currentTimeMillis(),// 产生记录时间
                    num.get(),// 获取当前请求数量
            };
            PeriodTagMap.put(tag, snap);
        }
        return PeriodTagMap;
    }

    // 统计一次成功请求, 如果没有tag参数则当做DEFAUTTAG相同类型统计
    public void request() {
        request(DEFAUTTAG, 1);
    }

    /**
     * 统计一次成功请求, 通过tag来区分统计
     *
     * @author tantexian, <my.oschina.net/tantexian>
     * @params tag 用于区别统计、注意不能为"*"
     * @since 2017/4/7
     */
    public void request(String tag) {
        checkTag(tag);
        // putIfAbsent如果不存在当前put的key值，则put成功，返回null值
        // 如果当前map已经存在该key，那么返回已存在key对应的value值
        AtomicLong requestTagNum = GlobalrequestTagMap.putIfAbsent(tag, new AtomicLong(0));
        if (requestTagNum != null) {
            // 设置一个新的tag
            requestTagNum.addAndGet(1);
            // 增加一个tag则，对应保存队列元素增加一倍
            secondQueueSize.addAndGet(SECNUM);
            minuteQueueSize.addAndGet(MINNUM);
            GlobalPeriodSecondTagMap.putIfAbsent(tag, new LinkedList<Long[]>());
            GlobalPeriodMinuteTagMap.putIfAbsent(tag, new LinkedList<Long[]>());
        } else {
            AtomicLong newRequestTagNum = GlobalrequestTagMap.get(tag);
            newRequestTagNum.addAndGet(1);
        }
    }

    /**
     * 统计成功请求, 通过tag来区分统计
     *
     * @param tag  用于区别统计、注意不能为"*"
     * @param nums 表示一次需要统计的次数
     * @author tantexian, <my.oschina.net/tantexian>
     * @since 2017/4/7
     */
    public void request(String tag, long nums) {
        checkTag(tag);
        // putIfAbsent如果不存在当前put的key值，则put成功，返回null值
        // 如果当前map已经存在该key，那么返回已存在key对应的value值
        AtomicLong requestTagNum = GlobalrequestTagMap.putIfAbsent(tag, new AtomicLong(0));
        if (requestTagNum != null) {
            // 设置一个新的tag
            requestTagNum.addAndGet(nums);
            // 增加一个tag则，对应保存队列元素增加一倍
            secondQueueSize.addAndGet(SECNUM);
            minuteQueueSize.addAndGet(MINNUM);
            // 如果当前tag不存在则添加
            GlobalPeriodSecondTagMap.putIfAbsent(tag, new LinkedList<Long[]>());
            GlobalPeriodMinuteTagMap.putIfAbsent(tag, new LinkedList<Long[]>());
        } else {
            AtomicLong newRequestTagNum = GlobalrequestTagMap.get(tag);
            newRequestTagNum.addAndGet(nums);
        }
    }

    private static void checkTag(String tag) {
        if (tag.equals("*")) {
            throw new RuntimeException("You can not allow use \"*\" as tag !!!");
        }
    }

    /**
     * 统计nums次成功请求，如果没有tag参数则当做DEFAUTTAG相同类型统计
     *
     * @author tantexian, <my.oschina.net/tantexian>
     * @params
     * @since 2017/4/7
     */
    public void request(long nums) {
        request(DEFAUTTAG, nums);
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

    // 根据model类型，推送对应数据给用户
    private void processMeterQueue(IntervalModel model) {
        List<Meterinfo> meterList = new ArrayList<Meterinfo>();
        Queue<Meterinfo> meterinfoQueue = GlobalSecondQueues;
        switch (model) {
            case SECOND:
                meterinfoQueue = GlobalSecondQueues;
                break;
            case MINUTE:
                meterinfoQueue = GlobalMinuteQueues;
                break;
        }

        for (Meterinfo info : meterinfoQueue) {
            if (this.acquireTag.equals("*")) {
                meterList.add(info);
            } else if (info.getTag().equals(this.acquireTag)) {
                meterList.add(info);
            }
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
