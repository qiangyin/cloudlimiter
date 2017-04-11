package com.cloudzone.cloudlimiter.meter;

import com.cloudzone.cloudlimiter.limiter.CloudTicker;
import org.junit.Test;

import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/6
 */
public class currentMapTest {
    private final static ConcurrentHashMap<Integer, LinkedList<Long>> integerLinkedListConcurrentHashMap = new ConcurrentHashMap<Integer, LinkedList<Long>>();

    @Test
    public void test1() {
        ConcurrentHashMap<Integer, AtomicLong> mymap = new ConcurrentHashMap<Integer, AtomicLong>();
        mymap.put(1, new AtomicLong(5));
        // 如果不存在该key，则put成功，否则返回老值
        AtomicLong val = new AtomicLong(8);
        AtomicLong atomiclong = mymap.putIfAbsent(2, val);
        if (atomiclong != null) {
            System.out.println("已经存在获取之前的老值，atomiclong");
            System.out.println(atomiclong);
        } else {
            System.out.println("如果为null，则说明已经put成功。");
            val.addAndGet(100);
            System.out.println("add 100 after val ==" + mymap.get(2));
        }

        AtomicLong again = mymap.putIfAbsent(2, new AtomicLong(44));
        if (again != null) {
            System.out.println("返回值即为当前put进入的value again：");
            System.out.println(again);
        } else {
            System.out.println("如果为null，则说明已经put成功。");
        }

        AtomicLong atomiclong1 = mymap.get(2);
        System.out.println(atomiclong1);

        atomiclong1.addAndGet(10);// 此处对atomiclong1变动会影响到map中的value值

        AtomicLong atomiclong3 = mymap.get(2);
        System.out.println(atomiclong3);
    }

    @Test
    public void test2() {

        integerLinkedListConcurrentHashMap.put(1, new LinkedList<Long>());
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (Map.Entry<Integer, LinkedList<Long>> entry : integerLinkedListConcurrentHashMap.entrySet()) {
                    entry.getKey();
                    LinkedList<Long> list = entry.getValue();
                    list.addLast(System.currentTimeMillis());
                    System.out.println(integerLinkedListConcurrentHashMap.get(1));
                }
            }
        }, 1000, 1000);

        CloudTicker.sleepSeconds(10000);
    }
}
