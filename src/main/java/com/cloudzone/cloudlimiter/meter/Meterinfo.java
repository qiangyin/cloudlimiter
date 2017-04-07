package com.cloudzone.cloudlimiter.meter;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author tantexian<my.oschina.net/tantexian>
 * @since 2017/4/4
 */
public class Meterinfo {
    private Date nowDate;
    private TimeUnit type;
    private long requestNum;

    // 用于区分统计不同tag对应的请求值
    private String tag;

    @Override
    public String toString() {
        return "Meterinfo{" +
                "nowDate=" + nowDate +
                ", type=" + type +
                ", requestNum=" + requestNum +
                ", tag='" + tag + '\'' +
                '}';
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
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

    public TimeUnit getType() {
        return type;
    }

    public void setType(TimeUnit type) {
        this.type = type;
    }

}
