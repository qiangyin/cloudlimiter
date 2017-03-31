package com.cloudzone.cloudlimiter.base;

import com.cloudzone.cloudlimiter.flow.FlowType;

import java.util.concurrent.TimeUnit;

/**
 * @author tantexian<my.oschina.net/tantexian>
 * @since 2017/3/30
 */
public abstract class CloudLimiter {
    private final CloudLimiter.SleepingCloudTicker CloudTicker;
    private final long offsetNanos;
    double storedPermits;
    double maxPermits;
    // 此值为每个令牌之间获取的间隔微秒数
    volatile double stableIntervalMicros;
    private final Object mutex;
    //
    private long nextFreeTicketMicros;

    // permitsPerSecond 每秒钟能够获取的令牌许可证数量
    public static CloudLimiter create(double permitsPerSecond) {
        // SYSTEM_CloudTicker为当前启动时System.nanoTime()
        return create(CloudLimiter.SleepingCloudTicker.SYSTEM_CloudTicker, permitsPerSecond);
    }

    // CloudTicker为初始启动时间ticker
    public static CloudLimiter create(CloudLimiter.SleepingCloudTicker CloudTicker, double permitsPerSecond) {
        CloudLimiter.Bursty cloudLimiter = new CloudLimiter.Bursty(CloudTicker, 1.0D);
        // 设置速率为每秒钟发放permitsPerSecond个令牌
        cloudLimiter.setRate(permitsPerSecond);
        return cloudLimiter;
    }

    public static CloudLimiter create(double permitsPerSecond, long warmupPeriod, TimeUnit unit) {
        return create(CloudLimiter.SleepingCloudTicker.SYSTEM_CloudTicker, permitsPerSecond, warmupPeriod, unit);
    }

    static CloudLimiter create(CloudLimiter.SleepingCloudTicker CloudTicker, double permitsPerSecond, long warmupPeriod, TimeUnit unit) {
        CloudLimiter.WarmingUp cloudLimiter = new CloudLimiter.WarmingUp(CloudTicker, warmupPeriod, unit);
        cloudLimiter.setRate(permitsPerSecond);
        return cloudLimiter;
    }

    static CloudLimiter createWithCapacity(CloudLimiter.SleepingCloudTicker CloudTicker, double permitsPerSecond, long maxBurstBuildup, TimeUnit unit) {
        double maxBurstSeconds = (double) unit.toNanos(maxBurstBuildup) / 1.0E9D;
        CloudLimiter.Bursty cloudLimiter = new CloudLimiter.Bursty(CloudTicker, maxBurstSeconds);
        cloudLimiter.setRate(permitsPerSecond);
        return cloudLimiter;
    }

    // 为了阻止此类被继承，使用了私有构造方法
    private CloudLimiter(CloudLimiter.SleepingCloudTicker CloudTicker) {
        // 创建全局锁对象
        this.mutex = new Object();
        // 表示下一次获取令牌票的剩余微秒数，初始化为0
        this.nextFreeTicketMicros = 0L;
        // 初始化启动ticker的纳秒
        this.CloudTicker = CloudTicker;
        // 获取从启动到目前的纳秒offset（其中CloudTicker.read()为获取当前的纳秒时间）
        this.offsetNanos = CloudTicker.read();
    }

    // 每秒钟发放的permitsPerSecond令牌数
    public final void setRate(double permitsPerSecond) {
        // 检查令牌数，必须大于0，且不能为非duoble数
        CloudPreconditions.checkArgument(permitsPerSecond > 0.0D && !Double.isNaN(permitsPerSecond), "rate must be positive");
        synchronized (this.mutex) {// 加锁
            // this.readSafeMicros()为当前与上次保存offset之间的微秒数
            this.resync(this.readSafeMicros());
            // 此值为每个令牌之间获取的间隔微秒数
            double stableIntervalMicros = (double) TimeUnit.SECONDS.toMicros(1L) / permitsPerSecond;
            this.stableIntervalMicros = stableIntervalMicros;
            // 此处代码对对应的子类实现
            this.doSetRate(permitsPerSecond, stableIntervalMicros);
        }
    }

