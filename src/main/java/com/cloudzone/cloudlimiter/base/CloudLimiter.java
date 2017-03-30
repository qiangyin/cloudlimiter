package com.cloudzone.cloudlimiter.base;

import com.cloudzone.cloudlimiter.flow.FlowType;
import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Ticker;
import com.google.common.util.concurrent.Uninterruptibles;

import java.util.concurrent.TimeUnit;

/**
 * @author tantexian<my.oschina.net/tantexian>
 * @since 2017/3/30
 */
@Beta
public abstract class CloudLimiter {
    private final CloudLimiter.SleepingTicker ticker;
    private final long offsetNanos;
    double storedPermits;
    double maxPermits;
    volatile double stableIntervalMicros;
    private final Object mutex;
    private long nextFreeTicketMicros;

    public static CloudLimiter create(double permitsPerSecond) {
        return create(CloudLimiter.SleepingTicker.SYSTEM_TICKER, permitsPerSecond);
    }

    @VisibleForTesting
    public static CloudLimiter create(CloudLimiter.SleepingTicker ticker, double permitsPerSecond) {
        CloudLimiter.Bursty cloudLimiter = new CloudLimiter.Bursty(ticker, 1.0D);
        cloudLimiter.setRate(permitsPerSecond);
        return cloudLimiter;
    }

    public static CloudLimiter create(double permitsPerSecond, long warmupPeriod, TimeUnit unit) {
        return create(CloudLimiter.SleepingTicker.SYSTEM_TICKER, permitsPerSecond, warmupPeriod, unit);
    }

    @VisibleForTesting
    static CloudLimiter create(CloudLimiter.SleepingTicker ticker, double permitsPerSecond, long warmupPeriod, TimeUnit unit) {
        CloudLimiter.WarmingUp cloudLimiter = new CloudLimiter.WarmingUp(ticker, warmupPeriod, unit);
        cloudLimiter.setRate(permitsPerSecond);
        return cloudLimiter;
    }

    @VisibleForTesting
    static CloudLimiter createWithCapacity(CloudLimiter.SleepingTicker ticker, double permitsPerSecond, long maxBurstBuildup, TimeUnit unit) {
        double maxBurstSeconds = (double) unit.toNanos(maxBurstBuildup) / 1.0E9D;
        CloudLimiter.Bursty cloudLimiter = new CloudLimiter.Bursty(ticker, maxBurstSeconds);
        cloudLimiter.setRate(permitsPerSecond);
        return cloudLimiter;
    }

    private CloudLimiter(CloudLimiter.SleepingTicker ticker) {
        this.mutex = new Object();
        this.nextFreeTicketMicros = 0L;
        this.ticker = ticker;
        this.offsetNanos = ticker.read();
    }

    public final void setRate(double permitsPerSecond) {
        Preconditions.checkArgument(permitsPerSecond > 0.0D && !Double.isNaN(permitsPerSecond), "rate must be positive");
        Object var3 = this.mutex;
        synchronized (this.mutex) {
            this.resync(this.readSafeMicros());
            double stableIntervalMicros = (double) TimeUnit.SECONDS.toMicros(1L) / permitsPerSecond;
            this.stableIntervalMicros = stableIntervalMicros;
            this.doSetRate(permitsPerSecond, stableIntervalMicros);
        }
    }

    abstract void doSetRate(double var1, double var3);

    public final double getRate() {
        return (double) TimeUnit.SECONDS.toMicros(1L) / this.stableIntervalMicros;
    }

    public double acquire() {
        return this.acquire(1);
    }

    public double acquire(int permits) {
        long microsToWait = this.reserve(permits);
        this.ticker.sleepMicrosUninterruptibly(microsToWait * 1000);
        return 1.0D * (double) microsToWait / (double) TimeUnit.SECONDS.toMicros(1L);
    }

    long reserve() {
        return this.reserve(1);
    }

    long reserve(int permits) {
        checkPermits(permits);
        Object var2 = this.mutex;
        synchronized (this.mutex) {
            return this.reserveNextTicket((double) permits, this.readSafeMicros());
        }
    }

    public boolean tryAcquire(long timeout, TimeUnit unit) {
        return this.tryAcquire(1, timeout, unit);
    }

    public boolean tryAcquire(int permits) {
        return this.tryAcquire(permits, 0L, TimeUnit.MICROSECONDS);
    }

    public boolean tryAcquire() {
        return this.tryAcquire(1, 0L, TimeUnit.MICROSECONDS);
    }

    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
        long timeoutMicros = unit.toMicros(timeout);
        checkPermits(permits);
        Object var9 = this.mutex;
        long microsToWait;
        synchronized (this.mutex) {
            long nowMicros = this.readSafeMicros();
            if (this.nextFreeTicketMicros > nowMicros + timeoutMicros) {
                return false;
            }

            microsToWait = this.reserveNextTicket((double) permits, nowMicros);
        }

