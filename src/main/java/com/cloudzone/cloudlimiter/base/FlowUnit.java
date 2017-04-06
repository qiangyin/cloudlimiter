package com.cloudzone.cloudlimiter.base;

/**
 * 流量单位：BYTE/KB/MB/GB/TB/PB
 *
 * @author tantexian<my.oschina.net/tantexian>
 * @since 2017/4/2
 */
public enum FlowUnit {
    /**
     * FlowUnit unit representing 1024  of a KB
     */
    BYTE {
        public long toByte(long d) {
            return d;
        }

        public long toKB(long d) {
            return d / (C1 / C0);
        }

        public long toMB(long d) {
            return d / (C2 / C0);
        }

        public long toGB(long d) {
            return d / (C3 / C0);
        }

        public long toTB(long d) {
            return d / (C4 / C0);
        }

        public long toPB(long d) {
            return d / (C5 / C0);
        }

        public long convert(long d, FlowUnit u) {
            return u.toByte(d);
        }
    },

    /**
     * FlowUnit unit representing 1024  of a KB
     */
    KB {
        public long toByte(long d) {
            return x(d, C1 / C0, MAX / (C1 / C0));
        }

        public long toKB(long d) {
            return d;
        }

        public long toMB(long d) {
            return d / (C2 / C1);
        }

        public long toGB(long d) {
            return d / (C3 / C1);
        }

        public long toTB(long d) {
            return d / (C4 / C1);
        }

        public long toPB(long d) {
            return d / (C5 / C1);
        }
    },

    /**
     * FlowUnit unit representing 1024  of a KB
     */
    MB {
        public long toByte(long d) {
            return x(d, C2 / C0, MAX / (C2 / C0));
        }

        public long toKB(long d) {
            return x(d, C2 / C1, MAX / (C2 / C1));
        }

        public long toMB(long d) {
            return d;
        }

        public long toGB(long d) {
            return d / (C3 / C2);
        }

        public long toTB(long d) {
            return d / (C4 / C2);
        }

        public long toPB(long d) {
            return d / (C5 / C2);
        }
    },

    /**
     * FlowUnit unit representing 1024  of a KB
     */
    GB {
        public long toByte(long d) {
            return x(d, C3 / C0, MAX / (C3 / C0));
        }

        public long toKB(long d) {
            return x(d, C3 / C1, MAX / (C3 / C1));
        }

        public long toMB(long d) {
            return x(d, C3 / C2, MAX / (C3 / C2));
        }

        public long toGB(long d) {
            return d;
        }

        public long toTB(long d) {
            return d / (C4 / C3);
        }

        public long toPB(long d) {
            return d / (C5 / C3);
        }
    },

    /**
     * FlowUnit unit representing 1024  of a KB
     */
    TB {
        public long toByte(long d) {
            return x(d, C4 / C0, MAX / (C4 / C0));
        }

        public long toKB(long d) {
            return x(d, C4 / C1, MAX / (C4 / C1));
        }

        public long toMB(long d) {
            return x(d, C4 / C2, MAX / (C4 / C2));
        }

        public long toGB(long d) {
            return x(d, C4 / C3, MAX / (C4 / C3));
        }

        public long toTB(long d) {
            return d;
        }

        public long toPB(long d) {
            return d / (C5 / C4);
        }
    },

    /**
     * FlowUnit unit representing 1024  of a KB
     */
    PB {
        public long toByte(long d) {
            return x(d, C5 / C0, MAX / (C5 / C0));
        }

        public long toKB(long d) {
            return x(d, C5 / C1, MAX / (C5 / C1));
        }

        public long toMB(long d) {
            return x(d, C5 / C2, MAX / (C5 / C2));
        }

        public long toGB(long d) {
            return x(d, C5 / C3, MAX / (C5 / C3));
        }

        public long toTB(long d) {
            return x(d, C5 / C4, MAX / (C5 / C4));
        }

        public long toPB(long d) {
            return d;
        }
    };

    // Handy constants for conversion methods
    private static final long C0 = 1L;
    private static final long C1 = C0 * 1024L;
    private static final long C2 = C1 * 1024L;
    private static final long C3 = C2 * 1024L;
    private static final long C4 = C3 * 1024L;
    private static final long C5 = C4 * 1024L;

    private static final long MAX = Long.MAX_VALUE;

