package com.cloudzone.cloudlimiter.meter;

import com.cloudzone.cloudlimiter.factory.CloudFactory;
import com.cloudzone.cloudlimiter.limiter.RealTimeLimiter;
import org.junit.Test;

/**
 * @author tantexian<my.oschina.net/tantexian>
 * @since 2017/4/4
 */
public class CloudMeterTest {
    final static private RealTimeLimiter realTimeLimiter = CloudFactory.createRealTimeLimiter(1000);
    final static private CloudMeter cloudMeter = new CloudMeter();

    @Test
    public void printStats() throws Exception {

        for (int i = 0; i < 1000000; i++) {
            if (i == 100) {
                System.out.println("100 " + i);
                realTimeLimiter.setRate(100);
            } else if (i == 600) {
                System.out.println("1000 " + i);
                realTimeLimiter.setRate(1000);
            } else if (i == 6000) {
                System.out.println("10000 " + i);
                realTimeLimiter.setRate(10000);
            }
            realTimeLimiter.acquire();
            cloudMeter.request();
            if (i % 1000 == 0) {
                cloudMeter.printStats();
            }

        }
    }

}