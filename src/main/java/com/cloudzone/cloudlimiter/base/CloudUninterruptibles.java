package com.cloudzone.cloudlimiter.base;

import java.util.concurrent.*;

/**
 * @author tantexian
 * @since 2017/3/31
 */
@CloudBeta
public final class CloudUninterruptibles {
    public static void awaitUninterruptibly(CountDownLatch latch) {
        boolean interrupted = false;

        try {
            while (true) {
                try {
                    latch.await();
                    return;
                } catch (InterruptedException var6) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }

        }
    }

    public static boolean awaitUninterruptibly(CountDownLatch latch, long timeout, TimeUnit unit) {
        boolean interrupted = false;

        try {
            long remainingNanos = unit.toNanos(timeout);
            long end = System.nanoTime() + remainingNanos;

            while (true) {
                try {
                    boolean e = latch.await(remainingNanos, TimeUnit.NANOSECONDS);
                    return e;
                } catch (InterruptedException var13) {
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

    public static void joinUninterruptibly(Thread toJoin) {
        boolean interrupted = false;

        try {
            while (true) {
                try {
                    toJoin.join();
                    return;
                } catch (InterruptedException var6) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }

        }
    }

    public static <V> V getUninterruptibly(Future<V> future) throws ExecutionException {
        boolean interrupted = false;

        try {
            while (true) {
                try {
                    V e = future.get();
                    return e;
                } catch (InterruptedException var6) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }

        }
    }

    public static <V> V getUninterruptibly(Future<V> future, long timeout, TimeUnit unit) throws ExecutionException, TimeoutException {
        boolean interrupted = false;

        try {
            long remainingNanos = unit.toNanos(timeout);
            long end = System.nanoTime() + remainingNanos;

            while (true) {
                try {
                    V e = future.get(remainingNanos, TimeUnit.NANOSECONDS);
                    return e;
                } catch (InterruptedException var13) {
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

    public static void joinUninterruptibly(Thread toJoin, long timeout, TimeUnit unit) {
        CloudPreconditions.checkNotNull(toJoin);
        boolean interrupted = false;

        try {
            long remainingNanos = unit.toNanos(timeout);
            long end = System.nanoTime() + remainingNanos;

            while (true) {
                try {
                    TimeUnit.NANOSECONDS.timedJoin(toJoin, remainingNanos);
                    return;
                } catch (InterruptedException var13) {
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

    public static <E> E takeUninterruptibly(BlockingQueue<E> queue) {
        boolean interrupted = false;

        try {
            while (true) {
                try {
                    E e = queue.take();
                    return e;
                } catch (InterruptedException var6) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }

        }
    }

    public static <E> void putUninterruptibly(BlockingQueue<E> queue, E element) {
        boolean interrupted = false;

        try {
            while (true) {
                try {
                    queue.put(element);
                    return;
                } catch (InterruptedException var7) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }

        }
    }

    public static void sleepUninterruptibly(long sleepFor, TimeUnit unit) {
        boolean interrupted = false;

        try {
            long remainingNanos = unit.toNanos(sleepFor);
            long end = System.nanoTime() + remainingNanos;

            while (true) {
                try {
                    TimeUnit.NANOSECONDS.sleep(remainingNanos);
                    return;
                } catch (InterruptedException var12) {
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

    private CloudUninterruptibles() {
    }
}

