package com.cloudzone.cloudlimiter.base;

import java.util.Collections;
import java.util.Set;

/**
 * @author tantexian
 * @since 2017/3/31
 */
@CloudGwtCompatible
final class CloudPresent<T> extends CloudOptional<T> {
    private final T reference;
    private static final long serialVersionUID = 0L;

    CloudPresent(T reference) {
        this.reference = reference;
    }

    public boolean isPresent() {
        return true;
    }

    public T get() {
        return this.reference;
    }

    public T or(T defaultValue) {
        CloudPreconditions.checkNotNull(defaultValue, "use Optional.orNull() instead of Optional.or(null)");
        return this.reference;
    }

    public CloudOptional<T> or(CloudOptional<? extends T> secondChoice) {
        CloudPreconditions.checkNotNull(secondChoice);
        return this;
    }

    public T or(CloudSupplier<? extends T> supplier) {
        CloudPreconditions.checkNotNull(supplier);
        return this.reference;
    }

    public T orNull() {
        return this.reference;
    }

    public Set<T> asSet() {
        return Collections.singleton(this.reference);
    }

    public <V> CloudOptional<V> transform(CloudFunction<? super T, V> function) {
        return new CloudPresent(CloudPreconditions.checkNotNull(function.apply(this.reference), "the Function passed to Optional.transform() must not return null."));
    }

    public boolean equals( Object object) {
        if (object instanceof CloudPresent) {
            CloudPresent other = (CloudPresent) object;
            return this.reference.equals(other.reference);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return 1502476572 + this.reference.hashCode();
    }

    public String toString() {
        return "Optional.of(" + this.reference + ")";
    }
}
