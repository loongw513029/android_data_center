package com.sztvis.datacenter.socket;

import android.util.Log;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class NettyServer implements onChannelOperation {
    private String TAG = "NettyServer";
    private EventLoopGroup bossGroup = new NioEventLoopGroup();
    private EventLoopGroup workerGroup = new NioEventLoopGroup();

    public static final ConcurrentHashMap<NettyServerHandler, SocketChannel> channelMap = new ConcurrentHashMap<>();

    public void start() {
        try {
            Thread t = Thread.currentThread();
            Log.i(TAG, "run() in EchoServer" + Calendar.getInstance().getTime() + "____" + t.getName());
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            Log.i(TAG, "有客户端连接了:" + socketChannel);
                            NettyServerHandler scobj = new NettyServerHandler(NettyServer.this);
                            socketChannel.pipeline().addLast(scobj);
                            channelMap.put(scobj, socketChannel);
                            Log.i(TAG, "socket通道数量:" + "--" + channelMap.size());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 32 * 1024)// 设置TCP缓冲区
                    .option(ChannelOption.SO_SNDBUF, 64 * 1024) //发送数据缓冲区
                    .option(ChannelOption.SO_RCVBUF, 64 * 1024) //接收数据缓冲区
                    .childOption(ChannelOption.SO_KEEPALIVE, true);//保持连接

            ChannelFuture future = bootstrap.bind(8087).sync();
            Log.i(TAG, "服务器已启动,端口：" + 8087);
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.i(TAG, "服务器启动失败");
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            //log.info(“);
        }
    }

    public void close() {
        Log.i(TAG, "正在尝试关闭Netty");
        bossGroup.shutdownGracefully().syncUninterruptibly();
        workerGroup.shutdownGracefully().syncUninterruptibly();
        channelMap.clear();
        Log.i(TAG, "关闭成功");
    }


    @Override
    public void onRemoveChannel(NettyServerHandler obj) {
        channelMap.remove(obj);
        System.out.println("移除客户端[" + obj.getEquipId() + "]:" + obj.getChannelHandler().channel());
    }

    @Override
    public void removeOtherChannel(String dev_sn) {
        for (Map.Entry<NettyServerHandler, SocketChannel> entry : channelMap.entrySet()) {
            if (entry.getKey().getEquipId().equals(dev_sn)) {
                channelMap.remove(entry.getKey());
                System.out.println("移除链接！！！:" + dev_sn);
            }
        }
    }

    /**
     * 检测设备是否存活
     *
     * @param dev_sn
     * @return
     */
    public static boolean socketIsAlive(String dev_sn) {
        for (Map.Entry<NettyServerHandler, SocketChannel> entry : channelMap.entrySet()) {
            if (entry.getKey().getEquipId().equals(dev_sn)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 发送数据
     *
     * @param dev_sn
     * @param msg
     */
    public static void sendData(String dev_sn, byte[] msg) {
        if (socketIsAlive(dev_sn)) {
            for (Map.Entry<NettyServerHandler, SocketChannel> entry : channelMap.entrySet()) {
                if (entry.getKey().getEquipId().equals(dev_sn)) {
                    entry.getKey().sendDataAPI(dev_sn, msg);
                }
            }
        }
    }

    public static void sendDataToALL(byte[] msg) {
        for (Map.Entry<NettyServerHandler, SocketChannel> entry : channelMap.entrySet()) {
            entry.getKey().sendDataAPI(entry.getKey().getEquipId(), msg);
            Log.d("NettyServer","发送对象:"+entry.getKey().getChannelHandler());
        }
    }


}
