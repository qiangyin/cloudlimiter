package com.cloudzone.cloudlimiter.meter;

import com.cloudzone.cloudlimiter.base.AcquireStatus;
import com.cloudzone.cloudlimiter.base.IntervalModel;
import com.cloudzone.cloudlimiter.base.MeterListenner;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/5
 */
public class CloudMeter {
    private static final String DEFAUTTAG = "DefaultTag";
    private static final ConcurrentHashMap<String, AtomicLong> GlobalrequestTagMap = new ConcurrentHashMap<String, AtomicLong>();

    // 队列中保存每个tag最近的60秒的TPS值
    private final int LASTERSECONDNUM = 60;

    // 队列中保存每个tag最近的10分钟的TPS值
    private final int LASTERMINUTENUM = 10;

    private static final ConcurrentHashMap<String, LinkedBlockingQueue<Meterinfo>> GlobalSecondTagMap = new ConcurrentHashMap<String, LinkedBlockingQueue<Meterinfo>>();
    private static final ConcurrentHashMap<String, LinkedBlockingQueue<Meterinfo>> GlobalMinuteTagMap = new ConcurrentHashMap<String, LinkedBlockingQueue<Meterinfo>>();


    final static ConcurrentHashMap<String, LinkedList<Long[]>> GlobalPeriodSecondTagMap = new ConcurrentHashMap<String, LinkedList<Long[]>>();
    final static ConcurrentHashMap<String, LinkedList<Long[]>> GlobalPeriodMinuteTagMap = new ConcurrentHashMap<String, LinkedList<Long[]>>();


    final static int SECOND = 1000;
    final static int MINUTE = 1000 * 60;


    private final ScheduledExecutorService scheduledExecutorService;


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
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        start();
    }

    public void shutdown() {
        this.scheduledExecutorService.shutdown();
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
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
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
                            if (GlobalSecondTagMap.get(tag).size() > LASTERSECONDNUM) {
                                GlobalSecondTagMap.get(tag).poll();
                            }
                            GlobalSecondTagMap.get(tag).add(meterinfo);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void meterPerMinute() {
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
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
                            if (GlobalSecondTagMap.get(tag).size() > LASTERMINUTENUM) {
                                GlobalSecondTagMap.get(tag).poll();
                            }
                            GlobalSecondTagMap.get(tag).add(meterinfo);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }, 0, 1, TimeUnit.MINUTES);
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
            initMapWithTag(tag);
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
            // 如果当前tag不存在则添加
            initMapWithTag(tag);
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

    private static void initMapWithTag(String tag) {
        // 如果当前tag不存在则添加
        GlobalPeriodSecondTagMap.putIfAbsent(tag, new LinkedList<Long[]>());
        GlobalPeriodMinuteTagMap.putIfAbsent(tag, new LinkedList<Long[]>());
        GlobalSecondTagMap.putIfAbsent(tag, new LinkedBlockingQueue<Meterinfo>());
        GlobalMinuteTagMap.putIfAbsent(tag, new LinkedBlockingQueue<Meterinfo>());
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
        Map<String, LinkedBlockingQueue<Meterinfo>> meterSecondOrMinuteTagMap = new ConcurrentHashMap<String, LinkedBlockingQueue<Meterinfo>>();
        switch (model) {
            case SECOND:
                meterSecondOrMinuteTagMap = GlobalSecondTagMap;
                break;
            case MINUTE:
                meterSecondOrMinuteTagMap = GlobalMinuteTagMap;
                break;
        }

        for (Map.Entry<String, LinkedBlockingQueue<Meterinfo>> entry : meterSecondOrMinuteTagMap.entrySet()) {
            String tag = entry.getKey();
            final LinkedBlockingQueue<Meterinfo> meterinfoQueue = entry.getValue();

            // 如果当前推送的tag为*，或者当前tag与用户设置获取的tag相同则放置到推送列表中
            if (this.acquireTag.equals("*") || this.acquireTag.equals(tag)) {
                for (Meterinfo info : meterinfoQueue) {
                    meterList.add(info);
                }
            }
        }

        AcquireStatus acquireStatus = this.meterListenner.acquireStats(meterList);
        switch (acquireStatus) {
            case ACQUIRE_SUCCESS:
                for (Meterinfo info : meterList) {
                    meterSecondOrMinuteTagMap.get(info.getTag()).remove(info);
                }
                break;
            case REACQUIRE_LATER:
                break;
        }
    }

}
