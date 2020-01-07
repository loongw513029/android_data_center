package com.sztvis.datacenter.socket;

public interface onChannelOperation {


    void onRemoveChannel(NettyServerHandler obj);

    void removeOtherChannel(String dev_sn);
}
