package com.cloudzone.cloudlimiter.meter;

/**
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/12
 */
public class Topic {
    // 用于区分统计不同tag对应的请求值
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
        if (obj instanceof Topic) {
            Topic topic1 = (Topic) obj;
            if (this.tag == null) {
                Exception exception = new RuntimeException("topic == null !!!");
                exception.printStackTrace();
            }
            if (this.tag.equals(topic1.tag)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.tag == null) {
            Exception exception = new RuntimeException("topic == null !!!");
            exception.printStackTrace();
        }
        return tag.hashCode();
    }

    @Override
    public String toString() {
        return "Topic{" +
                "tag='" + tag + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