    /**
     * Scale d by m, checking for overflow.
     * This has a short name to make above code more readable.
     */
    private static long x(long d, long m, long over) {
        if (d > over) return Long.MAX_VALUE;
        if (d < -over) return Long.MIN_VALUE;
        return d * m;
    }

    // To maintain full signature compatibility with 1.5, and to improve the
    // clarity of the generated javadoc (see 6287639: Abstract methods in
    // enum classes should not be listed as abstract), method convert
    // etc. are not declared abstract but otherwise act as abstract methods.

    /**
     * Converts the given time size in the given unit to this unit.
     * Conversions from finer to coarser granularities truncate, so
     * lose precision. For example, converting {@code 999} milliseconds
     * to seconds results in {@code 0}. Conversions from coarser to
     * finer granularities with arguments that would numerically
     * overflow saturate to {@code Long.MIN_VALUE} if negative or
     * {@code Long.MAX_VALUE} if positive.
     * <p>
     * <p>For example, to convert 10 minutes to milliseconds, use:
     * {@code FlowUnit.MILLISECONDS.convert(10L, FlowUnit.MINUTES)}
     *
     * @param sourceSize the time size in the given {@code sourceUnit}
     * @param sourceUnit the unit of the {@code sourceSize} argument
     * @return the converted size in this unit,
     * or {@code Long.MIN_VALUE} if conversion would negatively
     * overflow, or {@code Long.MAX_VALUE} if it would positively overflow.
     */
    public long convert(long sourceSize, FlowUnit sourceUnit) {
        throw new AbstractMethodError();
    }

    /**
     * Equivalent to
     * {@link #convert(long, FlowUnit) NANOSECONDS.convert(size, this)}.
     *
     * @param size the size
     * @return the converted size,
     * or {@code Long.MIN_VALUE} if conversion would negatively
     * overflow, or {@code Long.MAX_VALUE} if it would positively overflow.
     */
    public long toByte(long size) {
        throw new AbstractMethodError();
    }

    /**
     * Equivalent to
     * {@link #convert(long, FlowUnit) MICROSECONDS.convert(size, this)}.
     *
     * @param size the size
     * @return the converted size,
     * or {@code Long.MIN_VALUE} if conversion would negatively
     * overflow, or {@code Long.MAX_VALUE} if it would positively overflow.
     */
    public long toKB(long size) {
        throw new AbstractMethodError();
    }

    /**
     * Equivalent to
     * {@link #convert(long, FlowUnit) MILLISECONDS.convert(size, this)}.
     *
     * @param size the size
     * @return the converted size,
     * or {@code Long.MIN_VALUE} if conversion would negatively
     * overflow, or {@code Long.MAX_VALUE} if it would positively overflow.
     */
    public long toMB(long size) {
        throw new AbstractMethodError();
    }

    /**
     * Equivalent to
     * {@link #convert(long, FlowUnit) SECONDS.convert(size, this)}.
     *
     * @param size the size
     * @return the converted size,
     * or {@code Long.MIN_VALUE} if conversion would negatively
     * overflow, or {@code Long.MAX_VALUE} if it would positively overflow.
     */
    public long toSeconds(long size) {
        throw new AbstractMethodError();
    }

    /**
     * Equivalent to
     * {@link #convert(long, FlowUnit) MINUTES.convert(size, this)}.
     *
     * @param size the size
     * @return the converted size,
     * or {@code Long.MIN_VALUE} if conversion would negatively
     * overflow, or {@code Long.MAX_VALUE} if it would positively overflow.
     * @since 2017/4/2
     */
    public long toGB(long size) {
        throw new AbstractMethodError();
    }

    /**
     * Equivalent to
     * {@link #convert(long, FlowUnit) HOURS.convert(size, this)}.
     *
     * @param size the size
     * @return the converted size,
     * or {@code Long.MIN_VALUE} if conversion would negatively
     * overflow, or {@code Long.MAX_VALUE} if it would positively overflow.
     * @since 2017/4/2
     */
    public long toTB(long size) {
        throw new AbstractMethodError();
    }

    /**
     * Equivalent to
     * {@link #convert(long, FlowUnit) DAYS.convert(size, this)}.
     *
     * @param size the size
     * @return the converted size
     * @since 2017/4/2
     */
    public long toPB(long size) {
        throw new AbstractMethodError();
    }
}