        this.ticker.sleepMicrosUninterruptibly(microsToWait);
        return true;
    }

    private static void checkPermits(int permits) {
        Preconditions.checkArgument(permits > 0, "Requested permits must be positive");
    }

    private long reserveNextTicket(double requiredPermits, long nowMicros) {
        this.resync(nowMicros);
        long microsToNextFreeTicket = Math.max(0L, this.nextFreeTicketMicros - nowMicros);
        double storedPermitsToSpend = Math.min(requiredPermits, this.storedPermits);
        double freshPermits = requiredPermits - storedPermitsToSpend;
        long waitMicros = this.storedPermitsToWaitTime(this.storedPermits, storedPermitsToSpend) + (long) (freshPermits * this.stableIntervalMicros);
        this.nextFreeTicketMicros += waitMicros;
        this.storedPermits -= storedPermitsToSpend;
        return microsToNextFreeTicket;
    }

    abstract long storedPermitsToWaitTime(double var1, double var3);

    private void resync(long nowMicros) {
        if (nowMicros > this.nextFreeTicketMicros) {
            this.storedPermits = Math.min(this.maxPermits, this.storedPermits + (double) (nowMicros - this.nextFreeTicketMicros) / this.stableIntervalMicros);
            this.nextFreeTicketMicros = nowMicros;
        }

    }

    private long readSafeMicros() {
        return TimeUnit.NANOSECONDS.toMicros(this.ticker.read() - this.offsetNanos);
    }

    public String toString() {
        return String.format("CloudLimiter[stableRate=%3.1fqps]", new Object[]{Double.valueOf(1000000.0D / this.stableIntervalMicros)});
    }

    @VisibleForTesting
    abstract static class SleepingTicker extends Ticker {
        static final CloudLimiter.SleepingTicker SYSTEM_TICKER = new CloudLimiter.SleepingTicker() {
            public long read() {
                return systemTicker().read();
            }

            public void sleepMicrosUninterruptibly(long micros) {
                if (micros > 0L) {
                    Uninterruptibles.sleepUninterruptibly(micros, TimeUnit.MICROSECONDS);
                }

            }
        };

        SleepingTicker() {
        }

        abstract void sleepMicrosUninterruptibly(long var1);
    }

    private static class Bursty extends CloudLimiter {
        final double maxBurstSeconds;

        Bursty(CloudLimiter.SleepingTicker ticker, double maxBurstSeconds) {
            super(ticker);
            this.maxBurstSeconds = maxBurstSeconds;
        }

        void doSetRate(double permitsPerSecond, double stableIntervalMicros) {
            double oldMaxPermits = this.maxPermits;
            this.maxPermits = this.maxBurstSeconds * permitsPerSecond;
            this.storedPermits = oldMaxPermits == 0.0D ? 0.0D : this.storedPermits * this.maxPermits / oldMaxPermits;
        }

        long storedPermitsToWaitTime(double storedPermits, double permitsToTake) {
            return 10L;
        }
    }

    private static class WarmingUp extends CloudLimiter {
        final long warmupPeriodMicros;
        private double slope;
        private double halfPermits;

        WarmingUp(CloudLimiter.SleepingTicker ticker, long warmupPeriod, TimeUnit timeUnit) {
            super(ticker);
            this.warmupPeriodMicros = timeUnit.toMicros(warmupPeriod);
        }

        void doSetRate(double permitsPerSecond, double stableIntervalMicros) {
            double oldMaxPermits = this.maxPermits;
            this.maxPermits = (double) this.warmupPeriodMicros / stableIntervalMicros;
            this.halfPermits = this.maxPermits / 2.0D;
            double coldIntervalMicros = stableIntervalMicros * 3.0D;
            this.slope = (coldIntervalMicros - stableIntervalMicros) / this.halfPermits;
            if (oldMaxPermits == 1.0D / 0.0) {
                this.storedPermits = 0.0D;
            } else {
                this.storedPermits = oldMaxPermits == 0.0D ? this.maxPermits : this.storedPermits * this.maxPermits / oldMaxPermits;
            }

        }

        long storedPermitsToWaitTime(double storedPermits, double permitsToTake) {
            double availablePermitsAboveHalf = storedPermits - this.halfPermits;
            long micros = 0L;
            if (availablePermitsAboveHalf > 0.0D) {
                double permitsAboveHalfToTake = Math.min(availablePermitsAboveHalf, permitsToTake);
                micros = (long) (permitsAboveHalfToTake * (this.permitsToTime(availablePermitsAboveHalf) + this.permitsToTime(availablePermitsAboveHalf - permitsAboveHalfToTake)) / 2.0D);
                permitsToTake -= permitsAboveHalfToTake;
            }

            micros = (long) ((double) micros + this.stableIntervalMicros * permitsToTake);
            return micros;
        }

        private double permitsToTime(double permits) {
            return this.stableIntervalMicros + permits * this.slope;
        }
    }

    private static class Flow extends CloudLimiter {

        Flow(CloudLimiter.SleepingTicker ticker) {
            super(ticker);
        }

        void doSetRate(double permitsPerSecond, double stableIntervalMicros) {
        }

        long storedPermitsToWaitTime(double storedPermits, double permitsToTake) {
            return 0L;
        }

        public static CloudLimiter limiterPerSecond(FlowType flowLimit) {
            return create(CloudLimiter.SleepingTicker.SYSTEM_TICKER, flowLimit.getValue());
        }
    }
}
