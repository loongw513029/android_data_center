package com.sztvis.datacenter.model.vo;

import java.io.Serializable;

public class CardForm implements Serializable {
    private String cardNo;
    private long time;
    private int num;

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }
}
