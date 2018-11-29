package com.ruijia.qrdemo.function;

import android.Manifest;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.ruijia.qrdemo.R;
import com.ruijia.qrdemo.persmission.PermissionHelper;
import com.ruijia.qrdemo.persmission.PermissionInterface;
import com.ruijia.qrdemo.utils.CodeUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import cn.qqtheme.framework.picker.FilePicker;
import lib.ruijia.zbar.ZBarView;
import lib.ruijia.zbar.qrcodecore.BarcodeType;
import lib.ruijia.zbar.qrcodecore.QRCodeView;

/**
 * zbar识别方式 开路识别 后置摄像头
 * <p>
 * 开路：只负责发送，或只负责识别，通过标记位决定 数据是否发送或结束。
 * <p>
 * 闭路：接收端：识别完一条数据，返回二维码告知更新数据，等待识别新数据。
 * 发送端：触发按钮，发送二维码数据，发送端等待识别端的二维码（返回 更新数据的通知），接收后再发送新数据
 * <p>
 * 读取txt文件
 * https://github.com/journeyapps/zxing-android-embedded
 * 使用功能：持续识别+前置摄像头+margin
 */
public class ZbarOpenAct extends AppCompatActivity implements View.OnClickListener, QRCodeView.Delegate {
    //权限相关
    private String[] permissionArray;
    PermissionHelper permissionHelper;

    //控件
    private ZBarView mZBarView; //zbar
    private ImageView img_result;
    private Button btn_select, btn_show;

    //相机
    int cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;//
    // int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;//

    //生成二维码使用
    private int size = 400;//200就够了，再大也没用

    //test数据
    private List<String> orgDatas = new ArrayList<>();//发送数据
    private List<String> receiveDatas = new ArrayList<>();//接收数据,用于保存到txt中
    private int receiveLength = 0;//发送端传过来的数据长度，用于比较接收端数据是否有缺失。
    private File saveFile;//识别内容保存到txt中

    //操作流程控制
    private long totalSize;//接收文件大小
    private Handler handler;
    private String lastText;
    private long startTime;//发送开始时间
    private long overTime;//接收结束时间
    private long receiveTime;//摄像头一直识别，记录识别时间使用，如果识别时间超过MAX_TIME，自动结束识别流程
    private static final long TIME_SIZE = 1000;//转s单位
    private static final long PSOTDELAY_TIME = 800;//发送间隔时间
    private int sendTimes = 0;//发送次数，一次发送，到下一次再发送，为一次，

    //=====================================识别流程控制==========================================

    /**
     * zbar极限速度 140ms--200ms。
     */

