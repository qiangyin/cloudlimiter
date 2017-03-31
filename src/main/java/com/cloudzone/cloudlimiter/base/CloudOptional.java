package com.cloudzone.cloudlimiter.base;


import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;



/**
 * @author tantexian
 * @since 2017/3/31
 */
@CloudGwtCompatible(
        serializable = true
)
public abstract class CloudOptional<T> implements Serializable {
    private static final long serialVersionUID = 0L;

    public static <T> CloudOptional<T> absent() {
        return CloudAbsent.withType();
    }

    public static <T> CloudOptional<T> of(T reference) {
        return new CloudPresent(CloudPreconditions.checkNotNull(reference));
    }

    public static <T> CloudOptional<T> fromNullable( T nullableReference) {
        return (CloudOptional) (nullableReference == null ? absent() : new CloudPresent(nullableReference));
    }

    CloudOptional() {
    }

    public abstract boolean isPresent();

    public abstract T get();

    public abstract T or(T var1);

    public abstract CloudOptional<T> or(CloudOptional<? extends T> var1);

    @CloudBeta
    public abstract T or(CloudSupplier<? extends T> var1);

    
    public abstract T orNull();

    public abstract Set<T> asSet();

    public abstract <V> CloudOptional<V> transform(CloudFunction<? super T, V> var1);

    public abstract boolean equals( Object var1);

    public abstract int hashCode();

    public abstract String toString();

    @CloudBeta
    public static <T> Iterable<T> presentInstances(final Iterable<? extends CloudOptional<? extends T>> optionals) {
        CloudPreconditions.checkNotNull(optionals);
        return new Iterable() {
            public Iterator<T> iterator() {
                return new CloudAbstractIterator() {
                    private final Iterator<? extends CloudOptional<? extends T>> iterator = (Iterator) CloudPreconditions.checkNotNull(optionals.iterator());

                    protected T computeNext() {
                        while (true) {
                            if (this.iterator.hasNext()) {
                                CloudOptional optional = (CloudOptional) this.iterator.next();
                                if (!optional.isPresent()) {
                                    continue;
                                }

                                return (T) optional.get();
                            }

                            return (T) this.endOfData();
                        }
                    }
                };
            }
        };
    }
}
