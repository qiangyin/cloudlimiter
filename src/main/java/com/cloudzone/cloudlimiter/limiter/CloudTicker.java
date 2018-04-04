package com.cloudzone.cloudlimiter.limiter;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/1
 */
public abstract class CloudTicker {
    /**
     * Constructor for use by subclasses.
     */
    protected CloudTicker() {
    }

    /**
     * Returns the number of nanoseconds elapsed since this ticker's fixed
     * point of reference.
     */
    public abstract long read();

    /**
     * A ticker that reads the current time using {@link System#nanoTime}.
     *
     * @author tantexian, <my.oschina.net/tantexian>
     * @since 2017/4/1
     */
    public static CloudTicker systemTicker() {
        return SYSTEM_TICKER;
    }

    private static final CloudTicker SYSTEM_TICKER = new CloudTicker() {
        @Override
        public long read() {
            return System.nanoTime();
        }
    };

    /**
     * 阻塞睡眠sleepForMicros微秒
     *
     * @author tantexian, <my.oschina.net/tantexian>
     * @params sleepForMicros 睡眠的微秒数
     * @since 2017/4/1
     */
    public static void sleepMicros(long sleepForMicros) {
        boolean interrupted = false;
        try {
            long remainingNanos = TimeUnit.MICROSECONDS.toNanos(sleepForMicros);
            long end = System.nanoTime() + remainingNanos;
            while (true) {
                try {
                    // TimeUnit.sleep() treats negative timeouts just like zero.
                    NANOSECONDS.sleep(remainingNanos);
                    return;
                } catch (InterruptedException e) {
                    interrupted = true;
                    remainingNanos = end - System.nanoTime();
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 阻塞睡眠sleepForSecond秒
     *
     * @author tantexian, <my.oschina.net/tantexian>
     * @params sleepForSecond 睡眠的秒数
     * @since 2017/4/1
     */
    public static void sleepSeconds(double sleepForSecond) {
        long sleepForMicrs = (long) (sleepForSecond * 1000 * 1000);
        sleepMicros(sleepForMicrs);
    }

    /**
     * 阻塞睡眠sleepForMills秒
     *
     * @author tantexian, <my.oschina.net/tantexian>
     * @params sleepForMicros 睡眠的微秒数
     * @since 2017/4/1
     */
    public static void sleepMillis(double sleepForMills) {
        long sleepForMicrs = (long) (sleepForMills * 1000);
        sleepMicros(sleepForMicrs);
    }

    /**
     * 为了保护SYSTEM_TICKER的值，SleepingTicker不对外开放
     *
     * @author tantexian, <my.oschina.net/tantexian>
     * @since 2017/4/1
     */
    static abstract class SleepingTicker extends CloudTicker {
        final static SleepingTicker SYSTEM_TICKER = new SleepingTicker() {
            @Override
            public long read() {
                return systemTicker().read();
            }
        };
    }
}