    //QRCodeView.Delegate
    @Override
    public void onScanQRCodeSuccess(String resultStr) {
        Log.d("SJY", "扫描结果为：" + resultStr);
        mZBarView.startSpot(); // 延迟0.1秒后开始识别

        //结果相同不处理
        if (TextUtils.isEmpty(resultStr) || resultStr.equals(lastText)) {
            Log.d("SJY", "重复扫描");
            if (resultStr != null && resultStr.contains("识别完了")) {
                showBitmap("识别完了");
                if (orgDatas == null || orgDatas.size() <= 0) {//接收端识别
                    handOver(true);
                } else {
                    handOver(false);
                }
                return;
            }
            return;
        }
        //
        final String result = resultStr;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("SJY", "识别速度=" + (System.currentTimeMillis() - receiveTime)+"--长度="+result.length());
                receiveTime = System.currentTimeMillis();
                //显示扫描str结果
                lastText = result;

                //判断发送端/接收端
                if (orgDatas == null || orgDatas.size() <= 0) {//接收端识别
                    vibrate();
                    if (result.contains("listSize=")) {//开路识别标记，数据开始识别的地方:listSize=12
                        String[] arr = result.split("=");
                        receiveLength = Integer.parseInt(arr[1]);//获取数据大小
                        //接收端时间统计--开始
                        startTime = System.currentTimeMillis();
                        receiveTime = System.currentTimeMillis();
                    } else {
                        totalSize += result.length();
                        Log.d("SJY", "size=" + result.length() + "--total=" + totalSize);
                        receiveDatas.add(result);//保存接收数据，
                    }

                } else {//发送端识别
                    Log.d("SJY", "我是发送端，zBarOpenAct时，不识别数据");
                }
            }
        });
    }

    //QRCodeView.Delegate
    @Override
    public void onScanQRCodeOpenCameraError() {
        Log.e("SJY", "QRCodeView.Delegate--ScanQRCodeOpenCameraError()");
    }

    /**
     * 发送二维码
     */
    private void startShow() {
        //发送端时间统计
        startTime = System.currentTimeMillis();
        receiveTime = System.currentTimeMillis();
        sendTimes = 0;
        handler.postDelayed(sendtask, 3000);
    }

    Runnable sendtask = new Runnable() {
        @Override
        public void run() {
            if (sendTimes < orgDatas.size()) {
                showBitmap(orgDatas.get(sendTimes));
                sendTimes++;
                handler.postDelayed(this, PSOTDELAY_TIME);
            } else {
                showBitmap("识别完了");
                handOver(false);
            }
        }
    };

    /**
     * 异常或正常结束，都走这一步，结束识别,但是识别端不会关闭二维码预览，只会清空数据，等待再次识别新数据
     *
     * @param isReceive true 接收端
     */
    private void handOver(boolean isReceive) {
        overTime = System.currentTimeMillis();
        //统计结果
        StringBuffer buffer = new StringBuffer();
        long time = (overTime - startTime);
        if (isReceive) {
            Log.i("SJY", "接收端统计：总耗时 = " + time + "ms");
            Log.i("SJY", "发送数据长度=" + receiveLength + "---接收实际长度=" + receiveDatas.size() + "--数据大小" + totalSize + "B");
            if (receiveDatas != null && receiveDatas.size() > 0) {
                for (String str : receiveDatas) {
                    buffer.append(str).append("\n");
                }
            }

        } else {
            buffer.append("发送端统计：总耗时 = " + time + "ms").append("\n")
                    .append("发送list.size=" + orgDatas.size() + "次").append("\n")
                    .append("发送次数 = " + sendTimes + "次");
            Log.d("SJY", buffer.toString());
        }

    }

    /**
     * 重新初始化流程控制参数
     */
    private void initParams() {
        startTime = 0;
        overTime = 0;
        receiveTime = 0;
        sendTimes = 0;
        receiveDatas = new ArrayList<>();
        orgDatas = new ArrayList<>();
        btn_show.setVisibility(View.GONE);
        saveFile = null;
        img_result.setBackground(ContextCompat.getDrawable(this, R.mipmap.ic_launcher));
    }


    //=====================================复写==========================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_zbar_open);
        initView();
        //必要权限
        permissionArray = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };
        toStartPermission();
    }

    /**
     * 初始化控件
     */
    private void initView() {
        //控件
        btn_select = findViewById(R.id.btn_select);
        btn_show = findViewById(R.id.btn_show);
        btn_show.setVisibility(View.GONE);
        btn_show.setOnClickListener(this);
        btn_select.setOnClickListener(this);
        //zbar
        mZBarView = (ZBarView) findViewById(R.id.zbarview);
        mZBarView.setDelegate(this);
        //
        img_result = (ImageView) findViewById(R.id.barcodePreview);
        img_result.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //点击图片重新唤醒
                if (mZBarView != null) {
                    mZBarView.startSpotAndShowRect(); // 显示扫描框，并且延迟0.1秒后开始识别
                }
                initParams();
            }
        });

        //设置宽高
        size = 400;
        //保存识别内容-->根目录/save.txt
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "save.txt";
        saveFile = new File(path);
        if (saveFile != null) {
            saveFile.delete();
        }
        saveFile = new File(path);
        handler = new Handler();
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(200);
    }


    /**
     * 开始识别（其实布局绑定就已经识别，此处设置识别样式）
     */
    private void startPreview() {
        //前置摄像头(不加显示后置)
        mZBarView.startCamera(); // 打开后置摄像头开始预览，但是并未开始识别
        mZBarView.setType(BarcodeType.ONLY_QR_CODE, null); // 只识别 QR_CODE
        mZBarView.getScanBoxView().setOnlyDecodeScanBoxArea(true); // 仅识别扫描框中的码
//        mZBarView.startCamera(cameraId); // 打开前置摄像头开始预览，但是并未开始识别
        mZBarView.startSpotAndShowRect(); // 显示扫描框，并且延迟0.1秒后开始识别
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mZBarView != null) {
            mZBarView.startCamera(); // 打开后置摄像头开始预览，但是并未开始识别
            mZBarView.getScanBoxView().setOnlyDecodeScanBoxArea(true); // 仅识别扫描框中的码
            mZBarView.setType(BarcodeType.ONLY_QR_CODE, null); // 只识别 QR_CODE
//            mZBarView.startCamera(cameraId); // 打开前置摄像头开始预览，但是并未开始识别
            mZBarView.startSpotAndShowRect(); // 显示扫描框，并且延迟0.1秒后开始识别
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_select:
                //文件选择器
                FilePicker picker = new FilePicker(this, FilePicker.FILE);
                picker.setShowHideDir(false);
                picker.setRootPath(Environment.getExternalStorageDirectory().getAbsolutePath());
                picker.setAllowExtensions(new String[]{".txt"});
                picker.setFileIcon(getResources().getDrawable(android.R.drawable.ic_menu_agenda));
                picker.setFolderIcon(getResources().getDrawable(android.R.drawable.ic_menu_upload_you_tube));
                //picker.setArrowIcon(getResources().getDrawable(android.R.drawable.arrow_down_float));
                picker.setOnFilePickListener(new FilePicker.OnFilePickListener() {
                    @Override
                    public void onFilePicked(String currentPath) {
                        Log.d("SJY", "currentPath=" + currentPath);
                        initParams();
                        if (!TextUtils.isEmpty(currentPath)) {
                            getRealFile(currentPath);
                        }
                    }
                });
                picker.show();
                break;
            case R.id.btn_show:
                //显示二维码
                startShow();
                break;
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mZBarView != null) {
            //前置摄像头(不加显示后置)
            mZBarView.startCamera(); // 打开后置摄像头开始预览，但是并未开始识别
            mZBarView.setType(BarcodeType.ONLY_QR_CODE, null); // 只识别 QR_CODE
            mZBarView.getScanBoxView().setOnlyDecodeScanBoxArea(true); // 仅识别扫描框中的码
//            mZBarView.startCamera(cameraId); // 打开前置摄像头开始预览，但是并未开始识别
            mZBarView.startSpotAndShowRect(); // 显示扫描框，并且延迟0.1秒后开始识别
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mZBarView != null) {
            mZBarView.stopCamera(); // 关闭摄像头预览，并且隐藏扫描框
        }
    }

    @Override
    protected void onStop() {
        mZBarView.stopCamera(); // 关闭摄像头预览，并且隐藏扫描框
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mZBarView.onDestroy(); // 销毁二维码扫描控件
        handler.removeCallbacks(sendtask);
        super.onDestroy();
    }

    private void toStartPermission() {
        permissionHelper = new PermissionHelper(this, new PermissionInterface() {
            @Override
            public int getPermissionsRequestCode() {
                //设置权限请求requestCode，只有不跟onRequestPermissionsResult方法中的其他请求码冲突即可。
                return 10002;
            }

            @Override
            public String[] getPermissions() {
                //设置该界面所需的全部权限
                return permissionArray;
            }

            @Override
            public void requestPermissionsSuccess() {
                //权限请求用户已经全部允许
                startPreview();
            }

            @Override
            public void requestPermissionsFail() {

            }

        });
        //发起调用：
        permissionHelper.requestPermissions();
    }

    //权限回调处理
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (permissionHelper.requestPermissionsResult(requestCode, permissions, grantResults)) {
            //权限请求已处理，不用再处理
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //=====================================private处理==========================================

    /**
     * 将绝对路径转成文件
     *
     * @param absolutePath
     */
    private void getRealFile(String absolutePath) {
        File file = new File(absolutePath);
        if (file == null || !file.exists()) {
            Toast.makeText(this, "没有txt文件", Toast.LENGTH_SHORT).show();
        } else {
            btn_show.setVisibility(View.VISIBLE);
            initData(file);
        }
    }

    /**
     * 创建并显示
     *
     * @param content 不为空
     * @return
     */
    private void showBitmap(final String content) {
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... voids) {
                return CodeUtils.createByMultiFormatWriter(content, size);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    img_result.setImageBitmap(bitmap);
                } else {
                    Log.e("SJY", "生成英文二维码失败");
                    Toast.makeText(ZbarOpenAct.this, "生成英文二维码失败", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }


    /**
     * 创建数据
     * <p>
     * 将txt转成List<String>
     *
     * @param file
     */
    @SuppressLint("StaticFieldLeak")
    private void initData(final File file) {
        //
        if (!file.getName().contains("txt")) {
            Toast.makeText(this, file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return;
        }
        //
        orgDatas = new ArrayList<>();
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                String linStr;
                try {
                    //1
                    FileInputStream inputStream = new FileInputStream(file);
                    long length = file.length();
                    Log.d("SJY", "文件大小=" + length + "B");
                    InputStreamReader inputReader = new InputStreamReader(inputStream, "utf-8");
                    BufferedReader buffReader = new BufferedReader(inputReader);
                    //2
                    while ((linStr = buffReader.readLine()) != null) {//读一行
                        orgDatas.add(linStr);
                    }

                    int size = orgDatas.size();
                    //添加标记位 首尾各一个
                    orgDatas.add(0, "listSize=" + size);//首位标记
                    orgDatas.add("识别完了");//尾标记

                    //打印 可删除
                    StringBuffer buffer = new StringBuffer();
                    for (String str : orgDatas) {
                        buffer.append(str).append("\n");
                    }
                    Log.d("SJY", "+orgDatas.size()=" + orgDatas.size() + "--str=" + buffer.toString());
                    //
                    inputStream.close();
                    inputReader.close();
                    buffReader.close();
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    String s = "无法生成原始数据，无法转成二维码";
                    return s;
                }
            }

            @Override
            protected void onPostExecute(String s) {
                if (s != null) {
                    Toast.makeText(ZbarOpenAct.this, s, Toast.LENGTH_SHORT).show();
                    btn_show.setVisibility(View.GONE);
                    return;
                }
                Log.d("SJY", "---orgDatas.size()=" + orgDatas.size());
                btn_show.setVisibility(View.VISIBLE);
            }
        }.execute();


    }


}
