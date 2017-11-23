package com.cloudzone.cloudlimiter.base;

import java.util.concurrent.TimeUnit;

/**
 * A rate limiter. Conceptually, a rate limiter distributes permits at a
 * configurable rate. Each {@link #acquire()} blocks if necessary until a permit is
 * available, and then takes it. Once acquired, permits need not be released.
 * <p>
 * <p>Rate limiters are often used to restrict the rate at which some
 * physical or logical resource is accessed. This is in contrast to {@link
 * java.util.concurrent.Semaphore} which restricts the number of concurrent
 * accesses instead of the rate (note though that concurrency and rate are closely related,
 * e.g. see <a href="http://en.wikipedia.org/wiki/Little's_law">Little's Law</a>).
 * <p>
 * <p>A {@code GoogleCloudLimiter} is defined primarily by the rate at which permits
 * are issued. Absent additional configuration, permits will be distributed at a
 * fixed rate, defined in terms of permits per second. Permits will be distributed
 * smoothly, with the delay between individual permits being adjusted to ensure
 * that the configured rate is maintained.
 * <p>
 * <p>It is possible to configure a {@code GoogleCloudLimiter} to have a warmup
 * period during which time the permits issued each second steadily increases until
 * it hits the stable rate.
 * <p>
 * <p>As an example, imagine that we have a list of tasks to execute, but we don't want to
 * submit more than 2 per second:
 * <pre>  {@code
 *  final GoogleCloudLimiter rateLimiter = GoogleCloudLimiter.create(2.0); // rate is "2 permits per second"
 *  void submitTasks(List<Runnable> tasks, Executor executor) {
 *    for (Runnable task : tasks) {
 *      rateLimiter.acquire(); // may wait
 *      executor.execute(task);
 *    }
 *  }
 * }</pre>
 * <p>
 * <p>As another example, imagine that we produce a stream of data, and we want to cap it
 * at 5kb per second. This could be accomplished by requiring a permit per byte, and specifying
 * a rate of 5000 permits per second:
 * <pre>  {@code
 *  final GoogleCloudLimiter rateLimiter = GoogleCloudLimiter.create(5000.0); // rate = 5000 permits per second
 *  void submitPacket(byte[] packet) {
 *    rateLimiter.acquire(packet.length);
 *    networkService.send(packet);
 *  }
 * }</pre>
 * <p>
 * <p>It is important to note that the number of permits requested <i>never</i>
 * affect the throttling of the request itself (an invocation to {@code acquire(1)}
 * and an invocation to {@code acquire(1000)} will result in exactly the same throttling, if any),
 * but it affects the throttling of the <i>next</i> request. I.e., if an expensive task
 * arrives at an idle GoogleCloudLimiter, it will be granted immediately, but it is the <i>next</i>
 * request that will experience extra throttling, thus paying for the cost of the expensive
 * task.
 * <p>
 * <p>Note: {@code GoogleCloudLimiter} does not provide fairness guarantees.
 *
 * @author Dimitris Andreou
 * @since 13.0
 */
// TODO(user): switch to nano precision. A natural unit of cost is "bytes", and a micro precision
//     would mean a maximum rate of "1MB/s", which might be small in some cases.


