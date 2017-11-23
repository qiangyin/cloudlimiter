package com.cloudzone.cloudlimiter.base;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.*;

/**
 * @author tantexian
 * @since 2017/3/31
 */
@CloudGwtCompatible(
        emulated = true
)
@CloudBeta
public final class CloudEnums {
    @CloudGwtIncompatible("java.lang.ref.WeakReference")
    private static final Map<Class<? extends Enum<?>>, Map<String, WeakReference<? extends Enum<?>>>> enumConstantCache = new WeakHashMap();

    private CloudEnums() {
    }

    @CloudGwtIncompatible("reflection")
    public static Field getField(Enum<?> enumValue) {
        Class clazz = enumValue.getDeclaringClass();

        try {
            return clazz.getDeclaredField(enumValue.name());
        } catch (NoSuchFieldException var3) {
            throw new AssertionError(var3);
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public static <T extends Enum<T>> CloudFunction<String, T> valueOfFunction(Class<T> enumClass) {
        return new CloudEnums.ValueOfFunction(enumClass);
    }

    public static <T extends Enum<T>> CloudOptional<T> getIfPresent(Class<T> enumClass, String value) {
        CloudPreconditions.checkNotNull(enumClass);
        CloudPreconditions.checkNotNull(value);
        return CloudPlatform.getEnumIfPresent(enumClass, value);
    }

    @CloudGwtIncompatible("java.lang.ref.WeakReference")
    private static <T extends Enum<T>> Map<String, WeakReference<? extends Enum<?>>> populateCache(Class<T> enumClass) {
        HashMap result = new HashMap();
        Iterator i = EnumSet.allOf(enumClass).iterator();

        while (i.hasNext()) {
            Enum enumInstance = (Enum) i.next();
            result.put(enumInstance.name(), new WeakReference(enumInstance));
        }

        enumConstantCache.put(enumClass, result);
        return result;
    }

    @CloudGwtIncompatible("java.lang.ref.WeakReference")
    static <T extends Enum<T>> Map<String, WeakReference<? extends Enum<?>>> getEnumConstants(Class<T> enumClass) {
        Map var1 = enumConstantCache;
        synchronized (enumConstantCache) {
            Map constants = (Map) enumConstantCache.get(enumClass);
            if (constants == null) {
                constants = populateCache(enumClass);
            }

            return constants;
        }
    }

    public static <T extends Enum<T>> CloudConverter<String, T> stringCloudConverter(Class<T> enumClass) {
        return new CloudEnums.StringCloudConverter(enumClass);
    }

    private static final class StringCloudConverter<T extends Enum<T>> extends CloudConverter<String, T> implements Serializable {
        private final Class<T> enumClass;
        private static final long serialVersionUID = 0L;

        StringCloudConverter(Class<T> enumClass) {
            this.enumClass = (Class) CloudPreconditions.checkNotNull(enumClass);
        }

        protected T doForward(String value) {
            return Enum.valueOf(this.enumClass, value);
        }

        protected String doBackward(T enumValue) {
            return enumValue.name();
        }

        public boolean equals( Object object) {
            if (object instanceof CloudEnums.StringCloudConverter) {
                CloudEnums.StringCloudConverter that = (CloudEnums.StringCloudConverter) object;
                return this.enumClass.equals(that.enumClass);
            } else {
                return false;
            }
        }

        public int hashCode() {
            return this.enumClass.hashCode();
        }

        public String toString() {
            return "CloudEnums.stringCloudConverter(" + this.enumClass.getName() + ".class)";
        }
    }

    private static final class ValueOfFunction<T extends Enum<T>> implements CloudFunction<String, T>, Serializable {
        private final Class<T> enumClass;
        private static final long serialVersionUID = 0L;

        private ValueOfFunction(Class<T> enumClass) {
            this.enumClass = (Class) CloudPreconditions.checkNotNull(enumClass);
        }

        public T apply(String value) {
            try {
                return Enum.valueOf(this.enumClass, value);
            } catch (IllegalArgumentException var3) {
                return null;
            }
        }

        public boolean equals( Object obj) {
            return obj instanceof CloudEnums.ValueOfFunction && this.enumClass.equals(((CloudEnums.ValueOfFunction) obj).enumClass);
        }

        public int hashCode() {
            return this.enumClass.hashCode();
        }

        public String toString() {
            return "CloudEnums.valueOf(" + this.enumClass + ")";
        }
    }
}
