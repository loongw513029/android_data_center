package com.sztvis.datacenter.nanohttpd;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.jeremyliao.liveeventbus.LiveEventBus;
import com.sztvis.datacenter.app.BaseApplication;
import com.sztvis.datacenter.model.vo.CardForm;
import com.sztvis.datacenter.socket.vo.GpsInfo;
import com.sztvis.datacenter.utils.Constants;
import com.sztvis.datacenter.utils.HttpUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class Httpd extends NanoHTTPD {

    public Httpd(int port) {
        super(port);
    }

    public Httpd(String hostname, int port) {
        super(hostname, port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        StringBuilder builder = new StringBuilder();
        String uri = session.getUri();
        Log.i("Httpd", "uri:" + uri);
        if (method == Method.POST) {
            Map<String, String> files = new HashMap<String, String>();
            try {
                session.parseBody(files);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ResponseException e) {
                e.printStackTrace();
            }
            if (uri.equals("/getpicdata")) {
                CardForm cardForm = JSON.parseObject(session.getQueryParameterString(),CardForm.class);
                String cardNo = cardForm.getCardNo();
                long time = cardForm.getTime();
                LiveEventBus.get(Constants.BUS_KEY_CARD_RECORD).post(time);//发送消息让主界面截图
                boolean fag = true;
                String b64Str = null;
                //等待主界面截图完成
                while (fag) {
                    if (BaseApplication.Ch1Base64Shot != null) {
                        b64Str = BaseApplication.Ch1Base64Shot;
                        BaseApplication.Ch1Base64Shot = null;
                        fag = false;
                    }
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                builder.append("{\"status\":\"true\",\"picdata\":\"" + b64Str + "\"}");
                asyncSendCardDataToPlatform(cardNo, time, b64Str);
            }
        }
        if (method == Method.GET) {
            if (uri.equals("/setspeed")) {
                Map<String, Object> map = decodeParameter(session.getQueryParameterString());
                int sp = Integer.valueOf(map.get("sp").toString());
                GpsInfo gpsInfo = new GpsInfo();
                gpsInfo.setSpeed(sp);
                BaseApplication.currentGps = gpsInfo;
                Log.d("Httpd", map.get("sp").toString());
                builder.append("{\"code\":0,\"msg\":\"设置成功\"}");
            }
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", builder.toString());
    }

    private void asyncSendCardDataToPlatform(String card_no, long timespan, String pic) {
        new Thread() {
            @Override
            public void run() {
                HttpUtils.saveSwingCardRecord(card_no, timespan, pic);
            }
        }.start();
    }

    private Map<String, Object> decodeParameter(String getParams) {
        Map<String, Object> map = new HashMap<>();
        String[] arr = getParams.split("&");
        for (int i = 0; i < arr.length; i++) {
            String[] arr2 = arr[i].split("=");
            map.put(arr2[0], arr2[1]);
        }
        return map;
    }
}