@CloudBeta
public abstract class GoogleCloudLimiter {
  /*
   * How is the GoogleCloudLimiter designed, and why?
   *
   * The primary feature of a GoogleCloudLimiter is its "stable rate", the maximum rate that
   * is should allow at normal conditions. This is enforced by "throttling" incoming
   * requests as needed, i.e. compute, for an incoming request, the appropriate throttle time,
   * and make the calling thread wait as much.
   *
   * The simplest way to maintain a rate of QPS is to keep the timestamp of the last
   * granted request, and ensure that (1/QPS) seconds have elapsed since then. For example,
   * for a rate of QPS=5 (5 tokens per second), if we ensure that a request isn't granted
   * earlier than 200ms after the last one, then we achieve the intended rate.
   * If a request comes and the last request was granted only 100ms ago, then we wait for
   * another 100ms. At this rate, serving 15 fresh permits (i.e. for an acquire(15) request)
   * naturally takes 3 seconds.
   *
   * It is important to realize that such a GoogleCloudLimiter has a very superficial memory
   * of the past: it only remembers the last request. What if the GoogleCloudLimiter was unused for
   * a long period of time, then a request arrived and was immediately granted?
   * This GoogleCloudLimiter would immediately forget about that past underutilization. This may
   * result in either underutilization or overflow, depending on the real world consequences
   * of not using the expected rate.
   *
   * Past underutilization could mean that excess resources are available. Then, the GoogleCloudLimiter
   * should speed up for a while, to take advantage of these resources. This is important
   * when the rate is applied to networking (limiting bandwidth), where past underutilization
   * typically translates to "almost empty buffers", which can be filled immediately.
   *
   * On the other hand, past underutilization could mean that "the server responsible for
   * handling the request has become less ready for future requests", i.e. its caches become
   * stale, and requests become more likely to trigger expensive operations (a more extreme
   * case of this example is when a server has just booted, and it is mostly busy with getting
   * itself up to speed).
   *
   * To deal with such scenarios, we add an extra dimension, that of "past underutilization",
   * modeled by "storedPermits" variable. This variable is zero when there is no
   * underutilization, and it can grow up to maxStoredPermits, for sufficiently large
   * underutilization. So, the requested permits, by an invocation acquire(permits),
   * are served from:
   * - stored permits (if available)
   * - fresh permits (for any remaining permits)
   *
   * How this works is best explained with an example:
   *
   * For a GoogleCloudLimiter that produces 1 token per second, every second
   * that goes by with the GoogleCloudLimiter being unused, we increase storedPermits by 1.
   * Say we leave the GoogleCloudLimiter unused for 10 seconds (i.e., we expected a request at time
   * X, but we are at time X + 10 seconds before a request actually arrives; this is
   * also related to the point made in the last paragraph), thus storedPermits
   * becomes 10.0 (assuming maxStoredPermits >= 10.0). At that point, a request of acquire(3)
   * arrives. We serve this request out of storedPermits, and reduce that to 7.0 (how this is
   * translated to throttling time is discussed later). Immediately after, assume that an
   * acquire(10) request arriving. We serve the request partly from storedPermits,
   * using all the remaining 7.0 permits, and the remaining 3.0, we serve them by fresh permits
   * produced by the rate limiter.
   *
   * We already know how much time it takes to serve 3 fresh permits: if the rate is
   * "1 token per second", then this will take 3 seconds. But what does it mean to serve 7
   * stored permits? As explained above, there is no unique answer. If we are primarily
   * interested to deal with underutilization, then we want stored permits to be given out
   * /faster/ than fresh ones, because underutilization = free resources for the taking.
   * If we are primarily interested to deal with overflow, then stored permits could
   * be given out /slower/ than fresh ones. Thus, we require a (different in each case)
   * function that translates storedPermits to throtting time.
   *
   * This role is played by storedPermitsToWaitTime(double storedPermits, double permitsToTake).
   * The underlying model is a continuous function mapping storedPermits
   * (from 0.0 to maxStoredPermits) onto the 1/rate (i.e. intervals) that is effective at the given
   * storedPermits. "storedPermits" essentially measure unused time; we spend unused time
   * buying/storing permits. Rate is "permits / time", thus "1 / rate = time / permits".
   * Thus, "1/rate" (time / permits) times "permits" gives time, i.e., integrals on this
   * function (which is what storedPermitsToWaitTime() computes) correspond to minimum intervals
   * between subsequent requests, for the specified number of requested permits.
   *
   * Here is an example of storedPermitsToWaitTime:
   * If storedPermits == 10.0, and we want 3 permits, we take them from storedPermits,
   * reducing them to 7.0, and compute the throttling for these as a call to
   * storedPermitsToWaitTime(storedPermits = 10.0, permitsToTake = 3.0), which will
   * evaluate the integral of the function from 7.0 to 10.0.
   *
   * Using integrals guarantees that the effect of a single acquire(3) is equivalent
   * to { acquire(1); acquire(1); acquire(1); }, or { acquire(2); acquire(1); }, etc,
   * since the integral of the function in [7.0, 10.0] is equivalent to the sum of the
   * integrals of [7.0, 8.0], [8.0, 9.0], [9.0, 10.0] (and so on), no matter
   * what the function is. This guarantees that we handle correctly requests of varying weight
   * (permits), /no matter/ what the actual function is - so we can tweak the latter freely.
   * (The only requirement, obviously, is that we can compute its integrals).
   *
   * Note well that if, for this function, we chose a horizontal line, at height of exactly
   * (1/QPS), then the effect of the function is non-existent: we serve storedPermits at
   * exactly the same cost as fresh ones (1/QPS is the cost for each). We use this trick later.
   *
   * If we pick a function that goes /below/ that horizontal line, it means that we reduce
   * the area of the function, thus time. Thus, the GoogleCloudLimiter becomes /faster/ after a
   * period of underutilization. If, on the other hand, we pick a function that
   * goes /above/ that horizontal line, then it means that the area (time) is increased,
   * thus storedPermits are more costly than fresh permits, thus the GoogleCloudLimiter becomes
   * /slower/ after a period of underutilization.
   *
   * Last, but not least: consider a GoogleCloudLimiter with rate of 1 permit per second, currently
   * completely unused, and an expensive acquire(100) request comes. It would be nonsensical
   * to just wait for 100 seconds, and /then/ start the actual task. Why wait without doing
   * anything? A much better approach is to /allow/ the request right away (as if it was an
   * acquire(1) request instead), and postpone /subsequent/ requests as needed. In this version,
   * we allow starting the task immediately, and postpone by 100 seconds future requests,
   * thus we allow for work to get done in the meantime instead of waiting idly.
   *
   * This has important consequences: it means that the GoogleCloudLimiter doesn't remember the time
   * of the _last_ request, but it remembers the (expected) time of the _next_ request. This
   * also enables us to tell immediately (see tryAcquire(timeout)) whether a particular
   * timeout is enough to get us to the point of the next scheduling time, since we always
   * maintain that. And what we mean by "an unused GoogleCloudLimiter" is also defined by that
   * notion: when we observe that the "expected arrival time of the next request" is actually
   * in the past, then the difference (now - past) is the amount of time that the GoogleCloudLimiter
   * was formally unused, and it is that amount of time which we translate to storedPermits.
   * (We increase storedPermits with the amount of permits that would have been produced
   * in that idle time). So, if rate == 1 permit per second, and arrivals come exactly
   * one second after the previous, then storedPermits is _never_ increased -- we would only
   * increase it for arrivals _later_ than the expected one second.
   */

