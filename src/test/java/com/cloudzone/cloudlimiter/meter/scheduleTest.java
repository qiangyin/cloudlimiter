package com.cloudzone.cloudlimiter.meter;

import com.cloudzone.cloudlimiter.limiter.CloudTicker;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/5
 */
public class scheduleTest {

    final static Timer timer1 = new Timer("CloudMeterTimer", true);
    final static Timer timer2 = new Timer("CloudMeterTimer", true);
    static int i = 0;

    @Test
    public void sche() {
        timer1.schedule(new TimerTask() {
            @Override
            public void run() {
                send(i++);
            }
        }, 0, 5 * 1000);
        CloudTicker.sleepSeconds(1000);
    }

    @Test
    public void scheFixed() {
        timer2.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                send(i++);
            }
        }, 0, 5 * 1000);
        CloudTicker.sleepSeconds(1000);
    }

    @Test
    public void ScheduledRateExecutor() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                send(i++);
            }
        }, 0, 1, TimeUnit.SECONDS);
        CloudTicker.sleepSeconds(1000);
    }

    @Test
    public void ScheduledDelayExecutor() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
        executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                send(i++);
            }
        }, 0, 1, TimeUnit.SECONDS);
        CloudTicker.sleepSeconds(1000);
    }


    @Test
    public void test1() {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date startDate = null;
        try {
            startDate = dateFormatter.parse("2017/4/5 16:30:00");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                try {
                    Thread.sleep(6000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("now time:" + System.currentTimeMillis() + " execute task!" + this.scheduledExecutionTime());
            }
        }, startDate, 5 * 1000);
        CloudTicker.sleepSeconds(1000);
    }


    public static void send(int max) {
        System.out.println("---- send start " + System.currentTimeMillis());
        /*if (i % 5 == 0) {
            CloudTicker.sleepSeconds(2);
            System.out.println("sleeping...");
        } else {
            CloudTicker.sleepMicros(1000 * 800);
        }*/
        CloudTicker.sleepSeconds(6);
        System.out.println("---- send end   " + System.currentTimeMillis());
        System.out.println();
    }


}
