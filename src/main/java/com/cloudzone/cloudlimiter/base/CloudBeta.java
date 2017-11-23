package com.cloudzone.cloudlimiter.base;

import java.lang.annotation.*;

/**
 * @author tantexian
 * @since 2017/3/31
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
@Documented
@CloudGwtCompatible
public @interface CloudBeta {
}