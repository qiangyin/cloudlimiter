package com.cloudzone.cloudlimiter.meter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author tantexian
 * @since 2017/3/30
 */
public class StatsBenchmark {
    // 1
    private final AtomicLong sendRequestSuccessCount = new AtomicLong(0L);
    // 2
    private final AtomicLong sendRequestFailedCount = new AtomicLong(0L);
    // 3
    private final AtomicLong receiveResponseSuccessCount = new AtomicLong(0L);
    // 4
    private final AtomicLong receiveResponseFailedCount = new AtomicLong(0L);
    // 5
    private final AtomicLong sendSuccessTimeTotal = new AtomicLong(0L);
    // 6
    private final AtomicLong sendMaxRT = new AtomicLong(0L);


    public Long[] createSnapshot() {
        Long[] snap = new Long[]{//
                System.currentTimeMillis(),//
                this.sendRequestSuccessCount.get(),//
                this.sendRequestFailedCount.get(),//
                this.receiveResponseSuccessCount.get(),//
                this.receiveResponseFailedCount.get(),//
                this.sendSuccessTimeTotal.get(), //
        };

        return snap;
    }


    public AtomicLong getSendRequestSuccessCount() {
        return sendRequestSuccessCount;
    }


    public AtomicLong getSendRequestFailedCount() {
        return sendRequestFailedCount;
    }


    public AtomicLong getReceiveResponseSuccessCount() {
        return receiveResponseSuccessCount;
    }


    public AtomicLong getReceiveResponseFailedCount() {
        return receiveResponseFailedCount;
    }


    public AtomicLong getSendSuccessTimeTotal() {
        return sendSuccessTimeTotal;
    }


    public AtomicLong getSendMaxRT() {
        return sendMaxRT;
    }
}
