package com.cloudzone.cloudlimiter.base;

import java.lang.ref.WeakReference;

@CloudGwtCompatible(
        emulated = true
)
final class CloudPlatform {
    private CloudPlatform() {
    }

    static long systemNanoTime() {
        return System.nanoTime();
    }

    static CloudCharMatcher precomputeCharMatcher(CloudCharMatcher matcher) {
        return matcher.precomputedInternal();
    }

    static <T extends Enum<T>> CloudOptional<T> getEnumIfPresent(Class<T> enumClass, String value) {
        WeakReference ref = (WeakReference) CloudEnums.getEnumConstants(enumClass).get(value);
        CloudOptional<? extends java.lang.Object> cloudOptional = ref == null ? CloudOptional.absent() : CloudOptional.of(enumClass.cast(ref.get()));
        return (CloudOptional<T>) cloudOptional;
    }
}