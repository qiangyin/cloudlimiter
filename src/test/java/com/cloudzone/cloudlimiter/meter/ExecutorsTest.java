package com.cloudzone.cloudlimiter.meter;

import com.cloudzone.cloudlimiter.limiter.CloudTicker;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/14
 */
public class ExecutorsTest {
    private final static ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    static int num1 = 0;
    static int num2 = 0;

    public static void main(String[] args) {
        test1();
        test2();
    }

    public static void test1() {
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                printTime("test1: ");
                num1++;
                if (num1 % 3 == 0) {
                    CloudTicker.sleepMillis(3800);
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public static void test2() {
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                num2++;
                printTime("------------ test2: ");
                if (num2 % 5 == 0) {
                    CloudTicker.sleepMillis(500);
                }
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    private static void printTime(String tag) {
        System.out.println(tag + System.currentTimeMillis());
    }
}
