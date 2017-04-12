package com.cloudzone.cloudlimiter.meter;

import com.cloudzone.cloudlimiter.base.AcquireStatus;
import com.cloudzone.cloudlimiter.base.IntervalModel;
import com.cloudzone.cloudlimiter.base.MeterListener;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.collect.ComparisonChain.start;

/**
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/5
 */
public class CloudMeter {
    private static Topic DEFAUTTOPIC;

    private static final ConcurrentHashMap<Topic, AtomicLong> GlobalrequestTopicMap = new ConcurrentHashMap<Topic, AtomicLong>();

    // 队列中保存每个tag最近的60秒的TPS值
    private static final int LASTERSECONDNUM = 60;

    // 队列中保存每个tag最近的10分钟的TPS值
    private static final int LASTERMINUTENUM = 10;

    private static final ConcurrentHashMap<Topic, LinkedBlockingQueue<Meterinfo>> GlobalSecondTopicMap = new ConcurrentHashMap<Topic, LinkedBlockingQueue<Meterinfo>>();
    private static final ConcurrentHashMap<Topic, LinkedBlockingQueue<Meterinfo>> GlobalMinuteTopicMap = new ConcurrentHashMap<Topic, LinkedBlockingQueue<Meterinfo>>();


    final static ConcurrentHashMap<Topic, LinkedList<Long[]>> GlobalPeriodSecondTopicMap = new ConcurrentHashMap<Topic, LinkedList<Long[]>>();
    final static ConcurrentHashMap<Topic, LinkedList<Long[]>> GlobalPeriodMinuteTopicMap = new ConcurrentHashMap<Topic, LinkedList<Long[]>>();


    final static int SECOND = 1000;
    final static int MINUTE = 1000 * 60;


    private final static ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private static volatile boolean isStart = false;
    private static volatile boolean isPush = false;

    private MeterListener meterListener;

    public IntervalModel getIntervalModel() {
        return intervalModel;
    }

    public void setIntervalModel(IntervalModel intervalModel) {
        this.intervalModel = intervalModel;
    }

    // 默认推送秒间隔统计的数据
    private IntervalModel intervalModel = IntervalModel.SECOND;

    public Topic getAcquireTopic() {
        return acquireTopic;
    }

    public void setAcquireTopic(Topic acquireTopic) {
        this.acquireTopic = acquireTopic;
    }

    public void setAcquireTopic(String acquireTopicTag) {
        final Topic topic = new Topic();
        topic.setTag(acquireTopicTag);
        this.acquireTopic = topic;
    }

    public void setAcquireTopic(String acquireTopicTag, String acquireTopicType) {
        final Topic topic = new Topic();
        topic.setTag(acquireTopicTag);
        topic.setType(acquireTopicType);
        this.acquireTopic = topic;
    }

    // 推送统计信息的对应tag(默认为推送所有tag信息)
    private Topic acquireTopic;

    public CloudMeter() {
        startOnce();
    }

    public void shutdown() {
        this.scheduledExecutorService.shutdown();
    }

    public void registerListener(MeterListener meterListener) {
        this.meterListener = meterListener;
        this.pushAcquireMeterinfo();
    }

    private static void startOnce() {
        if (isStart == false) {
            isStart = true;
            DEFAUTTOPIC = new Topic();
            DEFAUTTOPIC.setTag("DefautTopicTag");
            meterPerSecond();
            meterPerMinute();
        }
    }

