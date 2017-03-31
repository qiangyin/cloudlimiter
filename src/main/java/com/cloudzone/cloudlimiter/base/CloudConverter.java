package com.cloudzone.cloudlimiter.base;

import java.io.Serializable;
import java.util.Iterator;

/**
 * @author tantexian
 * @since 2017/3/31
 */
@CloudBeta
@CloudGwtCompatible
public abstract class CloudConverter<A, B> implements CloudFunction<A, B> {
    private final boolean handleNullAutomatically;
    private transient CloudConverter<B, A> reverse;

    protected CloudConverter() {
        this(true);
    }

    CloudConverter(boolean handleNullAutomatically) {
        this.handleNullAutomatically = handleNullAutomatically;
    }

    protected abstract B doForward(A var1);

    protected abstract A doBackward(B var1);


    public final B convert(A a) {
        return this.correctedDoForward(a);
    }


    B correctedDoForward(A a) {
        return this.handleNullAutomatically ? (a == null ? null : CloudPreconditions.checkNotNull(this.doForward(a))) : this.doForward(a);
    }


    A correctedDoBackward(B b) {
        return this.handleNullAutomatically ? (b == null ? null : CloudPreconditions.checkNotNull(this.doBackward(b))) : this.doBackward(b);
    }

    public Iterable<B> convertAll(final Iterable<? extends A> fromIterable) {
        CloudPreconditions.checkNotNull(fromIterable, "fromIterable");
        return new Iterable() {
            public Iterator<B> iterator() {
                return new Iterator() {
                    private final Iterator<? extends A> fromIterator = fromIterable.iterator();

                    public boolean hasNext() {
                        return this.fromIterator.hasNext();
                    }

                    public B next() {
                        return CloudConverter.this.convert(this.fromIterator.next());
                    }

                    public void remove() {
                        this.fromIterator.remove();
                    }
                };
            }
        };
    }

    public CloudConverter<B, A> reverse() {
        CloudConverter result = this.reverse;
        return result == null ? (this.reverse = new ReverseCloudConverter(this)) : result;
    }

    public <C> CloudConverter<A, C> andThen(CloudConverter<B, C> secondCloudConverter) {
        return new CloudConverterComposition(this, (CloudConverter) CloudPreconditions.checkNotNull(secondCloudConverter));
    }

    /**
     * @deprecated
     */
    @Deprecated

    public final B apply(A a) {
        return this.convert(a);
    }

    public boolean equals(Object object) {
        return super.equals(object);
    }

    public static <A, B> CloudConverter<A, B> from(CloudFunction<? super A, ? extends B> forwardCloudFunction, CloudFunction<? super B, ? extends A> backwardCloudFunction) {
        return new CloudFunctionBasedCloudConverter(forwardCloudFunction, backwardCloudFunction);
    }

    public static <T> CloudConverter<T, T> identity() {
        return IdentityCloudConverter.INSTANCE;
    }

    private static final class IdentityCloudConverter<T> extends CloudConverter<T, T> implements Serializable {
        static final IdentityCloudConverter INSTANCE = new IdentityCloudConverter();
        private static final long serialVersionUID = 0L;

        private IdentityCloudConverter() {
        }

        protected T doForward(T t) {
            return t;
        }

        protected T doBackward(T t) {
            return t;
        }

        public IdentityCloudConverter<T> reverse() {
            return this;
        }

        public <S> CloudConverter<T, S> andThen(CloudConverter<T, S> otherCloudConverter) {
            return (CloudConverter) CloudPreconditions.checkNotNull(otherCloudConverter, "otherCloudConverter");
        }

        public String toString() {
            return "CloudConverter.identity()";
        }

        private Object readResolve() {
            return INSTANCE;
        }
    }

    private static final class CloudFunctionBasedCloudConverter<A, B> extends CloudConverter<A, B> implements Serializable {
        private final CloudFunction<? super A, ? extends B> forwardCloudFunction;
        private final CloudFunction<? super B, ? extends A> backwardCloudFunction;

        private CloudFunctionBasedCloudConverter(CloudFunction<? super A, ? extends B> forwardCloudFunction, CloudFunction<? super B, ? extends A> backwardCloudFunction) {
            this.forwardCloudFunction = (CloudFunction) CloudPreconditions.checkNotNull(forwardCloudFunction);
            this.backwardCloudFunction = (CloudFunction) CloudPreconditions.checkNotNull(backwardCloudFunction);
        }

        protected B doForward(A a) {
            return this.forwardCloudFunction.apply(a);
        }

        protected A doBackward(B b) {
            return this.backwardCloudFunction.apply(b);
        }

        public boolean equals(Object object) {
            if (!(object instanceof CloudFunctionBasedCloudConverter)) {
                return false;
            } else {
                CloudFunctionBasedCloudConverter that = (CloudFunctionBasedCloudConverter) object;
                return this.forwardCloudFunction.equals(that.forwardCloudFunction) && this.backwardCloudFunction.equals(that.backwardCloudFunction);
            }
        }

        public int hashCode() {
            return this.forwardCloudFunction.hashCode() * 31 + this.backwardCloudFunction.hashCode();
        }

        public String toString() {
            return "CloudConverter.from(" + this.forwardCloudFunction + ", " + this.backwardCloudFunction + ")";
        }
    }

    private static final class CloudConverterComposition<A, B, C> extends CloudConverter<A, C> implements Serializable {
        final CloudConverter<A, B> first;
        final CloudConverter<B, C> second;
        private static final long serialVersionUID = 0L;

        CloudConverterComposition(CloudConverter<A, B> first, CloudConverter<B, C> second) {
            this.first = first;
            this.second = second;
        }

        protected C doForward(A a) {
            throw new AssertionError();
        }

        protected A doBackward(C c) {
            throw new AssertionError();
        }


        C correctedDoForward(A a) {
            return this.second.correctedDoForward(this.first.correctedDoForward(a));
        }


        A correctedDoBackward(C c) {
            return this.first.correctedDoBackward(this.second.correctedDoBackward(c));
        }

        public boolean equals(Object object) {
            if (!(object instanceof CloudConverterComposition)) {
                return false;
            } else {
                CloudConverterComposition that = (CloudConverterComposition) object;
                return this.first.equals(that.first) && this.second.equals(that.second);
            }
        }

        public int hashCode() {
            return 31 * this.first.hashCode() + this.second.hashCode();
        }

        public String toString() {
            return this.first + ".andThen(" + this.second + ")";
        }
    }

    private static final class ReverseCloudConverter<A, B> extends CloudConverter<B, A> implements Serializable {
        final CloudConverter<A, B> original;
        private static final long serialVersionUID = 0L;

        ReverseCloudConverter(CloudConverter<A, B> original) {
            this.original = original;
        }

        protected A doForward(B b) {
            throw new AssertionError();
        }

        protected B doBackward(A a) {
            throw new AssertionError();
        }


        A correctedDoForward(B b) {
            return this.original.correctedDoBackward(b);
        }


        B correctedDoBackward(A a) {
            return this.original.correctedDoForward(a);
        }

        public CloudConverter<A, B> reverse() {
            return this.original;
        }

        public boolean equals(Object object) {
            if (object instanceof ReverseCloudConverter) {
                ReverseCloudConverter that = (ReverseCloudConverter) object;
                return this.original.equals(that.original);
            } else {
                return false;
            }
        }

        public int hashCode() {
            return ~this.original.hashCode();
        }

        public String toString() {
            return this.original + ".reverse()";
        }
    }
}
