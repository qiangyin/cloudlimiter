package com.cloudzone.cloudlimiter.meter;

import com.cloudzone.cloudlimiter.base.IntervalModel;
import com.cloudzone.cloudlimiter.factory.CloudFactory;
import com.cloudzone.cloudlimiter.limiter.RealTimeLimiter;

/**
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/14
 */
public class MeterAccuracyTest {

    public static void main(String[] args) {
        RealTimeLimiter realTimeLimiter = CloudFactory.createRealTimeLimiter(10);
        CloudMeter cloudMeter = CloudFactory.createCloudMeter();
        cloudMeter.setIntervalModel(IntervalModel.MINUTE);
        cloudMeter.registerListener(new MeterListenerIpml());

        while (true) {
            realTimeLimiter.acquire();
            cloudMeter.request();
        }
    }
}
