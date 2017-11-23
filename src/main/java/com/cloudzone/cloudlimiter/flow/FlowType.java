package com.cloudzone.cloudlimiter.flow;

/**
 * @author tantexian<my.oschina.net/tantexian>
 * @since 2017/3/30
 */
public enum FlowType {
    Byte(1),
    KB(1024),
    MB(1024 * 1024),
    GB(1024 * 1024 * 1024),
    TB(1024 * 1024 * 1024);

    long value;

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    private FlowType(long value) {
        this.value = value;
    }

}
