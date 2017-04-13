package com.cloudzone.cloudlimiter.base;

/**
 * @author tantexian, <my.oschina.net/tantexian>
 * @since 2017/4/5
 */
public enum IntervalModel {
    SECOND, // 只统计秒间隔统计的数据
    MINUTE, // 只统计分钟间隔统计的数据(需要等到开始的第二分钟才会推送数据)
    ALL; // 统计上述所有类型数据
}
