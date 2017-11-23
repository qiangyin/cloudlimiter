package com.cloudzone.cloudlimiter.base;

/**
 * @author tantexian
 * @since 2017/3/31
 */
@CloudGwtCompatible
public interface CloudSupplier<T> {
    T get();
}