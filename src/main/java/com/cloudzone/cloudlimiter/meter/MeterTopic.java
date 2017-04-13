package com.cloudzone.cloudlimiter.meter;

/**
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/12
 */
public class MeterTopic {
    // 用于区分统计不同tag对应的请求值（其中tag和type共同决定唯一的MeterTopic）
    private String tag;
    // 用于区分相同tag的不同的type
    private String type;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof MeterTopic) {
            MeterTopic other = (MeterTopic) obj;
            if (tag == other.tag && type == other.type) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((tag == null) ? 0 : tag.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "MeterTopic{" +
                "tag='" + tag + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
