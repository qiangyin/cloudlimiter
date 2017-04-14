package com.cloudzone.cloudlimiter.limiter;

import com.cloudzone.cloudlimiter.factory.CloudFactory;
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
                realTimeLimiter.setRate(LimiterDelayConstants.ONCE_PER_MINUTE);
            } else {
                realTimeLimiter.setRate(LimiterDelayConstants.ONCE_PER_SECOND);

            }
            realTimeLimiter.acquire();
            System.out.println(new Date());
        }
    }
}