    abstract void doSetRate(double d1, double d2);

    public final double getRate() {
        return (double) TimeUnit.SECONDS.toMicros(1L) / this.stableIntervalMicros;
    }

    // 此处封装默认参数为1
    public double acquire() {
        return this.acquire(1);
    }

    // 每次获取permits个令牌
    public double acquire(int permits) {
        final long start1 = System.nanoTime();
        // System.out.println("start- " + start1);
        // 获取permits个数令牌需要等待的微秒数
        long microsToWait = this.reserve(permits);
        //        System.out.println("microsToWait- " + microsToWait);
        long end1 = System.nanoTime();
        //        System.out.println("end- " + end1 + " diff == " + (end1 - start1));
        // 阻塞等待microsToWait秒
        this.CloudTicker.sleepMicrosUninterruptibly(microsToWait);
        // 返回 本次发放的令牌数等待的时间，单位为秒???
        return 1.0D * (double) microsToWait / (double) TimeUnit.SECONDS.toMicros(1L);
    }

    long reserve() {
        return this.reserve(1);
    }

    long reserve(int permits) {
        // 检查令牌数必须为整数
        checkPermits(permits);
        synchronized (this.mutex) {
            // 本次需要获取permits个令牌，this.readSafeMicros()为获取当前与上次保存offset之间的微秒数
            // 返回下一次获取令牌时间与当前时间的差值
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

        this.CloudTicker.sleepMicrosUninterruptibly(microsToWait);
        return true;
    }

    private static void checkPermits(int permits) {
        CloudPreconditions.checkArgument(permits > 0, "Requested permits must be positive");
    }

    // 本次需要获取permits个令牌，this.readSafeMicros()为获取当前与上次保存offset之间的微秒数。
    // requiredPermits为本次需要获取的令牌数
    private long reserveNextTicket(double requiredPermits, long nowMicros) {
        //
        this.resync(nowMicros);
        // 下一次获取令牌时间差值与当前时间差值大于零，则返回该值，否则为0
        long microsToNextFreeTicket = Math.max(0L, this.nextFreeTicketMicros - nowMicros);
        // 获取本次请求的令牌数及剩余存储的令牌数的最小值
        // 假若本次请求的令牌数requiredPermits为5个，已保存的令牌数storedPermits为3个，那么storedPermitsToSpend为3
        // 则新的令牌数freshPermits为2个
        double storedPermitsToSpend = Math.min(requiredPermits, this.storedPermits);
        double freshPermits = requiredPermits - storedPermitsToSpend;

        // 获取当前freshPermits数量的新令牌需要花费的时间
        long freshPermitsMicros = (long) (freshPermits * this.stableIntervalMicros);
        // Bursty算法中，storedPermitsToWaitTime永远返回0，此处waitMicros即为上述值
        long waitMicros = this.storedPermitsToWaitTime(this.storedPermits, storedPermitsToSpend) + freshPermitsMicros;
        // Bursty算法中（storedPermitsToWaitTime=0），所以此处为获取当前freshPermits数量的新令牌需要花费的时间
        this.nextFreeTicketMicros += waitMicros;
        // storedPermits则为当前保留的令牌数减去已经用掉的令牌数：storedPermits == storedPermits- storedPermitsToSpend == 0个，即本次将之前保存的令牌数全部消费掉了
        this.storedPermits -= storedPermitsToSpend;
        // 返回下一次获取令牌时间与当前时间的差值
        return microsToNextFreeTicket;
    }

    abstract long storedPermitsToWaitTime(double var1, double var3);

    // TODO nowMicros为当前与上一次offset微秒的时间间隔数
    private void resync(long nowMicros) {
        // 如果此间隔数大于到下一次的剩余时间纳秒数执行下述操作（否则直接返回）

        if (nowMicros > this.nextFreeTicketMicros) {
            System.out.println("1----------------------------------------------------------------");
            // 获取[最大令牌数]与[当前存储令牌数+]
            System.out.println("this.maxPermits ==" + this.maxPermits);
            System.out.println("this.storedPermits ==" + this.storedPermits);
            System.out.println("this.nowMicros ==" + nowMicros);
            System.out.println("this.nextFreeTicketMicros ==" + this.nextFreeTicketMicros);
            System.out.println("this.stableIntervalMicros ==" + this.stableIntervalMicros);
            System.out.println("2----------------------------------------------------------------");
            this.storedPermits = Math.min(this.maxPermits, this.storedPermits + (double) (nowMicros - this.nextFreeTicketMicros) / this.stableIntervalMicros);
            // 将当前与上一次offset微秒的时间间隔数赋值给nextFreeTicketMicros
            this.nextFreeTicketMicros = nowMicros;
        }

    }

    // 获取当前与上次保存offset之间的微秒数
    private long readSafeMicros() {
        return TimeUnit.NANOSECONDS.toMicros(this.CloudTicker.read() - this.offsetNanos);
    }

    public String toString() {
        return String.format("CloudLimiter[stableRate=%3.1fqps]", new Object[]{Double.valueOf(1000000.0D / this.stableIntervalMicros)});
    }

    abstract static class SleepingCloudTicker extends CloudTicker {
        static final CloudLimiter.SleepingCloudTicker SYSTEM_CloudTicker = new CloudLimiter.SleepingCloudTicker() {
            public long read() {
                return systemTicker().read();
            }

            // 阻塞等待micros微秒
            public void sleepMicrosUninterruptibly(long micros) {
                if (micros > 0L) {
                    CloudUninterruptibles.sleepUninterruptibly(micros, TimeUnit.MICROSECONDS);
                }

            }
        };

        SleepingCloudTicker() {
        }

        abstract void sleepMicrosUninterruptibly(long var1);
    }

    private static class Bursty extends CloudLimiter {
        final double maxBurstSeconds;// 默认为1.0

        // CloudTicker为初始启动时间ticker, maxBurstSeconds默认为1.0
        Bursty(CloudLimiter.SleepingCloudTicker CloudTicker, double maxBurstSeconds) {
            // 此处调用父类初始化
            super(CloudTicker);
            this.maxBurstSeconds = maxBurstSeconds;
        }

        // permitsPerSecond每秒发放的令牌数，stableIntervalMicros两个令牌之间的间隔微秒数
        void doSetRate(double permitsPerSecond, double stableIntervalMicros) {
            // 将当前maxPermits保存到oldMaxPermits
            double oldMaxPermits = this.maxPermits;
            // 计算当前maxPermits值
            this.maxPermits = this.maxBurstSeconds * permitsPerSecond;
            // 计算storedPermits值，如果oldMaxPermits值为0则返回0，否则为this.storedPermits * this.maxPermits / oldMaxPermits
            this.storedPermits = oldMaxPermits == 0.0D ? 0.0D : this.storedPermits * this.maxPermits / oldMaxPermits;
        }

        long storedPermitsToWaitTime(double storedPermits, double permitsToTake) {
            return 0L;
        }
    }

    private static class WarmingUp extends CloudLimiter {
        final long warmupPeriodMicros;
        private double slope;
        private double halfPermits;

        WarmingUp(CloudLimiter.SleepingCloudTicker CloudTicker, long warmupPeriod, TimeUnit timeUnit) {
            super(CloudTicker);
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

        Flow(CloudLimiter.SleepingCloudTicker CloudTicker) {
            super(CloudTicker);
        }

        void doSetRate(double permitsPerSecond, double stableIntervalMicros) {
        }

        long storedPermitsToWaitTime(double storedPermits, double permitsToTake) {
            return 0L;
        }

        public static CloudLimiter limiterPerSecond(FlowType flowLimit) {
            return create(CloudLimiter.SleepingCloudTicker.SYSTEM_CloudTicker, flowLimit.getValue());
        }
    }
}
