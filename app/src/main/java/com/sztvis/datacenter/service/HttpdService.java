package com.sztvis.datacenter.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.sztvis.datacenter.nanohttpd.Httpd;

import java.io.IOException;

public class HttpdService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Httpd httpd = new Httpd(9091);
        try {
            httpd.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