    /**
     * Creates a {@code GoogleCloudLimiter} with the specified stable throughput, given as
     * "permits per second" (commonly referred to as <i>QPS</i>, queries per second).
     * <p>
     * <p>The returned {@code GoogleCloudLimiter} ensures that on average no more than {@code
     * permitsPerSecond} are issued during any given second, with sustained requests
     * being smoothly spread over each second. When the incoming request rate exceeds
     * {@code permitsPerSecond} the rate limiter will release one permit every {@code
     * (1.0 / permitsPerSecond)} seconds. When the rate limiter is unused,
     * bursts of up to {@code permitsPerSecond} permits will be allowed, with subsequent
     * requests being smoothly limited at the stable rate of {@code permitsPerSecond}.
     *
     * @param permitsPerSecond the rate of the returned {@code GoogleCloudLimiter}, measured in
     *                         how many permits become available per second. Must be positive
     */
    // TODO(user): "This is equivalent to
    //                 {@code createWithCapacity(permitsPerSecond, 1, TimeUnit.SECONDS)}".
    public static GoogleCloudLimiter create(double permitsPerSecond) {
    /*
       * The default GoogleCloudLimiter configuration can save the unused permits of up to one second.
       * This is to avoid unnecessary stalls in situations like this: A GoogleCloudLimiter of 1qps,
       * and 4 threads, all calling acquire() at these moments:
       *
       * T0 at 0 seconds
       * T1 at 1.05 seconds
       * T2 at 2 seconds
       * T3 at 3 seconds
       *
       * Due to the slight delay of T1, T2 would have to sleep till 2.05 seconds,
       * and T3 would also have to sleep till 3.05 seconds.
     */
        return create(GoogleCloudLimiter.SleepingCloudTicker.SYSTEM_CloudTicker, permitsPerSecond);
    }

    public static GoogleCloudLimiter createWithCapacityOpen(double permitsPerSecond, long maxBurstBuildup, TimeUnit unit) {
        return createWithCapacity(GoogleCloudLimiter.SleepingCloudTicker.SYSTEM_CloudTicker, permitsPerSecond, maxBurstBuildup, unit);
    }

