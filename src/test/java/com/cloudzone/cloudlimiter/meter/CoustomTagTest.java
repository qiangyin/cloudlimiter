package com.cloudzone.cloudlimiter.meter;

import com.cloudzone.cloudlimiter.factory.CloudFactory;
import com.cloudzone.cloudlimiter.limiter.RealTimeLimiter;

/**
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/14
 */
public class CoustomTagTest {
    public static void main(String[] args) {
        RealTimeLimiter realTimeLimiter = CloudFactory.createRealTimeLimiter(10);
        CloudMeter cloudMeter = CloudFactory.createCloudMeter();
        cloudMeter.registerListener(new MeterListenerIpml());

        String str1 = "1";
        String str2 = "2";
        String tag = "tag-";
        int i = 0;
        while (true) {
            i++;
            realTimeLimiter.acquire();
            if (i % 5 == 0) {
                cloudMeter.request(tag + str1);
            } else {
                cloudMeter.request(tag + str2);
            }
        }
    }
}
