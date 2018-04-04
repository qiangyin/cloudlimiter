package com.cloudzone.cloudlimiter.limiter;

import com.cloudzone.cloudlimiter.base.FlowUnit;
import com.cloudzone.cloudlimiter.factory.CloudFactory;

import java.util.Date;

/**
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/11
 */
public class FlowTest {

    public static void main(String[] args) {
        FlowLimiter flowLimiterPerMinute = CloudFactory.createFlowLimiterPerMinute(1000, FlowUnit.BYTE);

        for (int i = 0; i < 10000; i++) {
            flowLimiterPerMinute.acquire(1000, FlowUnit.BYTE);
            System.out.println(new Date());
        }
    }
}
