package com.cloudzone.cloudlimiter.base;

/**
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/5
 */
public enum IntervalModel {
    SECOND, // 只推送秒间隔统计的数据
    MINUTE, // 只推送分钟间隔统计的数据(需要等到开始的第二分钟才会推送数据)
    ALL; // 推送上述所有类型数据
}
