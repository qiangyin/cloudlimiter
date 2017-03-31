package com.cloudzone.cloudlimiter.base;

import java.lang.annotation.*;

/**
 * @author tantexian
 * @since 2017/3/31
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
@Documented
@CloudGwtCompatible
public @interface CloudGwtIncompatible {
    String value();
}