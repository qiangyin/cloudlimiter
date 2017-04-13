package com.cloudzone.cloudlimiter.meter;

import com.cloudzone.cloudlimiter.base.AcquireStatus;
import com.cloudzone.cloudlimiter.base.IntervalModel;
import com.cloudzone.cloudlimiter.base.MeterListener;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用于按秒，按分钟间隔请求方法或者代码的请求调用次数
 *
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/5
 */
public class CloudMeter {
    private static MeterTopic DEFAUTTOPIC;

    private static final ConcurrentHashMap<MeterTopic, AtomicLong> GlobalrequestTopicMap = new ConcurrentHashMap<MeterTopic, AtomicLong>();

    // 队列中保存每个tag最近的60秒的TPS值
    private static final int LASTERSECONDNUM = 60;

    // 队列中保存每个tag最近的10分钟的TPS值
    private static final int LASTERMINUTENUM = 10;

    private static final ConcurrentHashMap<MeterTopic, LinkedBlockingQueue<Meterinfo>> GlobalSecondTopicMap = new ConcurrentHashMap<MeterTopic, LinkedBlockingQueue<Meterinfo>>();
    private static final ConcurrentHashMap<MeterTopic, LinkedBlockingQueue<Meterinfo>> GlobalMinuteTopicMap = new ConcurrentHashMap<MeterTopic, LinkedBlockingQueue<Meterinfo>>();


    final static ConcurrentHashMap<MeterTopic, LinkedList<Long[]>> GlobalPeriodSecondTopicMap = new ConcurrentHashMap<MeterTopic, LinkedList<Long[]>>();
    final static ConcurrentHashMap<MeterTopic, LinkedList<Long[]>> GlobalPeriodMinuteTopicMap = new ConcurrentHashMap<MeterTopic, LinkedList<Long[]>>();


    final static int SECOND = 1000;
    final static int MINUTE = 1000 * 60;


    private final static ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private static volatile boolean isStart = false;
    private static volatile boolean isPush = false;

    private MeterListener meterListener;

    /**
     * 构造函数初始化（单例）
     */
    private CloudMeter() {
        startOnce();
    }

    /**
     * 获取CloudMeter单例对象
     */
    public static CloudMeter getSingleInstance() {
        return SingletonHolder.cloudMeter;
    }

    /**
     * 使用静态内部类获取CloudMeter单例对象（延时加载，需要时候再创建）
     */
    private static class SingletonHolder {
        private static CloudMeter cloudMeter = new CloudMeter();
    }

    /**
     * 获取当前设置统计请求次数的时间间隔
     */
    public IntervalModel getIntervalModel() {
        return intervalModel;
    }

    /**
     * 设置统计请求次数的时间间隔
     */
    public void setIntervalModel(IntervalModel intervalModel) {
        this.intervalModel = intervalModel;
    }

    /**
     * 默认只统计按秒时间间隔数据
     */
    private IntervalModel intervalModel = IntervalModel.SECOND;

    /**
     * 获取当前获取订阅的Topic类型
     */
    public MeterTopic getAcquireMeterTopic() {
        return acquireMeterTopic;
    }


    /**
     * 设置当前获取订阅的Topic类型
     * 如果当前acquireMeterTopic==null，则订阅所有topic统计信息
     * acquireMeterTopic的tag不能为空，如果tag为"*",则订阅所有topic统计信息。
     * 如果acquireMeterTopic的tag和type都不为空，则根据tag及type同时过滤
     */
    public void setAcquireMeterTopic(MeterTopic acquireMeterTopic) {
        this.acquireMeterTopic = acquireMeterTopic;
    }

    /**
     * 设置当前获取订阅的Topic的tag值类型
     */
    public void setAcquireMeterTopic(String acquireTopicTag) {
        final MeterTopic meterTopic = new MeterTopic();
        meterTopic.setTag(acquireTopicTag);
        this.acquireMeterTopic = meterTopic;
    }

    /**
     * 设置当前获取订阅的Topic的tag及type值类型
     */
    public void setAcquireMeterTopic(String acquireTopicTag, String acquireTopicType) {
        final MeterTopic meterTopic = new MeterTopic();
        meterTopic.setTag(acquireTopicTag);
        meterTopic.setType(acquireTopicType);
        this.acquireMeterTopic = meterTopic;
    }

    /**
     * 推送统计信息的对应tag(默认为推送所有tag信息)
     */
    private MeterTopic acquireMeterTopic;

    /**
     * 退出释放对应资源（如果调用应用程序不在使用统计功能，建议调用次函数释放资源）
     */
    public void shutdown() {
        this.scheduledExecutorService.shutdown();
    }

    /**
     * 注册订阅获取统计数据的函数
     */
    public void registerListener(MeterListener meterListener) {
        this.meterListener = meterListener;
        this.pushAcquireMeterinfo();
    }

