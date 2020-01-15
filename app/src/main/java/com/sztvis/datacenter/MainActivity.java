package com.sztvis.datacenter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.arcsoft.face.ActiveFileInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.Face3DAngle;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.enums.DetectFaceOrientPriority;
import com.arcsoft.face.enums.DetectMode;
import com.arcsoft.face.enums.DetectModel;
import com.arcsoft.imageutil.ArcSoftImageFormat;
import com.arcsoft.imageutil.ArcSoftImageUtil;
import com.arcsoft.imageutil.ArcSoftImageUtilError;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.jeremyliao.liveeventbus.LiveEventBus;
import com.sztvis.datacenter.app.BaseApplication;
import com.sztvis.datacenter.media.widget.IpCameraView;
import com.sztvis.datacenter.socket.NettyServer;
import com.sztvis.datacenter.socket.vo.GpsInfo;
import com.sztvis.datacenter.utils.Constants;
import com.sztvis.datacenter.utils.FileUtils;
import com.sztvis.datacenter.utils.HttpUtils;
import com.sztvis.datacenter.utils.ImageUtils;
import com.sztvis.datacenter.utils.UpdateInterface;
import com.sztvis.datacenter.utils.UpdateUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private IpCameraView ipCameraView1;
    private IpCameraView ipCameraView2;
    Button scrShot1, scrShot2, scrShot3, scrShot4;


    private FaceEngine faceEngine = new FaceEngine();
    /**
     * VIDEO模式人脸检测引擎，用于预览帧人脸追踪
     */
    private FaceEngine ftEngine;

    private int ftInitCode;

    private List<FaceInfo> faceInfoList = new ArrayList<>();

    private TextView tipTxt;

    EditText txCode;
    Button updateBtn;

    Button btn1, btn2;

    ImageView serverState, gpsState, engineState;

    TextView txtVer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ipCameraView1 = (IpCameraView) findViewById(R.id.ipc_view1);
        ipCameraView2 = (IpCameraView) findViewById(R.id.ipc_view2);
        btn1 = (Button) findViewById(R.id.btn1);
        btn2 = (Button) findViewById(R.id.btn2);
        tipTxt = (TextView) findViewById(R.id.tip_txt);
        txCode = (EditText) findViewById(R.id.tx_code);
        updateBtn = (Button) findViewById(R.id.btn_update);
        serverState = (ImageView) findViewById(R.id.server_state);
        gpsState = (ImageView) findViewById(R.id.gps_state);
        engineState = (ImageView) findViewById(R.id.engine_state);
        txtVer = (TextView) findViewById(R.id.txt_ver);
        txtVer.setText("版本\nv" + AppUtils.getAppVersionName());
        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        updateBtn.setOnClickListener(this);
        BaseApplication.DevCode = SPUtils.getInstance().getString(Constants.SP_DEVICECODE, "T-1");
        txCode.setText(SPUtils.getInstance().getString(Constants.SP_DEVICECODE, "T-1"));
        //txCode.setOn
        UpdateUtil.checkHaveNewApp(this, new UpdateInterface() {
            @Override
            public void haveNewApp(boolean haveNewApp) {

            }
        });
        requestPermiss();

    }

    private void searchIpc() {
        ipCameraView1.play("rtsp://192.168.10.101:554/11");
        ipCameraView2.play("rtsp://192.168.10.102:554/11");
        initLiveBus();
    }

    private void requestPermiss() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_SETTINGS}, 0);
        } else {
            searchIpc();
            activeEngine();
        }
    }

    private void initLiveBus() {
        LiveEventBus.get(Constants.BUS_KEY_CARD_RECORD, Long.class)
                .observe(this, new androidx.lifecycle.Observer<Long>() {
                    @Override
                    public void onChanged(Long currentTimeMillis) {
                        String base64 = screenShot(0, currentTimeMillis + "");
                    }
                });
        LiveEventBus.get(Constants.BUS_KEY_GPS, Boolean.class)
                .observe(this, new androidx.lifecycle.Observer<Boolean>() {
                    @Override
                    public void onChanged(Boolean aBoolean) {
                        if (aBoolean) {
                            gpsState.setImageResource(R.mipmap.ic_green);
                        } else {
                            gpsState.setImageResource(R.mipmap.ic_red);
                        }
                    }
                });
        LiveEventBus.get(Constants.BUS_KEY_NETWORK, Boolean.class)
                .observe(this, new androidx.lifecycle.Observer<Boolean>() {
                    @Override
                    public void onChanged(Boolean aBoolean) {
                        if (aBoolean) {
                            serverState.setImageResource(R.mipmap.ic_green);
                        } else {
                            serverState.setImageResource(R.mipmap.ic_red);
                        }
                    }
                });
    }

    public void startRecord(int ch) {
        record(ch);
    }

    private void record(int ch) {
        File file;
        if (new File("/storage/sdcard/").exists()) {
            file = new File("/storage/sdcard/RecordVideos/" + ch);
        } else {
            file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/RecordVideos/" + ch);
        }
        if (!file.exists() && !file.mkdirs()) {
            Log.e(TAG, "录像保存路径错误");
            return;
        }
        String date = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date(System.currentTimeMillis()));
        String path = file.getAbsolutePath() + "/" + date + ".mp4";
        switch (ch) {
            case 0:
                ipCameraView1.startRecord(path);
                break;
        }
    }


    public String screenShot(int ch, String name) {
        File file;
        if (new File("/storage/sdcard/").exists()) {
            file = new File("/storage/sdcard/ScreenShots/" + ch);
        } else {
            file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/ScreenShots/" + (ch == 0 ? "swingcard" : ch + ""));
        }
        if (!file.exists() && !file.mkdirs()) {
            Log.e(TAG, "录像保存路径错误");
            return null;
        }
        Bitmap bitmap = null;
        switch (ch) {
            case 0:
                bitmap = ipCameraView1.screenShot2();
                break;
        }
        String path = file.getAbsolutePath() + File.separator + name + ".jpg";
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Toast.makeText(this, "截图成功", Toast.LENGTH_SHORT).show();
        if (bitmap != null) {
            String base64Str = ImageUtils.ImageToBase64(path).replaceAll("\r\n", "").replaceAll("\n","");
            BaseApplication.Ch1Base64Shot = base64Str;
            return base64Str;
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            searchIpc();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        ipCameraView1.stopRecord();
        ipCameraView1.stop();
        ipCameraView1.stop();
        IjkMediaPlayer.native_profileEnd();
    }


    @Override
    protected void onDestroy() {
        unInitEngine();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    /**
     * 激活引擎
     */
    public void activeEngine() {

        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                int activeCode = faceEngine.activeOnline(MainActivity.this, Constants.APP_ID, Constants.SDK_KEY);
                emitter.onNext(activeCode);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Integer activeCode) {
                        if (activeCode == ErrorInfo.MOK) {
                            Toast.makeText(MainActivity.this, "引擎激活成功", Toast.LENGTH_SHORT).show();
                            engineState.setImageResource(R.mipmap.ic_green);
                            initFaceEngine();
                        } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
                            Toast.makeText(MainActivity.this, "引擎已激活", Toast.LENGTH_SHORT).show();
                            engineState.setImageResource(R.mipmap.ic_green);
                            //Toast.makeText(MainActivity.this,"激活失败",Toast.LENGTH_SHORT).show();
                            //ToastUtil.showToast(FaceActivity.this,getString(R.string.already_activated));
                            initFaceEngine();
                        } else {
                            engineState.setImageResource(R.mipmap.ic_red);
                            Toast.makeText(MainActivity.this, "引擎激活失败", Toast.LENGTH_SHORT).show();
                        }

                        ActiveFileInfo activeFileInfo = new ActiveFileInfo();
                        int res = faceEngine.getActiveFileInfo(MainActivity.this, activeFileInfo);
                        if (res == ErrorInfo.MOK) {
                            Log.i(TAG, activeFileInfo.toString());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    private void initFaceEngine() {
        ftEngine = new FaceEngine();
        ftInitCode = ftEngine.init(this, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_ALL_OUT,
                8, 16, FaceEngine.ASF_FACE_DETECT | FaceEngine.ASF_AGE | FaceEngine.ASF_FACE3DANGLE | FaceEngine.ASF_GENDER | FaceEngine.ASF_LIVENESS);
        if (ftInitCode != ErrorInfo.MOK) {
            String error = getString(R.string.specific_engine_init_failed, "ftEngine", ftInitCode);
            Log.i(TAG, "initEngine: " + error);
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        }

        FaceCheckThread faceCheckThread = new FaceCheckThread();
        faceCheckThread.start();
    }

    private Handler handler = new Handler();

    private void unInitEngine() {
        if (ftInitCode == ErrorInfo.MOK && ftEngine != null) {
            synchronized (ftEngine) {
                int ftUnInitCode = ftEngine.unInit();
                Log.i(TAG, "unInitEngine: " + ftUnInitCode);
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn1:
                btn1.setBackground(getDrawable(R.drawable.bg_button));
                btn2.setBackground(getDrawable(R.drawable.bg_button_gray));
                ipCameraView1.setVisibility(View.VISIBLE);
                ipCameraView2.setVisibility(View.INVISIBLE);
                break;
            case R.id.btn2:
                btn1.setBackground(getDrawable(R.drawable.bg_button_gray));
                btn2.setBackground(getDrawable(R.drawable.bg_button));
                ipCameraView1.setVisibility(View.INVISIBLE);
                ipCameraView2.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_update:
                String code = txCode.getText().toString().trim();
                if (code.equals("")) {
                    ToastUtils.showShort("设备编码不能为空");
                    return;
                }
                SPUtils.getInstance().put(Constants.SP_DEVICECODE, code);
                BaseApplication.DevCode = code;
                byte[] byte2 = new byte[]{(byte) 0xBF, (byte) 0xCF, 0x00, 0x03, 0x04, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xDF, (byte) 0xEF};
                byte[] b2 = code.getBytes();
                System.arraycopy(b2, 0, byte2, 6, b2.length);
                ToastUtils.showShort("修改成功");

                break;
        }
    }

    class FaceCheckThread extends Thread {
        @Override
        public void run() {
            while (true) {
                //判断车辆是否处理静止状态
                GpsInfo gpsInfo = BaseApplication.currentGps;
                if (gpsInfo == null) continue;
                Log.d(TAG, gpsInfo.getSpeed() + "");
                if (gpsInfo.getSpeed() > 0) continue;

                Bitmap bitmapTemp = ipCameraView2.screenShot2();
                Bitmap cavBitmap = bitmapTemp;
                //Log.d(TAG, bitmapTemp.getWidth() + "," + bitmapTemp.getHeight());
                FileOutputStream fos1 = null;
                try {
                    Thread.sleep(200);
                    fos1 = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "temp.jpg");
                    bitmapTemp.compress(Bitmap.CompressFormat.JPEG, 80, fos1);
                    fos1.close();
                    // 图像对齐
                    Bitmap bitmap = ArcSoftImageUtil.getAlignedBitmap(bitmapTemp, true);
                    if (ftInitCode != ErrorInfo.MOK) {
                        Log.e(TAG, "face engine not initialized!");
                        return;
                    }
                    if (bitmap == null) {
                        Log.e(TAG, "bitmap is null!");
                        return;
                    }
                    if (ftEngine == null) {
                        Log.e(TAG, "faceEngine is null!");
                        return;
                    }
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    // bitmap转bgr24
                    byte[] bgr24 = ArcSoftImageUtil.createImageData(bitmap.getWidth(), bitmap.getHeight(), ArcSoftImageFormat.BGR24);
                    int transformCode = ArcSoftImageUtil.bitmapToImageData(bitmap, bgr24, ArcSoftImageFormat.BGR24);
                    if (transformCode != ArcSoftImageUtilError.CODE_SUCCESS) {
                        Log.e(TAG, "transform failed, code is " + transformCode);
                        return;
                    }
                    faceInfoList.clear();
                    /**
                     * 2.成功获取到了BGR24 数据，开始人脸检测
                     */
                    long fdStartTime = System.currentTimeMillis();
                    int detectCode = ftEngine.detectFaces(bgr24, width, height, FaceEngine.CP_PAF_BGR24, DetectModel.RGB, faceInfoList);
                    if (detectCode == ErrorInfo.MOK) {
                        Log.i(TAG, "processImage: fd costTime = " + (System.currentTimeMillis() - fdStartTime));
                        Log.i(TAG, "faceSize:" + faceInfoList.size());
                    }
                    long millis = System.currentTimeMillis();
                    /**
                     * 3.判断人脸的3D信息，排除半张脸
                     */
                    int faceProcessCode = ftEngine.process(bgr24, width, height, FaceEngine.CP_PAF_BGR24, faceInfoList, FaceEngine.ASF_FACE3DANGLE | FaceEngine.ASF_LIVENESS);
                    if (faceProcessCode == ErrorInfo.MOK) {
                        List<Face3DAngle> face3DAngleList = new ArrayList<>();
                        ftEngine.getFace3DAngle(face3DAngleList);
                        for (int i = 0; i < face3DAngleList.size(); i++) {
                            Face3DAngle face3DAngle = face3DAngleList.get(i);
                            if (face3DAngle.getYaw() > Constants.FACE_DEVIATION || face3DAngle.getYaw() < -Constants.FACE_DEVIATION) {
                                faceInfoList.remove(i);
                            }
                            Log.d(TAG, "Image:" + millis + ",Face3DAngle-" + i + ":Pitch:" + face3DAngle.getPitch() + ",Yaw:" + face3DAngle.getYaw());
                        }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tipTxt.setText("人脸数量：" + faceInfoList.size());
                        }
                    });
                    if (faceInfoList.size() > 0) {
                        String file = "/storage/sdcard";
                        if (!new File(file).exists())
                            file = Environment.getExternalStorageDirectory().getAbsolutePath();
                        File file1 = new File(file + "/ScreenShots/facedetect");
                        if (!file1.exists()) {
                            file1.mkdirs();
                        }
                        /**
                         * 绘制红色矩形框在图片上
                         */
                        Canvas canvas = new Canvas(cavBitmap);
                        Paint paint = new Paint();
                        paint.setColor(Color.RED);//红色边线
                        paint.setStyle(Paint.Style.STROKE);//不填充
                        paint.setStrokeWidth(1);  //线的宽度
                        for (FaceInfo faceInfo : faceInfoList) {
                            canvas.drawRect(faceInfo.getRect(), paint);
                        }
                        String filePath = file1.getAbsolutePath() + File.separator + millis + ".jpg";
                        fos1 = new FileOutputStream(filePath);
                        cavBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos1);
                        fos1.close();
//                        FileUtils.copy(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "temp.jpg",
//                                file1.getAbsolutePath() + File.separator + System.currentTimeMillis() + ".jpg");
//                        Log.d(TAG, "文件复制成功，路径：" + file1.getAbsolutePath() + File.separator + System.currentTimeMillis() + ".jpg");
                        String b64 = ImageUtils.ImageToBase64(filePath);
                        HttpUtils.saveFaceDetectRecord(b64, faceInfoList.size());
                    }
                    Thread.sleep(800);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    ;
}
