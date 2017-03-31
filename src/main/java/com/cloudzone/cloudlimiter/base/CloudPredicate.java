package com.cloudzone.cloudlimiter.base;

/**
 * @author tantexian
 * @since 2017/3/31
 */
@CloudGwtCompatible
public interface CloudPredicate<T> {
    boolean apply( T var1);

    boolean equals( Object var1);
}