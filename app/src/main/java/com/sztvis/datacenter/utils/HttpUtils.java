package com.sztvis.datacenter.utils;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.blankj.utilcode.util.NetworkUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.sztvis.datacenter.app.BaseApplication;
import com.sztvis.datacenter.db.OfflineData;
import com.sztvis.datacenter.model.ApiResponse;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpUtils {

    private static int timeout = 5000;

    public static void setTimeout(int t) {
        HttpUtils.timeout = t;
    }

    private static OkHttpClient client = new OkHttpClient();

    public static String postJSON(String url, String jsonStr) {
        client = new OkHttpClient.Builder()
                .callTimeout(timeout, TimeUnit.MILLISECONDS)
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .build();
        MediaType jsonType = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonType, jsonStr);
        Request request = new Request.Builder()
                .url(Constants.BASE_URL + url)
                .post(body)
                .build();
        Call call = client.newCall(request);
        Response response = null;
        try {
            response = call.execute();
            String res = request.body().toString();
            Log.d("HttpUtils", res);
            return res;

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return null;
    }

    public static String get(String url) {
        final Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = client.newCall(request);
        Response response = null;
        try {
            response = call.execute();
            String res = request.body().toString();
            Log.d("HttpUtils", res);
            return res;

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return null;
    }

    public static String saveSwingCardRecord(String card_no, long timespan, String pic) {
        String time = DateUtil.StringToString(timespan + "", "yyyyMMddHHmmss", "yyyy-MM-dd HH:mm:ss");
        String jsonStr = "{\"dev_code\":\"" + BaseApplication.DevCode + "\",\"card_no\":\"" + card_no + "\",\"location\":\"\",\"site_name\":\"\",\"shot_image\":\"" + pic + "\",\"update_time\":\"" + time + "\"}";
        if (NetworkUtils.isAvailableByPing(Constants.HOST)) {
            return postJSON("/dataTerminal/saveCardData", jsonStr);
        } else {
            OfflineData offlineData = new OfflineData(null, "/dataTerminal/saveCardData", jsonStr);
            BaseApplication.daoSession.insert(offlineData);
            return null;
        }
    }

    public static String saveFaceDetectRecord(String b64Str, int faceNums) {
        String jsonStr = "{\"dev_code\":\"" + BaseApplication.DevCode + "\",\"ch\":1,\"face_nums\":" + faceNums + ",\"face_image\":\"" + b64Str + "\",\"detect_time\":\"" + TimeUtils.getNowString() + "\"}";
        if (NetworkUtils.isAvailableByPing(Constants.HOST)) {
            return postJSON("/dataTerminal/saveDetectRecord", jsonStr);
        } else {
            OfflineData offlineData = new OfflineData(null, "/dataTerminal/saveDetectRecord", jsonStr);
            BaseApplication.daoSession.insert(offlineData);
            return null;
        }
    }

    public static ApiResponse getServerTime() {
        if (NetworkUtils.isAvailableByPing(Constants.HOST)) {
            String res = get("/platfrom/checkTime");
            return JSON.parseObject(res, ApiResponse.class);
        }
        return null;
    }


}
