package com.cloudzone.cloudlimiter.limiter;

import com.cloudzone.cloudlimiter.base.FlowUnit;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author tantexian<my.oschina.net/tantexian>
 * @since 2017/4/2
 */
public class FlowUnitTest {

    @Test
    public void unitTest() {

        System.out.println(FlowUnit.BYTE);

        System.out.println(TimeUnit.NANOSECONDS);

        System.out.println(FlowUnit.PB.toByte(1));
    }
}