package com.sztvis.datacenter.socket;

import android.util.Log;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.sztvis.datacenter.app.BaseApplication;
import com.sztvis.datacenter.socket.decoder.MessageDecoder;
import com.sztvis.datacenter.socket.vo.BaseMsg;
import com.sztvis.datacenter.utils.ByteUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.Arrays;
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
    private byte[] temp = new byte[1024];

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
        channelHandler = ctx;
        ByteBuf bb = (ByteBuf) msg;
        // 创建一个和buf同等长度的字节数组
        byte[] reqByte = new byte[bb.readableBytes()];
        // 将buf中的数据读取到数组中
        bb.readBytes(reqByte);
        if (reqByte.length > 5) {
            System.arraycopy(temp, 0, reqByte, 0, bb.readableBytes());
            int length = 0;
            //Log.i("IOT1",Arrays.toString(res));
            for (int i = 0; i < reqByte.length; i++) {
                if ((reqByte[i] & 0xFF) == 0xDF && (reqByte[i + 1] & 0xFF) == 0xEF) {
                    byte[] bts = new byte[i + 2 - length];
                    System.arraycopy(reqByte, length, bts, 0, i + 2 - length);
                    Log.i("IOT2", ByteUtil.byteToHex(bts));
                    length = i + 2;
                    dealData(ctx, reqByte);
                }
            }
        }
        Log.i(TAG, "request:" + ByteBufUtil.hexDump(reqByte));

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

    public void sendDataAPI(String equipId, byte[] bytes) {
        if (channelHandler != null) {
            ChannelFuture channelFuture = channelHandler.writeAndFlush(Unpooled.copiedBuffer(bytes));
            Log.i(TAG, "向设备" + equipId + "发送了数据:" + ByteUtil.byteToHex(bytes) + ",结果：" + channelFuture.isSuccess());
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if(future.isSuccess()){
                        Log.i(TAG, "重发成功");
                    }else{
                        Log.i(TAG, future.cause().toString());
                    }
                }
            });

        }
    }

    private void dealData(ChannelHandlerContext ctx, byte[] bytes) {
        //Log.i(TAG, "->  " + msg);

        try {

            if (BaseApplication.socketIsAlive) {
                BaseApplication.tcpSocket.send(bytes);
            }
            MessageDecoder messageDecoder = new MessageDecoder(bytes, this);
            if (messageDecoder.checkCrc()) {
                messageDecoder.decode();
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