    @CloudVisibleForTesting
    static GoogleCloudLimiter create(GoogleCloudLimiter.SleepingCloudTicker CloudTicker, double permitsPerSecond) {
        GoogleCloudLimiter rateLimiter = new GoogleCloudLimiter.Bursty(CloudTicker, 1.0 /* maxBurstSeconds */);
        rateLimiter.setRate(permitsPerSecond);
        return rateLimiter;
    }

    /**
     * Creates a {@code GoogleCloudLimiter} with the specified stable throughput, given as
     * "permits per second" (commonly referred to as <i>QPS</i>, queries per second), and a
     * <i>warmup period</i>, during which the {@code GoogleCloudLimiter} smoothly ramps up its rate,
     * until it reaches its maximum rate at the end of the period (as long as there are enough
     * requests to saturate it). Similarly, if the {@code GoogleCloudLimiter} is left <i>unused</i> for
     * a duration of {@code warmupPeriod}, it will gradually return to its "cold" state,
     * i.e. it will go through the same warming up process as when it was first created.
     * <p>
     * <p>The returned {@code GoogleCloudLimiter} is intended for cases where the resource that actually
     * fulfills the requests (e.g., a remote server) needs "warmup" time, rather than
     * being immediately accessed at the stable (maximum) rate.
     * <p>
     * <p>The returned {@code GoogleCloudLimiter} starts in a "cold" state (i.e. the warmup period
     * will follow), and if it is left unused for long enough, it will return to that state.
     *
     * @param permitsPerSecond the rate of the returned {@code GoogleCloudLimiter}, measured in
     *                         how many permits become available per second. Must be positive
     * @param warmupPeriod     the duration of the period where the {@code GoogleCloudLimiter} ramps up its
     *                         rate, before reaching its stable (maximum) rate
     * @param unit             the time unit of the warmupPeriod argument
     */
    public static GoogleCloudLimiter create(double permitsPerSecond, long warmupPeriod, TimeUnit unit) {
        return create(GoogleCloudLimiter.SleepingCloudTicker.SYSTEM_CloudTicker, permitsPerSecond, warmupPeriod, unit);
    }

    @CloudVisibleForTesting
    static GoogleCloudLimiter create(
            GoogleCloudLimiter.SleepingCloudTicker CloudTicker, double permitsPerSecond, long warmupPeriod, TimeUnit unit) {
        GoogleCloudLimiter rateLimiter = new GoogleCloudLimiter.WarmingUp(CloudTicker, warmupPeriod, unit);
        rateLimiter.setRate(permitsPerSecond);
        return rateLimiter;
    }

    @CloudVisibleForTesting
    static GoogleCloudLimiter createWithCapacity(
            GoogleCloudLimiter.SleepingCloudTicker CloudTicker, double permitsPerSecond, long maxBurstBuildup, TimeUnit unit) {
        double maxBurstSeconds = unit.toNanos(maxBurstBuildup) / 1E+9;
        GoogleCloudLimiter.Bursty rateLimiter = new GoogleCloudLimiter.Bursty(CloudTicker, maxBurstSeconds);
        rateLimiter.setRate(permitsPerSecond);
        return rateLimiter;
    }

    /**
     * The underlying timer; used both to measure elapsed time and sleep as necessary. A separate
     * object to facilitate testing.
     */
    private final GoogleCloudLimiter.SleepingCloudTicker CloudTicker;

    /**
     * The timestamp when the GoogleCloudLimiter was created; used to avoid possible overflow/time-wrapping
     * errors.
     */
    private final long offsetNanos;

    /**
     * The currently stored permits.
     */
    double storedPermits;

    /**
     * The maximum number of stored permits.
     */
    double maxPermits;

    /**
     * The interval between two unit requests, at our stable rate. E.g., a stable rate of 5 permits
     * per second has a stable interval of 200ms.
     */
    volatile double stableIntervalMicros;

    private final Object mutex = new Object();

    /**
     * The time when the next request (no matter its size) will be granted. After granting a request,
     * this is pushed further in the future. Large requests push this further than small requests.
     */
    private long nextFreeTicketMicros = 0L; // could be either in the past or future

