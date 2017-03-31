package com.cloudzone.cloudlimiter.base;

/**
 * @author tantexian
 * @since 2017/3/31
 */
@CloudGwtCompatible
public interface CloudFunction<F, T> {

    T apply(F var1);

    boolean equals(Object var1);
}