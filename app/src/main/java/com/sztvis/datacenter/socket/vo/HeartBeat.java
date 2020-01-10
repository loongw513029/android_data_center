package com.sztvis.datacenter.socket.vo;

import com.sztvis.datacenter.utils.ByteUtil;


/**
 * Created by Administrator on 2019/8/1.
 */

public class HeartBeat extends DataBase{
    private short version;
    private int timelong;
    private String deviceCode;

    public short getVersion() {
        return version;
    }

    public void setVersion(short version) {
        this.version = version;
    }

    public int getTimelong() {
        return timelong;
    }

    public void setTimelong(int timelong) {
        this.timelong = timelong;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    @Override
    public void parseBody(byte[] message) {
        this.setVersion(ByteUtil.subBytesToShort(message,18,2));
        this.setDeviceCode(ByteUtil.subBytesToString(message,6,12).replaceAll("\\u0000",""));
    }
}

