package com.cloudzone.cloudlimiter.meter;

import com.cloudzone.cloudlimiter.base.AcquireStatus;
import com.cloudzone.cloudlimiter.base.FlowUnit;
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

    final static int SECOND = 1000;
    final static int MINUTE = 1000 * 60;

    // 队列中保存每个tag最近的60秒的TPS值
    private static final int LASTERSECONDNUM = 60;
    // 队列中保存每个tag最近的10分钟的TPS值
    private static final int LASTERMINUTENUM = 10;

    private volatile boolean isPush = false;
    private static volatile boolean isSecondTimerStart = false;
    private static volatile boolean isMinitusTimerStart = false;

    private ConcurrentHashMap<MeterTopic, AtomicLong> globalrequestTopicMap = new ConcurrentHashMap<MeterTopic, AtomicLong>();


    private ConcurrentHashMap<MeterTopic, LinkedBlockingQueue<Meterinfo>> globalSecondTopicMap = new ConcurrentHashMap<MeterTopic, LinkedBlockingQueue<Meterinfo>>();
    private ConcurrentHashMap<MeterTopic, LinkedBlockingQueue<Meterinfo>> globalMinuteTopicMap = new ConcurrentHashMap<MeterTopic, LinkedBlockingQueue<Meterinfo>>();


    private ConcurrentHashMap<MeterTopic, LinkedList<Long[]>> globalPeriodSecondTopicMap = new ConcurrentHashMap<MeterTopic, LinkedList<Long[]>>();
    private ConcurrentHashMap<MeterTopic, LinkedList<Long[]>> globalPeriodMinuteTopicMap = new ConcurrentHashMap<MeterTopic, LinkedList<Long[]>>();


    private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();


    private MeterListener meterListener;

    /**
     * 构造函数初始化
     */
    public CloudMeter() {
        startOnce();
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
        // 关闭时候，强制制造1次数据，以达到推送完毕之前有数据
        meterPerSecondHandle();
        meterPerMinuteHandle();
        pushHandle();
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
    private void startOnce() {
        meterPerSecondSchedule(this);
        meterPerMinuteSchedule(this);
        DEFAUTTOPIC = new MeterTopic();
        DEFAUTTOPIC.setTag("DefautTopicTag");
    }


    /**
     * 按照秒间隔统计请求数据
     */
    private static void meterPerSecondSchedule(final CloudMeter cloudMeter) {
        cloudMeter.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                cloudMeter.meterPerSecondHandle();
                // 此处代表second TPS统计定时器已经启动了
                if (isSecondTimerStart == false) {
                    isSecondTimerStart = true;
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void meterPerSecondHandle() {
        try {
            for (Map.Entry<MeterTopic, LinkedList<Long[]>> entry : globalPeriodSecondTopicMap.entrySet()) {
                MeterTopic meterTopic = entry.getKey();
                LinkedList<Long[]> secondList = entry.getValue();
                Long[] snap = this.createPeriodTopicMap().get(meterTopic);
                secondList.addLast(snap);

                if (secondList.size() >= 2) {
                    Long[] firstSnap = secondList.removeFirst();
                    Long[] secondSnap = secondList.getFirst();
                    long requestNum = (secondSnap[1] - firstSnap[1]);
                    Meterinfo meterinfo = new Meterinfo();
                    meterinfo.setRequestNum(requestNum);
                    meterinfo.setNowDate(new Date(firstSnap[0]));
                    meterinfo.setTimeUnitType(TimeUnit.SECONDS);
                    meterinfo.setMeterTopic(meterTopic);
                    if (globalSecondTopicMap.get(meterTopic).size() > LASTERSECONDNUM) {
                        globalSecondTopicMap.get(meterTopic).poll();
                    }
                    globalSecondTopicMap.get(meterTopic).add(meterinfo);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 按照分钟间隔统计请求数据
     */
    private static void meterPerMinuteSchedule(final CloudMeter cloudMeter) {
        cloudMeter.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                cloudMeter.meterPerMinuteHandle();
                // 此处代表Minute TPS统计定时器已经启动了
                if (isMinitusTimerStart == false) {
                    isMinitusTimerStart = true;
                }
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    private void meterPerMinuteHandle() {
        try {
            for (Map.Entry<MeterTopic, LinkedList<Long[]>> entry : globalPeriodMinuteTopicMap.entrySet()) {
                MeterTopic meterTopic = entry.getKey();
                LinkedList<Long[]> minuteList = entry.getValue();
                Long[] snap = this.createPeriodTopicMap().get(meterTopic);
                minuteList.addLast(snap);

                if (minuteList.size() >= 2) {
                    Long[] firstSnap = minuteList.removeFirst();
                    Long[] secondSnap = minuteList.getFirst();
                    long requestNum = (secondSnap[1] - firstSnap[1]);
                    Meterinfo meterinfo = new Meterinfo();
                    meterinfo.setRequestNum(requestNum);
                    meterinfo.setNowDate(new Date(firstSnap[0]));
                    meterinfo.setTimeUnitType(TimeUnit.MINUTES);
                    meterinfo.setMeterTopic(meterTopic);
                    if (globalMinuteTopicMap.get(meterTopic).size() > LASTERMINUTENUM) {
                        globalMinuteTopicMap.get(meterTopic).poll();
                    }
                    globalMinuteTopicMap.get(meterTopic).add(meterinfo);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建一次当前时刻的快照数据（保存当前时间及请求总次数）
     */
    private Map<MeterTopic, Long[]> createPeriodTopicMap() {
        ConcurrentHashMap<MeterTopic, Long[]> PeriodTopicMap = new ConcurrentHashMap<MeterTopic, Long[]>();
        for (Map.Entry<MeterTopic, AtomicLong> entry : this.globalrequestTopicMap.entrySet()) {
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
        request(DEFAUTTOPIC, 1);
    }

    /**
     * 统计nums次成功请求, 因为没有传递topic参数则当做DEFAUTTOPIC类型统计
     *
     * @param nums 当前请求次数
     */
    public void request(long nums) {
        request(DEFAUTTOPIC, nums);
    }

    /**
     * 统计一次成功请求（根据对应的topicTag及topicType分类统计）
     *
     * @param topicTag  需要分类统计的topic对应的tag
     * @param topicType 需要分类统计的topic对应的type
     */
    public void request(String topicTag, String topicType) {
        MeterTopic meterTopic = new MeterTopic();
        meterTopic.setTag(topicTag);
        meterTopic.setType(topicType);
        request(meterTopic, 1);
    }

    /**
     * 统计nums次成功请求（根据对应的topicTag及topicType分类统计）
     *
     * @param topicTag  需要分类统计的topic对应的tag
     * @param topicType 需要分类统计的topic对应的type
     * @param nums      当前请求次数
     */
    public void request(String topicTag, String topicType, long nums) {
        MeterTopic meterTopic = new MeterTopic();
        meterTopic.setTag(topicTag);
        meterTopic.setType(topicType);
        request(meterTopic, nums);
    }

    /**
     * 统计一次成功请求（根据对应的topic分类统计）
     *
     * @param topicTag  需要分类统计的topic对应的tag
     * @param topicType 需要分类统计的topic对应的type
     * @param flowUnit  当前统计的单位（流量单位：BYTE/KB/MB/GB/TB/PB）
     * @param size      当前统计的大小
     */
    public void request(String topicTag, String topicType, FlowUnit flowUnit, long size) {
        MeterTopic meterTopic = new MeterTopic();
        meterTopic.setTag(topicTag);
        meterTopic.setType(topicType);
        request(meterTopic, flowUnit.toByte(size));
    }

    /**
     * 统计一次成功请求（根据对应的topicTag分类统计，其中topicType默认为null）
     *
     * @param topicTag 需要分类统计的topic对应的tag
     */
    public void request(String topicTag) {
        MeterTopic meterTopic = new MeterTopic();
        meterTopic.setTag(topicTag);
        request(meterTopic, 1);
    }

    /**
     * 统计nums次成功请求（根据对应的topicTag分类统计）
     *
     * @param topicTag 需要分类统计的topic对应的tag
     * @param nums     当前请求次数
     */
    public void request(String topicTag, long nums) {
        MeterTopic meterTopic = new MeterTopic();
        meterTopic.setTag(topicTag);
        request(meterTopic, nums);
    }

    /**
     * 统计一次成功请求（根据对应的topic分类统计）
     *
     * @param topicTag 需要分类统计的topic对应的tag
     * @param flowUnit 当前统计的单位（流量单位：BYTE/KB/MB/GB/TB/PB）
     * @param size     当前统计的大小
     */
    public void request(String topicTag, FlowUnit flowUnit, long size) {
        MeterTopic meterTopic = new MeterTopic();
        meterTopic.setTag(topicTag);
        request(meterTopic, flowUnit.toByte(size));
    }

    /**
     * 统计一次成功请求（根据对应的topic分类统计）
     *
     * @param meterTopic 需要分类统计的topic
     */
    public void request(MeterTopic meterTopic) {
        request(meterTopic, 1);
    }

    /**
     * 统计一次成功请求（根据对应的topic分类统计）
     *
     * @param meterTopic 需要分类统计的topic
     * @param flowUnit   当前统计的单位（流量单位：BYTE/KB/MB/GB/TB/PB）
     * @param size       当前统计的大小
     */
    public void request(MeterTopic meterTopic, FlowUnit flowUnit, long size) {
        request(meterTopic, flowUnit.toByte(size));
    }

    /**
     * 统计nums次成功请求, 通过meterTopic来分类统计
     *
     * @param meterTopic 需要分类统计的topic
     * @param nums       当前请求次数
     */
    public void request(MeterTopic meterTopic, long nums) {
        checkTopic(meterTopic);
        initMapWithTopic(meterTopic);
        AtomicLong requestTopicNum = globalrequestTopicMap.get(meterTopic);
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
    private void initMapWithTopic(MeterTopic meterTopic) {
        // putIfAbsent如果不存在当前put的key值，则put成功，返回null值
        // 如果当前map已经存在该key，那么返回已存在key对应的value值
        this.globalrequestTopicMap.putIfAbsent(meterTopic, new AtomicLong(-1));
        List resultSec = this.globalPeriodSecondTopicMap.putIfAbsent(meterTopic, new LinkedList<Long[]>());
        if (resultSec == null) { // 表示第一次加入该topic
            LinkedList<Long[]> secondList = this.globalPeriodSecondTopicMap.putIfAbsent(meterTopic, new LinkedList<Long[]>());
            Long[] snap = this.createPeriodTopicMap().get(meterTopic);
            secondList.addLast(snap);
        }

        List resultMin = this.globalPeriodMinuteTopicMap.putIfAbsent(meterTopic, new LinkedList<Long[]>());
        if (resultMin == null) { // 表示第一次加入该topic
            LinkedList<Long[]> minuteList = this.globalPeriodMinuteTopicMap.putIfAbsent(meterTopic, new LinkedList<Long[]>());
            Long[] snap = this.createPeriodTopicMap().get(meterTopic);
            minuteList.addLast(snap);
        }

        this.globalSecondTopicMap.putIfAbsent(meterTopic, new LinkedBlockingQueue<Meterinfo>());
        this.globalMinuteTopicMap.putIfAbsent(meterTopic, new LinkedBlockingQueue<Meterinfo>());
    }


    /**
     * 推送统计信息
     */
    private void pushAcquireMeterinfo() {
        if (isPush == false) {
            isPush = true;
            pushSchedule();
        }
    }

    /**
     * 按照500毫秒间隔推送统计信息
     */
    private void pushSchedule() {
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                pushHandle();
            }
        }, 1000, 500, TimeUnit.MILLISECONDS);
    }

    private void pushHandle() {
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

    /**
     * 根据model类型，推送对应数据给订阅者
     */
    private void processMeterQueue(IntervalModel model) {
        List<Meterinfo> meterList = new ArrayList<Meterinfo>();
        Map<MeterTopic, LinkedBlockingQueue<Meterinfo>> meterSecondOrMinuteTopicMap = new ConcurrentHashMap<MeterTopic, LinkedBlockingQueue<Meterinfo>>();
        switch (model) {
            case SECOND:
                meterSecondOrMinuteTopicMap = globalSecondTopicMap;
                break;
            case MINUTE:
                meterSecondOrMinuteTopicMap = globalMinuteTopicMap;
                break;
        }

        for (Map.Entry<MeterTopic, LinkedBlockingQueue<Meterinfo>> entry : meterSecondOrMinuteTopicMap.entrySet()) {
            MeterTopic meterTopic = entry.getKey();
            final LinkedBlockingQueue<Meterinfo> meterinfoQueue = entry.getValue();


            // 如果当前tag与用户设置获取的tag相同，或者acquireTopic的key与当前信息key相同则放置到推送列表中
            /*System.out.println(this.acquireMeterTopic);
            System.out.println(meterSecondOrMinuteTopicMap);*/
            boolean needPush = false;


            if (this.acquireMeterTopic == null || this.acquireMeterTopic.getTag().equals("*") || this.acquireMeterTopic.equals(meterTopic)) {
                // 如果当前设置订阅acquireTopic==null，或者Topic的tag为*，或者与meterTopic相等推送信息
                needPush = true;
            } else if (this.acquireMeterTopic.getTag().equals(meterTopic.getTag()) && this.acquireMeterTopic.getType() == null) {
                // 如果当前设置订阅acquireTopic的tag与当前meterTopic的tag相等，但是acquireTopic的type==null
                needPush = true;
            }

            if (needPush) {
                for (Meterinfo info : meterinfoQueue) {
                    meterList.add(info);
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
