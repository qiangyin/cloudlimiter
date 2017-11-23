package com.cloudzone.cloudlimiter.base;


import java.util.Collections;
import java.util.Set;

/**
 * @author tantexian
 * @since 2017/3/31
 */
@CloudGwtCompatible
final class CloudAbsent<T> extends CloudOptional<T> {
    static final CloudAbsent<Object> INSTANCE = new CloudAbsent();
    private static final long serialVersionUID = 0L;

    static <T> CloudOptional<T> withType() {
        return (CloudOptional<T>) INSTANCE;
    }

    private CloudAbsent() {
    }

    public boolean isPresent() {
        return false;
    }

    public T get() {
        throw new IllegalStateException("CloudOptional.get() cannot be called on an absent value");
    }

    public T or(T defaultValue) {
        return CloudPreconditions.checkNotNull(defaultValue, "use CloudOptional.orNull() instead of CloudOptional.or(null)");
    }

    public CloudOptional<T> or(CloudOptional<? extends T> secondChoice) {
        return (CloudOptional) CloudPreconditions.checkNotNull(secondChoice);
    }

    public T or(CloudSupplier<? extends T> CloudSupplier) {
        return CloudPreconditions.checkNotNull(CloudSupplier.get(), "use CloudOptional.orNull() instead of a CloudSupplier that returns null");
    }


    public T orNull() {
        return null;
    }

    public Set<T> asSet() {
        return Collections.emptySet();
    }

    public <V> CloudAbsent<V> transform(CloudFunction<? super T, V> CloudFunction) {
        CloudPreconditions.checkNotNull(CloudFunction);
        return (CloudAbsent<V>) CloudAbsent.absent();
    }

    public boolean equals(Object object) {
        return object == this;
    }

    public int hashCode() {
        return 1502476572;
    }

    public String toString() {
        return "CloudOptional.absent()";
    }

    private Object readResolve() {
        return INSTANCE;
    }
}

