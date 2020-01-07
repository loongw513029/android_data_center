package com.sztvis.datacenter.app;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.blankj.utilcode.util.NetworkUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.sztvis.datacenter.db.DaoMaster;
import com.sztvis.datacenter.db.DaoSession;
import com.sztvis.datacenter.db.OfflineData;
import com.sztvis.datacenter.service.HttpdService;
import com.sztvis.datacenter.socket.NettyServer;
import com.sztvis.datacenter.utils.Constants;
import com.sztvis.datacenter.utils.HttpUtils;

import java.util.List;

public class BaseApplication extends Application {
    private static Context mContext;

    public static DaoSession daoSession;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        initGreenDao();
        /**
         * 启动Socket服务端
         */
        new Thread() {
            @Override
            public void run() {
                NettyServer nettyServer = new NettyServer();
                nettyServer.start();
            }
        }.start();
        /**
         * 启动Http服务器
         */
        Intent httpdIntent = new Intent(this, HttpdService.class);
        startService(httpdIntent);

        NetworkTask networkTask = new NetworkTask();
        networkTask.execute();
    }

    public static Context getAppContext() {
        return mContext;
    }

    public static String Ch1Base64Shot = null;

    public static String DevCode = "";

    public static boolean netWorkIsAlive = false;

    /**
     * 初始话GreenDao
     */
    private void initGreenDao() {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "driver.db");
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        BaseApplication.daoSession = daoMaster.newSession();
    }


    //网络检测线程
    class NetworkTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            while (true) {
                BaseApplication.netWorkIsAlive = NetworkUtils.isAvailableByPing(Constants.HOST);
                if(BaseApplication.netWorkIsAlive){
                    List<OfflineData> offlineDataList = daoSession.loadAll(OfflineData.class);
                    for(OfflineData offlineData:offlineDataList){
                        HttpUtils.postJSON(offlineData.getUrl(),offlineData.getData());
                        daoSession.delete(offlineData);
                    }
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