    /**
     * 保证定时统计任务只会执行一次
     */
    private static void startOnce() {
        if (isStart == false) {
            isStart = true;
            DEFAUTTOPIC = new MeterTopic();
            DEFAUTTOPIC.setTag("DefautTopicTag");
            meterPerSecond();
            meterPerMinute();
        }
    }

    /**
     * 按照秒间隔统计请求数据
     */
    private static void meterPerSecond() {
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    for (Map.Entry<MeterTopic, LinkedList<Long[]>> entry : GlobalPeriodSecondTopicMap.entrySet()) {
                        MeterTopic meterTopic = entry.getKey();
                        LinkedList<Long[]> secondList = entry.getValue();
                        Long[] snap = CloudMeter.createPeriodTopicMap().get(meterTopic);
                        secondList.addLast(snap);

                        if (secondList.size() > 2) {
                            Long[] firstSnap = secondList.removeFirst();
                            Long[] secondSnap = secondList.getFirst();
                            long requestNum = (secondSnap[1] - firstSnap[1]);
                            Meterinfo meterinfo = new Meterinfo();
                            meterinfo.setRequestNum(requestNum);
                            meterinfo.setNowDate(new Date(firstSnap[0]));
                            meterinfo.setTimeUnitType(TimeUnit.SECONDS);
                            meterinfo.setMeterTopic(meterTopic);
                            if (GlobalSecondTopicMap.get(meterTopic).size() > LASTERSECONDNUM) {
                                GlobalSecondTopicMap.get(meterTopic).poll();
                            }
                            GlobalSecondTopicMap.get(meterTopic).add(meterinfo);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * 按照分钟间隔统计请求数据
     */
    private static void meterPerMinute() {
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    for (Map.Entry<MeterTopic, LinkedList<Long[]>> entry : GlobalPeriodMinuteTopicMap.entrySet()) {
                        MeterTopic meterTopic = entry.getKey();
                        LinkedList<Long[]> minuteList = entry.getValue();
                        Long[] snap = CloudMeter.createPeriodTopicMap().get(meterTopic);
                        minuteList.addLast(snap);

                        if (minuteList.size() > 2) {
                            Long[] firstSnap = minuteList.removeFirst();
                            Long[] secondSnap = minuteList.getFirst();
                            long requestNum = (secondSnap[1] - firstSnap[1]);
                            Meterinfo meterinfo = new Meterinfo();
                            meterinfo.setRequestNum(requestNum);
                            meterinfo.setNowDate(new Date(firstSnap[0]));
                            meterinfo.setTimeUnitType(TimeUnit.MINUTES);
                            meterinfo.setMeterTopic(meterTopic);
                            if (GlobalMinuteTopicMap.get(meterTopic).size() > LASTERMINUTENUM) {
                                GlobalMinuteTopicMap.get(meterTopic).poll();
                            }
                            GlobalMinuteTopicMap.get(meterTopic).add(meterinfo);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    /**
     * 创建一次当前时刻的快照数据（保存当前时间及请求总次数）
     */
    private static Map<MeterTopic, Long[]> createPeriodTopicMap() {
        ConcurrentHashMap<MeterTopic, Long[]> PeriodTopicMap = new ConcurrentHashMap<MeterTopic, Long[]>();
        for (Map.Entry<MeterTopic, AtomicLong> entry : GlobalrequestTopicMap.entrySet()) {
            MeterTopic meterTopic = entry.getKey();
            AtomicLong num = entry.getValue();
            Long[] snap = new Long[]{
                    System.currentTimeMillis(),// 产生记录时间
                    num.get(),// 获取当前请求数量
            };
            PeriodTopicMap.put(meterTopic, snap);
        }
        return PeriodTopicMap;
    }

    /**
     * 统计一次成功请求, 因为没有传递topic参数则当做DEFAUTTOPIC类型统计
     */
    public void request() {
        request(DEFAUTTOPIC);
    }

    /**
     * 统计nums次成功请求, 因为没有传递topic参数则当做DEFAUTTOPIC类型统计
     */
    public void request(long nums) {
        request(DEFAUTTOPIC, nums);
    }

    /**
     * 统计一次成功请求（根据对应的topicTag及topicType分类统计）
     */
    public void request(String topicTag, String topicType) {
        MeterTopic meterTopic = new MeterTopic();
        meterTopic.setTag(topicTag);
        meterTopic.setType(topicType);
        request(meterTopic);
    }

    /**
     * 统计一次成功请求（根据对应的topicTag分类统计，其中topicType默认为null）
     */
    public void request(String topicTag) {
        MeterTopic meterTopic = new MeterTopic();
        meterTopic.setTag(topicTag);
        request(meterTopic);
    }


    /**
     * 统计一次成功请求（根据对应的topic分类统计）
     */
    public void request(MeterTopic meterTopic) {
        request(meterTopic, 1);
    }


    /**
     * 统计成功请求, 通过meterTopic来分类统计
     *
     * @param meterTopic 用于区别统计
     * @param nums       表示当前需要统计的次数
     */
    public void request(MeterTopic meterTopic, long nums) {
        checkTopic(meterTopic);
        initMapWithTopic(meterTopic);
        AtomicLong requestTopicNum = GlobalrequestTopicMap.get(meterTopic);
        requestTopicNum.addAndGet(nums);
    }

    /**
     * 检查meterTopic合法性
     * topic不能为null，topic的tag不能为null或者"*"(只允许订阅topic的tag为"*",代表订阅所有)
     */
    private static void checkTopic(MeterTopic meterTopic) {
        if (meterTopic == null) {
            Exception exception = new RuntimeException("meterTopic == null !!!");
            exception.printStackTrace();
        }
        if (meterTopic != null && meterTopic.getTag() == null) {
            Exception exception = new RuntimeException("You can not allow tag == null !!!");
            exception.printStackTrace();
        }
        if (meterTopic != null && meterTopic.getTag().equals("*")) {
            Exception exception = new RuntimeException("You can not allow use \"*\" as tag !!!");
            exception.printStackTrace();
        }

    }


    /**
     * 初始化meterTopic对应保存数据的数据结构
     * 如果当前meterTopic不存在则添加
     */
    private static void initMapWithTopic(MeterTopic meterTopic) {
        // putIfAbsent如果不存在当前put的key值，则put成功，返回null值
        // 如果当前map已经存在该key，那么返回已存在key对应的value值
        GlobalrequestTopicMap.putIfAbsent(meterTopic, new AtomicLong(0));
        GlobalPeriodSecondTopicMap.putIfAbsent(meterTopic, new LinkedList<Long[]>());
        GlobalPeriodMinuteTopicMap.putIfAbsent(meterTopic, new LinkedList<Long[]>());
        GlobalSecondTopicMap.putIfAbsent(meterTopic, new LinkedBlockingQueue<Meterinfo>());
        GlobalMinuteTopicMap.putIfAbsent(meterTopic, new LinkedBlockingQueue<Meterinfo>());
    }


    /**
     * 推送统计信息
     */
    private void pushAcquireMeterinfo() {
        if (isPush == false) {
            isPush = true;
            push();
        }
    }

    /**
     * 按照500毫秒间隔推送统计信息
     */
    private void push() {
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    switch (intervalModel) {
                        case ALL:
                            processMeterQueue(IntervalModel.SECOND);
                            processMeterQueue(IntervalModel.MINUTE);
                            break;
                        default:
                            processMeterQueue(intervalModel);
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }, 1000, 500, TimeUnit.MILLISECONDS);
    }

    /**
     * 根据model类型，推送对应数据给订阅者
     */
    private void processMeterQueue(IntervalModel model) {
        List<Meterinfo> meterList = new ArrayList<Meterinfo>();
        Map<MeterTopic, LinkedBlockingQueue<Meterinfo>> meterSecondOrMinuteTopicMap = new ConcurrentHashMap<MeterTopic, LinkedBlockingQueue<Meterinfo>>();
        switch (model) {
            case SECOND:
                meterSecondOrMinuteTopicMap = GlobalSecondTopicMap;
                break;
            case MINUTE:
                meterSecondOrMinuteTopicMap = GlobalMinuteTopicMap;
                break;
        }

        for (Map.Entry<MeterTopic, LinkedBlockingQueue<Meterinfo>> entry : meterSecondOrMinuteTopicMap.entrySet()) {
            MeterTopic meterTopic = entry.getKey();
            final LinkedBlockingQueue<Meterinfo> meterinfoQueue = entry.getValue();

            // 如果当前acquireTopic==null，或者Topic的tag为*，推送所有信息
            // 如果当前tag与用户设置获取的tag相同，或者acquireTopic的key与当前信息key相同则放置到推送列表中
            boolean needPush = false;
            if (this.acquireMeterTopic == null || this.acquireMeterTopic.getTag().equals("*") || this.acquireMeterTopic.equals(meterTopic)) {
                if (this.acquireMeterTopic == null) {
                    needPush = true;
                } else if (this.acquireMeterTopic.getType() == null || this.acquireMeterTopic.getType() != null && this.acquireMeterTopic.getType().equals(meterTopic.getType())) {
                    needPush = true;
                }

                if (needPush) {
                    for (Meterinfo info : meterinfoQueue) {
                        meterList.add(info);
                    }
                }

            }
        }

        AcquireStatus acquireStatus = this.meterListener.acquireStats(meterList);
        switch (acquireStatus) {
            case ACQUIRE_SUCCESS:
                for (Meterinfo info : meterList) {
                    meterSecondOrMinuteTopicMap.get(info.getMeterTopic()).remove(info);
                }
                break;
            case REACQUIRE_LATER:
                break;
        }
    }

}
