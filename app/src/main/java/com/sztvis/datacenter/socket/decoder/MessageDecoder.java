package com.sztvis.datacenter.socket.decoder;

import android.util.Log;

import com.sztvis.datacenter.app.BaseApplication;
import com.sztvis.datacenter.socket.NettyServerHandler;
import com.sztvis.datacenter.socket.resp.ControlReplyResult;
import com.sztvis.datacenter.socket.resp.DeviceStatusResult;
import com.sztvis.datacenter.socket.resp.IoResult;
import com.sztvis.datacenter.socket.resp.ReplyStateResult;
import com.sztvis.datacenter.socket.resp.RespBase;
import com.sztvis.datacenter.socket.resp.RfidDistanceResult;
import com.sztvis.datacenter.socket.vo.*;
import com.sztvis.datacenter.utils.ByteUtil;



/**
 * Created by Administrator on 2019/8/1.
 */

public class MessageDecoder {
    private byte[] message;
    int crc = 0;
    private NettyServerHandler ctx;
    public MessageDecoder(byte[] message, NettyServerHandler ctx) {
        this.message = message;
        this.ctx = ctx;
        for (int i = 0; i < message.length; i++) {
            message[i] &= 0xFF;
        }
        crc = ((message[message.length - 4] << 8) & 0xFF00) | (message[message.length - 3] & 0xFF);
        message[message.length - 4] = 0x00;
        message[message.length - 3] = 0x00;
    }

    /**
     * check CRC16
     *
     * @return
     */
    public boolean checkCrc() {
        int crc16r = CRC16.CRC16_Check(message);
        if (crc == crc16r)
            return true;
        else
            return false;
    }

    /**
     * 心跳应答消息
     * @return
     */
    private byte[] healthAnswer() {
        byte[] bytes = new byte[]{(byte) 0xBF, (byte) 0xCF, 0x00, 0x03, 0x01, 0x01, 0x00, (byte) 0xDF, (byte) 0xEF};
        short crc16 = (short) CRC16.CRC16_Check(bytes);
        bytes[bytes.length - 4] = (byte) (crc16 >> 8);
        bytes[bytes.length - 3] = (byte) crc16;
        return bytes;
    }

    public void decode() {
        switch (message[4]) {
            case 0x01:
                switch (message[5]) {
                    case 0x01://心跳
                        HeartBeat heartBeat = new HeartBeat();
                        heartBeat.parseBody(message);
                        ctx.setEquipId(heartBeat.getDeviceCode());
//                        if(ctx!=null){
//                            ctx.sendDataAPI(ctx.getEquipId(),healthAnswer());
//                        }
                        break;
                    case 0x02://GPS
                        GpsInfo gpsInfo = new GpsInfo();
                        int res = gpsInfo.parseBody2(message);
                        if (res == 0){//获取成功的
                            BaseApplication.currentGps = gpsInfo;
                        }
                        break;
                    case 0x04:
                        Rfid rfid = new Rfid();
                        rfid.parseBody(message);
                        break;
                    case 0x05:
                        Sensor sensor = new Sensor();
                        sensor.parseBody(message);
                        break;
                    case 0x06:
                        GSensor gSensor = new GSensor();
                        gSensor.parseBody(message);
                        break;

                    case 0x08:
                        NfcMsg nfcMsg = new NfcMsg();
                        nfcMsg.parseBody(message);
                        break;
                }
                break;
            case 0x02:
                switch (message[5]) {
                    case 0x01:
                        IO io = new IO();
                        io.parseBody(message);
                        break;
                    case 0x02:
                        Reply reply = new Reply();
                        reply.parseBody(message);
                        break;
                }
                break;

            case 0x04:
                switch (message[5]) {
                    case 0x01:
                        RespBase res = new RespBase();
                        res.setCode(message[6]);
                        break;
                    case 0x02:
                        RespBase res1 = new RespBase();
                        res1.setCode(message[6]);
                        break;
                }
                break;
            case 0x05:
                switch (message[5]) {
                    case 0x01:
                        DeviceStatusResult res = new DeviceStatusResult();
                        res.setCode(message[6]);
                        res.setVersion(ByteUtil.subBytesToShort(message, 7, 2));
                        res.setTimelong(ByteUtil.subBytesToInteger(message, 9, 4));
                        break;
                    case 0x02:
                        IoResult res1 = new IoResult();
                        res1.setCode(message[6]);
                        res1.setIoNo(message[7]);
                        res1.setIoVal(message[8]);
                        break;
                    case 0x03:
                        RfidDistanceResult res2 = new RfidDistanceResult();
                        res2.setCode(message[6]);
                        res2.setLevel(message[7]);
                        break;
                    case 0x04:
                        ReplyStateResult res3 = new ReplyStateResult();
                        res3.setCode(message[6]);
                        res3.setReplayNo(message[7]);
                        res3.setReplayVal(message[8]);
                        break;
                }
                break;
            case 0x06:
                switch (message[5]) {
                    case 0x01:
                        RespBase res = new RespBase();
                        res.setCode(message[6]);
                        break;
                    case 0x02:
                        ControlReplyResult res1 = new ControlReplyResult();
                        res1.setCode(message[6]);
                        res1.setReplyNo(message[7]);
                        break;
                }
                break;
        }
    }
}