    private GoogleCloudLimiter(GoogleCloudLimiter.SleepingCloudTicker CloudTicker) {
        this.CloudTicker = CloudTicker;
        this.offsetNanos = CloudTicker.read();
    }

    /**
     * Updates the stable rate of this {@code GoogleCloudLimiter}, that is, the
     * {@code permitsPerSecond} argument provided in the factory method that
     * constructed the {@code GoogleCloudLimiter}. Currently throttled threads will <b>not</b>
     * be awakened as a result of this invocation, thus they do not observe the new rate;
     * only subsequent requests will.
     * <p>
     * <p>Note though that, since each request repays (by waiting, if necessary) the cost
     * of the <i>previous</i> request, this means that the very next request
     * after an invocation to {@code setRate} will not be affected by the new rate;
     * it will pay the cost of the previous request, which is in terms of the previous rate.
     * <p>
     * <p>The behavior of the {@code GoogleCloudLimiter} is not modified in any other way,
     * e.g. if the {@code GoogleCloudLimiter} was configured with a warmup period of 20 seconds,
     * it still has a warmup period of 20 seconds after this method invocation.
     *
     * @param permitsPerSecond the new stable rate of this {@code GoogleCloudLimiter}. Must be positive
     */
    public final void setRate(double permitsPerSecond) {
        CloudPreconditions.checkArgument(permitsPerSecond > 0.0
                && !Double.isNaN(permitsPerSecond), "rate must be positive");
        synchronized (mutex) {
            resync(readSafeMicros());
            double stableIntervalMicros = TimeUnit.SECONDS.toMicros(1L) / permitsPerSecond;
            this.stableIntervalMicros = stableIntervalMicros;
            doSetRate(permitsPerSecond, stableIntervalMicros);
        }
    }

    abstract void doSetRate(double permitsPerSecond, double stableIntervalMicros);

    /**
     * Returns the stable rate (as {@code permits per seconds}) with which this
     * {@code GoogleCloudLimiter} is configured with. The initial value of this is the same as
     * the {@code permitsPerSecond} argument passed in the factory method that produced
     * this {@code GoogleCloudLimiter}, and it is only updated after invocations
     * to {@linkplain #setRate}.
     */
    public final double getRate() {
        return TimeUnit.SECONDS.toMicros(1L) / stableIntervalMicros;
    }

    /**
     * Acquires a single permit from this {@code GoogleCloudLimiter}, blocking until the
     * request can be granted. Tells the amount of time slept, if any.
     * <p>
     * <p>This method is equivalent to {@code acquire(1)}.
     *
     * @return time spent sleeping to enforce rate, in seconds; 0.0 if not rate-limited
     * @since 16.0 (present in 13.0 with {@code void} return type})
     */
    public double acquire() {
        return acquire(1);
    }

    /**
     * Acquires the given number of permits from this {@code GoogleCloudLimiter}, blocking until the
     * request can be granted. Tells the amount of time slept, if any.
     *
     * @param permits the number of permits to acquire
     * @return time spent sleeping to enforce rate, in seconds; 0.0 if not rate-limited
     * @since 16.0 (present in 13.0 with {@code void} return type})
     */
    public double acquire(int permits) {
        long microsToWait = reserve(permits);
        CloudTicker.sleepMicrosUninterruptibly(microsToWait);
        return 1.0 * microsToWait / TimeUnit.SECONDS.toMicros(1L);
    }

    /**
     * Reserves a single permit from this {@code GoogleCloudLimiter} for future use, returning the number of
     * microseconds until the reservation.
     * <p>
     * <p>This method is equivalent to {@code reserve(1)}.
     *
     * @return time in microseconds to wait until the resource can be acquired.
     */
    long reserve() {
        return reserve(1);
    }

    /**
     * Reserves the given number of permits from this {@code GoogleCloudLimiter} for future use, returning
     * the number of microseconds until the reservation can be consumed.
     *
     * @return time in microseconds to wait until the resource can be acquired.
     */
    long reserve(int permits) {
        checkPermits(permits);
        synchronized (mutex) {
            return reserveNextTicket(permits, readSafeMicros());
        }
    }

