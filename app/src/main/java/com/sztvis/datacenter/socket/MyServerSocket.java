package com.sztvis.datacenter.socket;

import android.util.Log;

import com.sztvis.datacenter.utils.ByteUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyServerSocket {
    private boolean isEnable;
    private final ExecutorService threadPool;//线程池
    public static ServerSocket socket;
    private static Socket socket2;
    public MyServerSocket() {
        threadPool = Executors.newCachedThreadPool();
    }

    /**
     * 开启server
     */
    public void startServerAsync() {
        isEnable=true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                doProcSync();
            }
        }).start();
    }

    /**
     * 关闭server
     */
    public void stopServerAsync() throws IOException {
        if (!isEnable){
            return;
        }
        isEnable=true;
        socket.close();
        socket=null;
    }

    public void doProcSync() {
        try {
            InetSocketAddress socketAddress=new InetSocketAddress(8087);
            socket = new ServerSocket();
            socket.bind(socketAddress);
            while (isEnable){
                final Socket remotePeer= socket.accept();
                socket2 = remotePeer;
                threadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("MyServerSocket","remotePeer..............."+remotePeer.getRemoteSocketAddress().toString());
                        onAcceptRemotePeer(remotePeer);
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onAcceptRemotePeer(Socket remotePeer) {
        try {
            remotePeer.getOutputStream().write("connected successful".getBytes());//告诉客户端连接成功
            // 从Socket当中得到InputStream对象
            InputStream inputStream = remotePeer.getInputStream();
            byte[] buffer = new byte[1024 * 4];
            int temp = 0;
            // 从InputStream当中读取客户端所发送的数据
            while ((temp = inputStream.read(buffer)) != -1) {
                Log.e("MyServerSocket", ByteUtil.byteToHex(buffer));
                //remotePeer.getOutputStream().write(buffer, 0, temp);//把客户端传来的消息发送回去
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendData(byte[] bytes){
        if(socket2!=null){
            try {
                socket2.getOutputStream().write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
