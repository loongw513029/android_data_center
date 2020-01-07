package com.sztvis.datacenter.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sztvis.datacenter.MainActivity;

public class BootReceiver extends BroadcastReceiver {
    //这个广播应该与清单文件的保持一致
    final String ACTION = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION)) {
            Intent mainActivityIntent = new Intent(context, MainActivity.class);
            //这个很重要，是开机自启动的广播 别写错了
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mainActivityIntent);
        }
    }
}
