package com.cloudzone.cloudlimiter.limiter;

import com.cloudzone.cloudlimiter.factory.CloudFactory;

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
}