    /**
     * Acquires a permit from this {@code GoogleCloudLimiter} if it can be obtained
     * without exceeding the specified {@code timeout}, or returns {@code false}
     * immediately (without waiting) if the permit would not have been granted
     * before the timeout expired.
     * <p>
     * <p>This method is equivalent to {@code tryAcquire(1, timeout, unit)}.
     *
     * @param timeout the maximum time to wait for the permit
     * @param unit    the time unit of the timeout argument
     * @return {@code true} if the permit was acquired, {@code false} otherwise
     */
    public boolean tryAcquire(long timeout, TimeUnit unit) {
        return tryAcquire(1, timeout, unit);
    }

    /**
     * Acquires permits from this {@link GoogleCloudLimiter} if it can be acquired immediately without delay.
     * <p>
     * <p>
     * This method is equivalent to {@code tryAcquire(permits, 0, anyUnit)}.
     *
     * @param permits the number of permits to acquire
     * @return {@code true} if the permits were acquired, {@code false} otherwise
     * @since 14.0
     */
    public boolean tryAcquire(int permits) {
        return tryAcquire(permits, 0, TimeUnit.MICROSECONDS);
    }

    /**
     * Acquires a permit from this {@link GoogleCloudLimiter} if it can be acquired immediately without
     * delay.
     * <p>
     * <p>
     * This method is equivalent to {@code tryAcquire(1)}.
     *
     * @return {@code true} if the permit was acquired, {@code false} otherwise
     * @since 14.0
     */
    public boolean tryAcquire() {
        return tryAcquire(1, 0, TimeUnit.MICROSECONDS);
    }

