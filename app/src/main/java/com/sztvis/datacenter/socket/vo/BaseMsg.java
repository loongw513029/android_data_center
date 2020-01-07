package com.sztvis.datacenter.socket.vo;

import java.io.Serializable;

public class BaseMsg implements Serializable {
    private int t;
    private Object m;

    public int getT() {
        return t;
    }

    public void setT(int t) {
        this.t = t;
    }

    public Object getM() {
        return m;
    }

    public void setM(Object m) {
        this.m = m;
    }
}