    private static void meterPerSecond() {
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    for (Map.Entry<Topic, LinkedList<Long[]>> entry : GlobalPeriodSecondTopicMap.entrySet()) {
                        Topic topic = entry.getKey();
                        LinkedList<Long[]> secondList = entry.getValue();
                        Long[] snap = CloudMeter.createPeriodTopicMap().get(topic);
                        secondList.addLast(snap);

                        if (secondList.size() > 2) {
                            Long[] firstSnap = secondList.removeFirst();
                            Long[] secondSnap = secondList.getFirst();
                            long requestNum = (secondSnap[1] - firstSnap[1]);
                            Meterinfo meterinfo = new Meterinfo();
                            meterinfo.setRequestNum(requestNum);
                            meterinfo.setNowDate(new Date(firstSnap[0]));
                            meterinfo.setTimeUnitType(TimeUnit.SECONDS);
                            meterinfo.setTopic(topic);
                            if (GlobalSecondTopicMap.get(topic).size() > LASTERSECONDNUM) {
                                GlobalSecondTopicMap.get(topic).poll();
                            }
                            GlobalSecondTopicMap.get(topic).add(meterinfo);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private static void meterPerMinute() {
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    for (Map.Entry<Topic, LinkedList<Long[]>> entry : GlobalPeriodMinuteTopicMap.entrySet()) {
                        Topic topic = entry.getKey();
                        LinkedList<Long[]> minuteList = entry.getValue();
                        Long[] snap = CloudMeter.createPeriodTopicMap().get(topic);
                        minuteList.addLast(snap);

                        if (minuteList.size() > 2) {
                            Long[] firstSnap = minuteList.removeFirst();
                            Long[] secondSnap = minuteList.getFirst();
                            long requestNum = (secondSnap[1] - firstSnap[1]);
                            Meterinfo meterinfo = new Meterinfo();
                            meterinfo.setRequestNum(requestNum);
                            meterinfo.setNowDate(new Date(firstSnap[0]));
                            meterinfo.setTimeUnitType(TimeUnit.MINUTES);
                            meterinfo.setTopic(topic);
                            if (GlobalMinuteTopicMap.get(topic).size() > LASTERMINUTENUM) {
                                GlobalMinuteTopicMap.get(topic).poll();
                            }
                            GlobalMinuteTopicMap.get(topic).add(meterinfo);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    private static Map<Topic, Long[]> createPeriodTopicMap() {
        ConcurrentHashMap<Topic, Long[]> PeriodTopicMap = new ConcurrentHashMap<Topic, Long[]>();
        for (Map.Entry<Topic, AtomicLong> entry : GlobalrequestTopicMap.entrySet()) {
            Topic topic = entry.getKey();
            AtomicLong num = entry.getValue();
            Long[] snap = new Long[]{
                    System.currentTimeMillis(),// 产生记录时间
                    num.get(),// 获取当前请求数量
            };
            PeriodTopicMap.put(topic, snap);
        }
        return PeriodTopicMap;
    }

    // 统计一次成功请求, 如果没有tag参数则当做DEFAUTTAG相同类型统计
    public void request() {
        request(DEFAUTTOPIC);
    }

    // 统计一次成功请求, 如果没有tag参数则当做DEFAUTTAG相同类型统计
    public void request(String topicTag, String topicType) {
        Topic topic = new Topic();
        topic.setTag(topicTag);
        topic.setType(topicType);
        request(topic);
    }

    // 统计一次成功请求, 如果没有tag参数则当做DEFAUTTAG相同类型统计
    public void request(String topicTag) {
        Topic topic = new Topic();
        topic.setTag(topicTag);
        request(topic);
    }

    /**
     * 统计一次成功请求, 通过topic来区分统计
     *
     * @author tantexian, <my.oschina.net/tantexian>
     * @params tag 用于区别统计、注意不能为"*"
     * @since 2017/4/7
     */
    public void request(Topic topic) {
        request(topic, 1);
    }


    /**
     * 统计成功请求, 通过tag来区分统计
     *
     * @param topic 用于区别统计、注意不能为"*"
     * @param nums  表示一次需要统计的次数
     * @author tantexian, <my.oschina.net/tantexian>
     * @since 2017/4/7
     */
    public void request(Topic topic, long nums) {
        initMapWithTopic(topic);
        AtomicLong requestTopicNum = GlobalrequestTopicMap.get(topic);
        requestTopicNum.addAndGet(nums);
        // System.out.println("topic ==" + topic + " requestTopicNum == " + requestTopicNum);
    }

    private static void checkTopic(Topic topic) {
        if (topic == null) {
            Exception exception = new RuntimeException("topic == null !!!");
            exception.printStackTrace();
        }
        if (topic.getTag() == null) {
            Exception exception = new RuntimeException("You can not allow tag == null !!!");
            exception.printStackTrace();
        }
        if (topic.getTag().equals("*")) {
            Exception exception = new RuntimeException("You can not allow use \"*\" as tag !!!");
            exception.printStackTrace();
        }

    }

    // 如果当前tag不存在则添加
    private static void initMapWithTopic(Topic topic) {
        // putIfAbsent如果不存在当前put的key值，则put成功，返回null值
        // 如果当前map已经存在该key，那么返回已存在key对应的value值
        GlobalrequestTopicMap.putIfAbsent(topic, new AtomicLong(0));
        GlobalPeriodSecondTopicMap.putIfAbsent(topic, new LinkedList<Long[]>());
        GlobalPeriodMinuteTopicMap.putIfAbsent(topic, new LinkedList<Long[]>());
        GlobalSecondTopicMap.putIfAbsent(topic, new LinkedBlockingQueue<Meterinfo>());
        GlobalMinuteTopicMap.putIfAbsent(topic, new LinkedBlockingQueue<Meterinfo>());
    }

    /**
     * 统计nums次成功请求，如果没有tag参数则当做DEFAUTTAG相同类型统计
     *
     * @author tantexian, <my.oschina.net/tantexian>
     * @params
     * @since 2017/4/7
     */
    public void request(long nums) {
        request(DEFAUTTOPIC, nums);
    }

    /**
     * 推送统计信息
     *
     * @author tantexian, <my.oschina.net/tantexian>
     * @params
     * @since 2017/4/6
     */
    private void pushAcquireMeterinfo() {
        if (isPush == false) {
            isPush = true;
            push();
        }
    }

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

    // 根据model类型，推送对应数据给用户
    private void processMeterQueue(IntervalModel model) {
        List<Meterinfo> meterList = new ArrayList<Meterinfo>();
        Map<Topic, LinkedBlockingQueue<Meterinfo>> meterSecondOrMinuteTopicMap = new ConcurrentHashMap<Topic, LinkedBlockingQueue<Meterinfo>>();
        switch (model) {
            case SECOND:
                meterSecondOrMinuteTopicMap = GlobalSecondTopicMap;
                break;
            case MINUTE:
                meterSecondOrMinuteTopicMap = GlobalMinuteTopicMap;
                break;
        }

        // System.out.println("meterSecondOrMinuteTopicMap == " + meterSecondOrMinuteTopicMap);
        for (Map.Entry<Topic, LinkedBlockingQueue<Meterinfo>> entry : meterSecondOrMinuteTopicMap.entrySet()) {
            Topic topic = entry.getKey();
            final LinkedBlockingQueue<Meterinfo> meterinfoQueue = entry.getValue();

            // 如果当前acquireTopic==null，或者Topic的tag为*，推送所有信息
            // 如果当前tag与用户设置获取的tag相同，或者acquireTopic的key与当前信息key相同则放置到推送列表中
            boolean needPush = false;
            if (this.acquireTopic == null || this.acquireTopic.getTag().equals("*") || this.acquireTopic.equals(topic)) {
                if (this.acquireTopic == null) {
                    needPush = true;
                } else if (this.acquireTopic.getType() == null || this.acquireTopic.getType() != null && this.acquireTopic.getType().equals(topic.getType())) {
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
                    meterSecondOrMinuteTopicMap.get(info.getTopic()).remove(info);
                }
                break;
            case REACQUIRE_LATER:
                break;
        }
    }

}
