package com.sztvis.datacenter.socket;

public class SocketType {
    //心跳数据
    public static final int HEALTH = 0;
    //刷卡数据
    public static final int PAYRECORD = 1;
    //GPS数据
    public static final int GPS = 2;
    //一键报警
    public static final int ONKEYALARM = 3;

    //一键巡检
    public static final int ONKEYINSPECT = 4;

    //CAN
    public static final int CAN = 5;

    //客流数据
    public static final int KELIU = 6;

    //安全带
    public static final int SAFEBEAT = 7;

    //对讲系统
    public static final int TALKSYSTEM = 8;
}