    /**
     * Acquires the given number of permits from this {@code GoogleCloudLimiter} if it can be obtained
     * without exceeding the specified {@code timeout}, or returns {@code false}
     * immediately (without waiting) if the permits would not have been granted
     * before the timeout expired.
     *
     * @param permits the number of permits to acquire
     * @param timeout the maximum time to wait for the permits
     * @param unit    the time unit of the timeout argument
     * @return {@code true} if the permits were acquired, {@code false} otherwise
     */
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
        long timeoutMicros = unit.toMicros(timeout);
        checkPermits(permits);
        long microsToWait;
        synchronized (mutex) {
            long nowMicros = readSafeMicros();
            if (nextFreeTicketMicros > nowMicros + timeoutMicros) {
                return false;
            } else {
                microsToWait = reserveNextTicket(permits, nowMicros);
            }
        }
        CloudTicker.sleepMicrosUninterruptibly(microsToWait);
        return true;
    }

    private static void checkPermits(int permits) {
        CloudPreconditions.checkArgument(permits > 0, "Requested permits must be positive");
    }

    /**
     * Reserves next ticket and returns the wait time that the caller must wait for.
     * <p>
     * <p>The return value is guaranteed to be non-negative.
     */
    private long reserveNextTicket(double requiredPermits, long nowMicros) {
        resync(nowMicros);
        long microsToNextFreeTicket = Math.max(0, nextFreeTicketMicros - nowMicros);
        double storedPermitsToSpend = Math.min(requiredPermits, this.storedPermits);
        double freshPermits = requiredPermits - storedPermitsToSpend;

        long waitMicros = storedPermitsToWaitTime(this.storedPermits, storedPermitsToSpend)
                + (long) (freshPermits * stableIntervalMicros);

        this.nextFreeTicketMicros = nextFreeTicketMicros + waitMicros;
        this.storedPermits -= storedPermitsToSpend;
        return microsToNextFreeTicket;
    }

    /**
     * Translates a specified portion of our currently stored permits which we want to
     * spend/acquire, into a throttling time. Conceptually, this evaluates the integral
     * of the underlying function we use, for the range of
     * [(storedPermits - permitsToTake), storedPermits].
     * <p>
     * This always holds: {@code 0 <= permitsToTake <= storedPermits}
     */
    abstract long storedPermitsToWaitTime(double storedPermits, double permitsToTake);

    private void resync(long nowMicros) {
        // if nextFreeTicket is in the past, resync to now
        if (nowMicros > nextFreeTicketMicros) {
            storedPermits = Math.min(maxPermits,
                    storedPermits + (nowMicros - nextFreeTicketMicros) / stableIntervalMicros);
            nextFreeTicketMicros = nowMicros;
        }
    }

    private long readSafeMicros() {
        return TimeUnit.NANOSECONDS.toMicros(CloudTicker.read() - offsetNanos);
    }

    @Override
    public String toString() {
        return String.format("GoogleCloudLimiter[stableRate=%3.1fqps]", 1000000.0 / stableIntervalMicros);
    }

    /**
     * This implements the following function:
     * <p>
     * ^ throttling
     * |
     * 3*stable +                  /
     * interval |                 /.
     * (cold)  |                / .
     * |               /  .   <-- "warmup period" is the area of the trapezoid between
     * 2*stable +              /   .       halfPermits and maxPermits
     * interval |             /    .
     * |            /     .
     * |           /      .
     * stable +----------/  WARM . }
     * interval |          .   UP  . } <-- this rectangle (from 0 to maxPermits, and
     * |          . PERIOD. }     height == stableInterval) defines the cooldown period,
     * |          .       . }     and we want cooldownPeriod == warmupPeriod
     * |---------------------------------> storedPermits
     * (halfPermits) (maxPermits)
     * <p>
     * Before going into the details of this particular function, let's keep in mind the basics:
     * 1) The state of the GoogleCloudLimiter (storedPermits) is a vertical line in this figure.
     * 2) When the GoogleCloudLimiter is not used, this goes right (up to maxPermits)
     * 3) When the GoogleCloudLimiter is used, this goes left (down to zero), since if we have storedPermits,
     * we serve from those first
     * 4) When _unused_, we go right at the same speed (rate)! I.e., if our rate is
     * 2 permits per second, and 3 unused seconds pass, we will always save 6 permits
     * (no matter what our initial position was), up to maxPermits.
     * If we invert the rate, we get the "stableInterval" (interval between two requests
     * in a perfectly spaced out sequence of requests of the given rate). Thus, if you
     * want to see "how much time it will take to go from X storedPermits to X+K storedPermits?",
     * the answer is always stableInterval * K. In the same example, for 2 permits per second,
     * stableInterval is 500ms. Thus to go from X storedPermits to X+6 storedPermits, we
     * require 6 * 500ms = 3 seconds.
     * <p>
     * In short, the time it takes to move to the right (save K permits) is equal to the
     * rectangle of width == K and height == stableInterval.
     * 4) When _used_, the time it takes, as explained in the introductory class note, is
     * equal to the integral of our function, between X permits and X-K permits, assuming
     * we want to spend K saved permits.
     * <p>
     * In summary, the time it takes to move to the left (spend K permits), is equal to the
     * area of the function of width == K.
     * <p>
     * Let's dive into this function now:
     * <p>
     * When we have storedPermits <= halfPermits (the left portion of the function), then
     * we spend them at the exact same rate that
     * fresh permits would be generated anyway (that rate is 1/stableInterval). We size
     * this area to be equal to _half_ the specified warmup period. Why we need this?
     * And why half? We'll explain shortly below (after explaining the second part).
     * <p>
     * Stored permits that are beyond halfPermits, are mapped to an ascending line, that goes
     * from stableInterval to 3 * stableInterval. The average height for that part is
     * 2 * stableInterval, and is sized appropriately to have an area _equal_ to the
     * specified warmup period. Thus, by point (4) above, it takes "warmupPeriod" amount of time
     * to go from maxPermits to halfPermits.
     * <p>
     * BUT, by point (3) above, it only takes "warmupPeriod / 2" amount of time to return back
     * to maxPermits, from halfPermits! (Because the trapezoid has double the area of the rectangle
     * of height stableInterval and equivalent width). We decided that the "cooldown period"
     * time should be equivalent to "warmup period", thus a fully saturated GoogleCloudLimiter
     * (with zero stored permits, serving only fresh ones) can go to a fully unsaturated
     * (with storedPermits == maxPermits) in the same amount of time it takes for a fully
     * unsaturated GoogleCloudLimiter to return to the stableInterval -- which happens in halfPermits,
     * since beyond that point, we use a horizontal line of "stableInterval" height, simulating
     * the regular rate.
     * <p>
     * Thus, we have figured all dimensions of this shape, to give all the desired
     * properties:
     * - the width is warmupPeriod / stableInterval, to make cooldownPeriod == warmupPeriod
     * - the slope starts at the middle, and goes from stableInterval to 3*stableInterval so
     * to have halfPermits being spend in double the usual time (half the rate), while their
     * respective rate is steadily ramping up
     */
    private static class WarmingUp extends GoogleCloudLimiter {

        final long warmupPeriodMicros;
        /**
         * The slope of the line from the stable interval (when permits == 0), to the cold interval
         * (when permits == maxPermits)
         */
        private double slope;
        private double halfPermits;

        WarmingUp(GoogleCloudLimiter.SleepingCloudTicker CloudTicker, long warmupPeriod, TimeUnit timeUnit) {
            super(CloudTicker);
            this.warmupPeriodMicros = timeUnit.toMicros(warmupPeriod);
        }

        @Override
        void doSetRate(double permitsPerSecond, double stableIntervalMicros) {
            double oldMaxPermits = maxPermits;
            maxPermits = warmupPeriodMicros / stableIntervalMicros;
            halfPermits = maxPermits / 2.0;
            // Stable interval is x, cold is 3x, so on average it's 2x. Double the time -> halve the rate
            double coldIntervalMicros = stableIntervalMicros * 3.0;
            slope = (coldIntervalMicros - stableIntervalMicros) / halfPermits;
            if (oldMaxPermits == Double.POSITIVE_INFINITY) {
                // if we don't special-case this, we would get storedPermits == NaN, below
                storedPermits = 0.0;
            } else {
                storedPermits = (oldMaxPermits == 0.0)
                        ? maxPermits // initial state is cold
                        : storedPermits * maxPermits / oldMaxPermits;
            }
        }

        @Override
        long storedPermitsToWaitTime(double storedPermits, double permitsToTake) {
            double availablePermitsAboveHalf = storedPermits - halfPermits;
            long micros = 0;
            // measuring the integral on the right part of the function (the climbing line)
            if (availablePermitsAboveHalf > 0.0) {
                double permitsAboveHalfToTake = Math.min(availablePermitsAboveHalf, permitsToTake);
                micros = (long) (permitsAboveHalfToTake * (permitsToTime(availablePermitsAboveHalf)
                        + permitsToTime(availablePermitsAboveHalf - permitsAboveHalfToTake)) / 2.0);
                permitsToTake -= permitsAboveHalfToTake;
            }
            // measuring the integral on the left part of the function (the horizontal line)
            micros += (stableIntervalMicros * permitsToTake);
            return micros;
        }

        private double permitsToTime(double permits) {
            return stableIntervalMicros + permits * slope;
        }
    }

    /**
     * This implements a "bursty" GoogleCloudLimiter, where storedPermits are translated to
     * zero throttling. The maximum number of permits that can be saved (when the GoogleCloudLimiter is
     * unused) is defined in terms of time, in this sense: if a GoogleCloudLimiter is 2qps, and this
     * time is specified as 10 seconds, we can save up to 2 * 10 = 20 permits.
     */
    private static class Bursty extends GoogleCloudLimiter {
        /**
         * The work (permits) of how many seconds can be saved up if this GoogleCloudLimiter is unused?
         */
        final double maxBurstSeconds;

        Bursty(GoogleCloudLimiter.SleepingCloudTicker CloudTicker, double maxBurstSeconds) {
            super(CloudTicker);
            this.maxBurstSeconds = maxBurstSeconds;
        }

        @Override
        void doSetRate(double permitsPerSecond, double stableIntervalMicros) {
            double oldMaxPermits = this.maxPermits;
            maxPermits = maxBurstSeconds * permitsPerSecond;
            storedPermits = (oldMaxPermits == 0.0)
                    ? 0.0 // initial state
                    : storedPermits * maxPermits / oldMaxPermits;
        }

        @Override
        long storedPermitsToWaitTime(double storedPermits, double permitsToTake) {
            return 0L;
        }
    }

    @CloudVisibleForTesting
    static abstract class SleepingCloudTicker extends CloudTicker {
        abstract void sleepMicrosUninterruptibly(long micros);

        static final GoogleCloudLimiter.SleepingCloudTicker SYSTEM_CloudTicker = new GoogleCloudLimiter.SleepingCloudTicker() {
            @Override
            public long read() {
                return systemTicker().read();
            }

            @Override
            public void sleepMicrosUninterruptibly(long micros) {
                if (micros > 0) {
                    CloudUninterruptibles.sleepUninterruptibly(micros, TimeUnit.MICROSECONDS);
                }
            }
        };
    }
}
