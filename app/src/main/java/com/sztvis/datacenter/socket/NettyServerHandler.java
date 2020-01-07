package com.sztvis.datacenter.socket;

import android.util.Log;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.sztvis.datacenter.socket.vo.BaseMsg;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.concurrent.atomic.AtomicInteger;

public class NettyServerHandler extends ChannelInboundHandlerAdapter {
    private String TAG = "NettyServerHandler";

    /**
     * 空闲次数
     **/
    private AtomicInteger idle_count = new AtomicInteger(1);

    /**
     * 发送次数
     **/
    private AtomicInteger count = new AtomicInteger(1);


    private onChannelOperation mListener;

    public NettyServerHandler(onChannelOperation mListener) {
        this.mListener = mListener;
    }

    private ChannelHandlerContext channelHandler;

    private String equipId;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object obj) throws Exception {
        if (obj instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) obj;
            //如果读通道处于空闲状态，说明没有收到心跳命令
            if (IdleState.READER_IDLE.equals(event.state())) {
                Log.i(TAG, "已经60秒没有接收到客户端[" + this.getEquipId() + "]的信息了");
                Log.i(TAG, "关掉不活跃的通道[" + this.getEquipId() + "]");
                ctx.channel().close();
                idle_count.getAndIncrement();
            }
        } else {
            super.userEventTriggered(ctx, obj);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        mListener.onRemoveChannel(this);
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String request = (String) msg;
        Log.i(TAG, "request:" + request);
        dealData(ctx, request);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        Log.i(TAG, "exceptionCaught");
        if (ctx.channel().isActive()) {
            mListener.onRemoveChannel(this);
            ctx.close();
            //icService.updateDevState(this.getEquipId(), 0);
        }
    }

    public void sendDataAPI(String equipId, String sendData) {
        if (channelHandler != null) {
            channelHandler.writeAndFlush(Unpooled.copiedBuffer(sendData.getBytes()));
            Log.i(TAG, "向设备" + equipId + "发送了数据:" + sendData);
        }
    }

    private void dealData(ChannelHandlerContext ctx, String msg) {
        Log.i(TAG, "->  " + msg);
        try {
            channelHandler = ctx;
            BaseMsg baseMsg = JSON.parseObject(msg, BaseMsg.class);
            switch (baseMsg.getT()) {
                case SocketType.HEALTH://心跳
                    break;
                case SocketType.PAYRECORD:
                    break;
                case SocketType.GPS:
                    break;
                case SocketType.ONKEYALARM:
                    break;
                case SocketType.ONKEYINSPECT:
                    break;
                case SocketType.CAN:
                    break;
                case SocketType.KELIU:
                    break;
                case SocketType.SAFEBEAT:
                    break;
                case SocketType.TALKSYSTEM:
                    break;
            }
            //从信息中获取设备ID
            //equipId = "";
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public ChannelHandlerContext getChannelHandler() {
        return channelHandler;
    }

    public void setChannelHandler(ChannelHandlerContext channelHandler) {
        this.channelHandler = channelHandler;
    }

    public String getEquipId() {
        return equipId;
    }

    public void setEquipId(String equipId) {
        this.equipId = equipId;
    }
}
