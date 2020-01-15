package com.sztvis.datacenter.app;

import android.app.AlarmManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;

import com.blankj.utilcode.util.NetworkUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.Utils;
import com.chtj.socket.BaseTcpSocket;
import com.chtj.socket.ISocketListener;
import com.jeremyliao.liveeventbus.LiveEventBus;
import com.sztvis.datacenter.db.DaoMaster;
import com.sztvis.datacenter.db.DaoSession;
import com.sztvis.datacenter.db.OfflineData;
import com.sztvis.datacenter.model.ApiResponse;
import com.sztvis.datacenter.service.HttpdService;
import com.sztvis.datacenter.socket.NettyServer;
import com.sztvis.datacenter.socket.vo.GpsInfo;
import com.sztvis.datacenter.utils.ByteUtil;
import com.sztvis.datacenter.utils.Constants;
import com.sztvis.datacenter.utils.CrashHandler;
import com.sztvis.datacenter.utils.DateUtil;
import com.sztvis.datacenter.utils.HttpUtils;
import com.sztvis.datacenter.utils.TimeUtil;
import com.wp.android_onvif.onvif.SetSystemDateAndTimeThread;
import com.wp.android_onvif.util.OnvifSdk;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class BaseApplication extends Application {
    private static Context mContext;
    public static List<String> gpsList = new ArrayList<>();
    public static DaoSession daoSession;
    public static GpsInfo currentGps = null;
    public static BaseTcpSocket tcpSocket = null;
    public static boolean socketIsAlive = false;
    NettyServer nettyServer;
    @Override
    public void onCreate() {
        super.onCreate();
        Utils.init(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.System.canWrite(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                mContext = getApplicationContext();
                OnvifSdk.initSdk(this);
                initGreenDao();
                initSocket();
                /**
                 * 启动Socket服务端
                 */
                new Thread() {
                    @Override
                    public void run() {
                        nettyServer = new NettyServer();
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

                boolean is24Hour = DateFormat.is24HourFormat(this);
                if (!is24Hour) {
                    android.provider.Settings.System.putString(getContentResolver(), android.provider.Settings.System.TIME_12_24, "24");
                }
                SyncTimeTask syncTimeTask = new SyncTimeTask();
                syncTimeTask.execute();
            }
        }


        //CrashHandler.getInstance().init(this);

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
                LiveEventBus.get(Constants.BUS_KEY_NETWORK).post(BaseApplication.netWorkIsAlive);
                if (currentGps == null) {
                    LiveEventBus.get(Constants.BUS_KEY_GPS).post(false);
                } else {
                    if (!StringUtils.isEmpty(currentGps.getTime())) {
                        try {
                            if (DateUtil.secondBetween(currentGps.getTime(), TimeUtils.getNowString()) > 5) {
                                LiveEventBus.get(Constants.BUS_KEY_GPS).post(false);
                            } else {
                                LiveEventBus.get(Constants.BUS_KEY_GPS).post(true);
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } else {
                        LiveEventBus.get(Constants.BUS_KEY_GPS).post(false);
                    }
                }
                if (BaseApplication.netWorkIsAlive) {
                    List<OfflineData> offlineDataList = daoSession.loadAll(OfflineData.class);
                    for (OfflineData offlineData : offlineDataList) {
                        HttpUtils.postJSON(offlineData.getUrl(), offlineData.getData());
                        daoSession.delete(offlineData);
                    }
                }

                if (!socketIsAlive) {
                    tcpSocket.connect(getAppContext());
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 同步时间
     */
    class SyncTimeTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            while (true) {
                ApiResponse rep = HttpUtils.getServerTime();
                String serverTime = rep.getResult().toString();
                long serverTimespan = DateUtil.StringToDate(serverTime).getTime();
                //服务器时间跟本地时间相差3秒以上
                if (serverTimespan - System.currentTimeMillis() >= 2000) {
                    ((AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE)).setTime(serverTimespan);//设置本机时间
                    OnvifSdk.setSystemDateAndTime(getAppContext(), DateUtil.StringToDate(serverTime), "192.168.10.101", "admin", "admin", new SetSystemDateAndTimeThread.OnSetSystemDateAndTimeCallBack() {
                        @Override
                        public void setSystemDateAndTimeResult(boolean isSuccess, String result) {
                            if (isSuccess) {
                                Log.d("App", "192.168.10.101 时间同步成功");
                                Log.d("App", result);
                            }
                        }
                    });
                    OnvifSdk.setSystemDateAndTime(getAppContext(), DateUtil.StringToDate(serverTime), "192.168.10.102", "admin", "admin", new SetSystemDateAndTimeThread.OnSetSystemDateAndTimeCallBack() {
                        @Override
                        public void setSystemDateAndTimeResult(boolean isSuccess, String result) {
                            if (isSuccess) {
                                Log.d("App", "192.168.10.102 时间同步成功");
                                Log.d("App", result);
                            }
                        }
                    });
                }
                try {
                    Thread.sleep(5000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void initSocket() {
        tcpSocket = new BaseTcpSocket(Constants.T_Server, Constants.T_Port, 5000);
        tcpSocket.setSocketListener(new ISocketListener() {
            @Override
            public void recv(byte[] data, int offset, int size) {
                Log.d("154返回", "返回数据个数:" + size);
                byte[] bytes = new byte[size];
                System.arraycopy(data, 0, bytes, 0, size);
                Log.d("154返回", ByteUtil.byteToHex(bytes));
                if(bytes[4] == 0x01 && bytes[5] == 0x01) {
                    NettyServer.sendDataToALL(bytes);
                }


            }

            @Override
            public void writeSuccess(byte[] data) {
                Log.d("App", "write:" + ByteUtil.byteToHex(data));

            }

            @Override
            public void connSuccess() {
                socketIsAlive = true;
            }

            @Override
            public void connFaild(Throwable t) {
                socketIsAlive = false;
                //服务端出现异常，比如服务端停止运行
                Log.d("Socket", "connFaild" + t.getLocalizedMessage());

            }

            @Override
            public void connClose() {
                //socketIsAlive = false;
                Log.d("Socket", "connClose");
                //tcpSocket.connect(getApplicationContext());
            }
        });
        tcpSocket.connect(this);
    }
}
