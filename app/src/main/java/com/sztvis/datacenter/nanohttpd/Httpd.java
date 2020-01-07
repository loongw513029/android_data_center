package com.sztvis.datacenter.nanohttpd;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.jeremyliao.liveeventbus.LiveEventBus;
import com.sztvis.datacenter.app.BaseApplication;
import com.sztvis.datacenter.model.vo.CardForm;
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
        if (method == Method.POST) {
            String uri = session.getUri();
            Log.i("Httpd", "uri:" + uri);
            Map<String, String> files = new HashMap<String, String>();
            try {
                session.parseBody(files);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ResponseException e) {
                e.printStackTrace();
            }
            if (uri.equals("/getpicdata")) {
                String jsonBody = files.get("postData");
                CardForm cardForm = JSON.parseObject(jsonBody, CardForm.class);
                LiveEventBus.get(Constants.BUS_KEY_CARD_RECORD).post(cardForm.getTime());//发送消息让主界面截图
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
                asyncSendCardDataToPlatform(cardForm.getCardNo(), cardForm.getTime(), b64Str);
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
}
