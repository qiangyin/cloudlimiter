package com.cloudzone.cloudlimiter.limiter;

import com.cloudzone.cloudlimiter.factory.CloudFactory;
import com.cloudzone.cloudlimiter.meter.CloudMeter;
import com.cloudzone.cloudlimiter.meter.MeterListenerIpml;
import org.junit.Test;

import java.util.Date;

/**
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/14
 */
public class LimiterDelayExecutorTest {

    public static void main(String[] args) {
        final RealTimeLimiter realTimeLimiter = CloudFactory.createRealTimeLimiter(LimiterDelayConstants.ONCE_PER_HOUR);

        while (true) {
            realTimeLimiter.acquire();
            System.out.println(new Date());
        }
    }

    @Test
    public void test1() {
        final RealTimeLimiter realTimeLimiter = CloudFactory.createRealTimeLimiter(LimiterDelayConstants.ONCE_PER_SECOND);
        int i = 0;
        while (true) {
            i++;
            if (i % 10 == 0) {
                System.out.println("ONCE_PER_MINUTE");
                realTimeLimiter.setRate(LimiterDelayConstants.ONCE_PER_MINUTE);
            } else {
                realTimeLimiter.setRate(LimiterDelayConstants.ONCE_PER_SECOND);

            }
            realTimeLimiter.acquire();
            System.out.println(new Date());
        }
    }

    @Test
    public void test3() {
        final RealTimeLimiter realTimeLimiter = CloudFactory.createRealTimeLimiter(LimiterDelayConstants.ONCE_PER_MINUTE);
        CloudMeter cloudMeter = CloudFactory.createCloudMeter();
        cloudMeter.registerListener(new MeterListenerIpml());

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // CloudTicker.sleepSeconds(10);
                // 此处启动线程设置1000必须等到一分钟后才生效（因为初始化为每分钟执行一次，不允许中断）
                System.out.println("---1000");
                realTimeLimiter.setRate(1000);
            }
        });

        thread.start();
        int i = 0;
        while (true) {
            i++;
            realTimeLimiter.acquire();
            cloudMeter.request();
            // System.out.println(new Date());
        }


    }
}
