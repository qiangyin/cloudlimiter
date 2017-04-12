package com.cloudzone.cloudlimiter.meter;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author tantexian<my.oschina.net/tantexian>
 * @since 2017/4/4
 */
public class Meterinfo {
    private Date nowDate;
    // 用于表示统计的不同类型，例如：按照秒间隔统计，按照分钟间隔统计
    private TimeUnit timeUnitType;
    private long requestNum;

    // 用于区分不同统计主题
    private Topic topic;

    // Topic的tag字段
    private String tag;

    // Topic的type字段
    private String type;

    public String getTag() {
        return this.topic.getTag();
    }

    public String getType() {
        return this.topic.getType();
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public Date getNowDate() {
        return nowDate;
    }

    public void setNowDate(Date nowDate) {
        this.nowDate = nowDate;
    }

    public long getRequestNum() {
        return requestNum;
    }

    public void setRequestNum(long requestNum) {
        this.requestNum = requestNum;
    }

    public TimeUnit getTimeUnitType() {
        return timeUnitType;
    }

    public void setTimeUnitType(TimeUnit timeUnitType) {
        this.timeUnitType = timeUnitType;
    }

    @Override
    public String toString() {
        return "Meterinfo{" +
                "nowDate=" + nowDate +
                ", timeUnitType=" + timeUnitType +
                ", requestNum=" + requestNum +
                ", topic=" + topic +
                ", tag='" + this.getTag() + '\'' +
                ", type='" + this.getType() + '\'' +
                '}';
    }
}
