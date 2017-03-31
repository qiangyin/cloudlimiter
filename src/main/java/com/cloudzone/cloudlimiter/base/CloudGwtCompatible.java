package com.cloudzone.cloudlimiter.base;

import java.lang.annotation.*;

/**
 * @author tantexian
 * @since 2017/3/31
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@CloudGwtCompatible
public @interface CloudGwtCompatible {
    boolean serializable() default false;

    boolean emulated() default false;
}
