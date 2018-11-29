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
import com.ruijia.qrdemo.utils.ConvertUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.qqtheme.framework.picker.FilePicker;
import lib.ruijia.zbar.ZBarContinueView;
import lib.ruijia.zbar.qrcodecore.BarcodeType;
import lib.ruijia.zbar.qrodecontinue.ContinueQRCodeView;

/**
 * 优化后，zbar识别方式 开路识别 后置摄像头
 * 以txt文本读取内容为例/其他文件后续再集成
 * <p>
 * 类似TCP/IP协议，每一段传输数据有标记，接收端处理标记，ACK反馈给发送端，发送端将未识别的数据再发送过去。接收端接收后反馈给发送端 识别成功。
 * 发送端：标记：sjy+0000+0000+数据段
 */
public class ZbarOpenAndCloseAct extends AppCompatActivity implements View.OnClickListener, ContinueQRCodeView.Delegate {
    private static final long PSOTDELAY_TIME = 130;//发送间隔时间
    private static final long PSOTDELAY_TIME2 = 300;//缺失发送间隔时间
    //权限相关
    private String[] permissionArray;
    PermissionHelper permissionHelper;
    //控件
    private ZBarContinueView mZBarView; //zbar
    private ImageView img_result;
    private Button btn_select, btn_show;

    //相机
    int cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;//
    // int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;//

    //生成二维码使用
    private int size = 400;//200就够了，再大也没用

    //test数据
    private List<String> orgDatas = new ArrayList<>();//原始数据
    List<String> sendDatas = new ArrayList<>();//已处理的发送端数据
    List<String> sendDatas2 = new ArrayList<>();//缺失的数据；
    private Map<Integer, Integer> receiveFlagMap = new HashMap<>();//接收端的标记的集合,
    private List<Integer> backFlagList = new ArrayList<>();//发送端 返回缺失的标记,
    private Map<Integer, String> receveMap = new HashMap<>();//接收的数据暂时保存到map中，最终保存到receiveDatas
    private List<String> receiveDatas = new ArrayList<>();//接收的完整数据
    private StringBuffer lostBuffer = new StringBuffer();//缺失的数据,用/字符分开

    //操作流程控制
    private long totalSize;//接收文件大小
    private Handler handler;
    private String lastText;
    private long startTime;//发送开始时间
    private long overTime;//接收结束时间
    private long receiveTime;//摄像头一直识别，记录识别时间使用，如果识别时间超过MAX_TIME，自动结束识别流程
    private int sendTimes = 0;//发送次数，一次发送，到下一次再发送，为一次，
    private int receveSize = 0;//接收端 标记 总数据长度

    //发送端时间统计
    private long send_startTime;
    private long send_overTime;
    private boolean hasBack = false;//是否有缺失数据；

    //=====================================识别流程控制==========================================

    /**
     * zbar极限速度 150ms--200ms。
     */

    //QRCodeView.Delegate
    @Override
    public void onScanQRCodeSuccess(String resultStr) {
        Log.d("SJY", "扫描结果为：" + resultStr);
//        mZBarView.startSpot(); // 延迟x ms后开始识别

        //结果相同不处理
        if (TextUtils.isEmpty(resultStr) || resultStr.equals(lastText)) {
            Log.d("SJY", "重复扫描");
            return;
        }

        //接收端结束标记，进行数据统计
        if (resultStr.contains("识别完了")) {
            handOver(true);
            Log.d("SJY", "识别完了");
            return;
        } else if (resultStr.contains("rcvSuccess")) {//接收端 结束标记
            return;
        }
        //解析数据
        //（1）解析标记
        String flagStr = resultStr.substring(0, 11);
        //接收端
        if (flagStr.contains("snd")) {//发送端发送的数据

            String[] flagArray = flagStr.split("d");
            //继续排除乱码
            if (!(flagArray.length == 2)) {
                Log.d("SJY", "数据没有长度标记");
                return;
            }
            //处理标记
            String lenStr = flagStr.substring(3, 7);
            final String posStr = flagStr.substring(7, 11);
            receveSize = Integer.parseInt(lenStr);//标记 数据总长度
            final int pos = Integer.parseInt(posStr);
            //保存标记
            receiveFlagMap.put(pos, pos);

            //内容
            final String result = resultStr.substring(11);

            //扔到handler的异步中处理
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("SJY", "识别速度=" + (System.currentTimeMillis() - receiveTime));
                    receiveTime = System.currentTimeMillis();
                    //显示扫描str结果
                    lastText = result;
                    //震动
                    vibrate();
                    totalSize += result.length();
                    Log.d("SJY", "长度=" + result.length() + "--total=" + totalSize);
                    receveMap.put(pos, result);//map暂时保存数据
                }
            });

            //发送端
        } else if (flagStr.contains("rcv")) {//接收端返回的数据
            //返回的格式：rcv00000000+1/2/3
            String[] flagArray = flagStr.split("v");
            //继续排除乱码
            if (!(flagArray.length == 2)) {
                Log.d("SJY", "数据没有长度标记");
                return;
            }
            //内容
            final String result = resultStr.substring(11);
            String[] list = result.split("/");
            sendDatas2 = new ArrayList<>();
            hasBack = true;
            for (int i = 0; i < list.length; i++) {
                String lost = sendDatas.get(Integer.parseInt(list[i]));
                Log.d("SJY", "缺失位置=" + list[i] + "--缺失的数据=" + lost);
                sendDatas2.add(lost);
            }
            //发送缺失数据
            sendLostData();

        } else {
            Log.d("SJY", "数据没有任何标记");
            return;
        }

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
        send_startTime = System.currentTimeMillis();
        handler.postDelayed(sendtask, 3000);
    }

    /**
     * 发送缺失的数据（二次发送）
     */
    private void sendLostData() {
        sendTimes = 0;
        handler.postDelayed(sendLostTask, 3000);
    }

    //初次发送
    Runnable sendtask = new Runnable() {
        @Override
        public void run() {
            if (sendTimes < sendDatas.size()) {
                showBitmap(sendDatas.get(sendTimes));
                sendTimes++;
                handler.postDelayed(this, PSOTDELAY_TIME);
            } else {
                showBitmap("识别完了识别完了识别完了识别完了识别完了");
                send_overTime = System.currentTimeMillis();
                hasBack = false;
                if (!hasBack) {
                    Log.d("SJY", "发送端耗时：" + (send_overTime - send_startTime - 3000) + "ms--缺失返回标记--无");
                }
            }
        }
    };

    //发送缺失数据
    Runnable sendLostTask = new Runnable() {
        @Override
        public void run() {
            if (sendTimes < sendDatas2.size()) {
                showBitmap(sendDatas2.get(sendTimes));
                sendTimes++;
                handler.postDelayed(this, PSOTDELAY_TIME2);
            } else {
                showBitmap("识别完了识别完了识别完了识别完了识别完了");
                send_overTime = System.currentTimeMillis();
            }
        }
    };

    /**
     * 异常或正常结束，都走这一步，统计识别的内容
     *
     * @param isReceive true 接收端
     */
    private void handOver(boolean isReceive) {
        overTime = System.currentTimeMillis();
        //统计结果
        final StringBuffer buffer = new StringBuffer();
        if (isReceive) {
            Log.i("SJY", "数据大小" + totalSize + "B");
            lostBuffer = new StringBuffer();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //计算缺失的部分
                    backFlagList = new ArrayList<>();
                    for (int i = 0; i < receveSize; i++) {
                        if (receiveFlagMap.get(i) == null) {
                            Log.d("SJY", "缺失=" + i);
                            backFlagList.add(i);
                        }
                    }
                    if (backFlagList.size() > 0) {//有缺失数据
                        Log.d("SJY", "接收端--数据缺失:");
                        //
                        int count = 0;
                        for (int i = 0; i < backFlagList.size(); i++) {
                            buffer.append(backFlagList.get(i) + "").append("/");
                            count++;
                        }
                        buffer.deleteCharAt(buffer.toString().length() - 1);
                        //拼接数据
                        String backStr = "rcv" + ConvertUtils.int2String(receveSize) + ConvertUtils.int2String(count) + buffer.toString();
                        //数据返回通知
                        showBitmap(backStr);

                    } else {//没有缺失数据
                        Log.d("SJY", "接收端--数据完整");
                        showBitmap("rcvSuccess");
                        //拼接数据
                        for (int i = 0; i < receveSize; i++) {
                            String str = receveMap.get(i);
                            Log.d("SJY", "数据=" + str);
                            receiveDatas.add(str);
                        }
                    }

                }
            });
        } else {
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
        receveSize = 0;
        sendTimes = 0;
        receiveDatas = new ArrayList<>();
        orgDatas = new ArrayList<>();
        sendDatas = new ArrayList<>();
        btn_show.setVisibility(View.GONE);
        receiveFlagMap = new HashMap<>();
        backFlagList = new ArrayList<>();
        receveMap = new HashMap<>();
        //
        img_result.setImageBitmap(null);
        img_result.setBackground(ContextCompat.getDrawable(this, R.mipmap.ic_launcher));
    }


    //=====================================复写==========================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_zbar_open_close);
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
        mZBarView = (ZBarContinueView) findViewById(R.id.zbarview);
        mZBarView.setDelegate(this);
        //
        img_result = (ImageView) findViewById(R.id.barcodePreview);
        img_result.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //点击图片重新唤醒
                if (mZBarView != null) {
                    mZBarView.startSpot(); // 显示扫描框，并且延迟0.1秒后开始识别
                }
                initParams();
            }
        });

        //设置宽高
        size = 400;
        handler = new Handler();
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }


    /**
     * 开始识别（其实布局绑定就已经识别，此处设置识别样式）
     */
    private void startPreview() {
        //前置摄像头(不加显示后置)
        mZBarView.startCamera(); // 打开后置摄像头开始预览，但是并未开始识别
        mZBarView.setType(BarcodeType.ONLY_QR_CODE, null); // 只识别 QR_CODE
        mZBarView.getScanBoxView().setOnlyDecodeScanBoxArea(false); // 仅识别扫描框中的码
//        mZBarView.startCamera(cameraId); // 打开前置摄像头开始预览，但是并未开始识别
        mZBarView.startSpot(); // 显示扫描框，并且延迟0.1秒后开始识别
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mZBarView != null) {
            mZBarView.startCamera(); // 打开后置摄像头开始预览，但是并未开始识别
            mZBarView.getScanBoxView().setOnlyDecodeScanBoxArea(false); // 仅识别扫描框中的码
            mZBarView.setType(BarcodeType.ONLY_QR_CODE, null); // 只识别 QR_CODE
//            mZBarView.startCamera(cameraId); // 打开前置摄像头开始预览，但是并未开始识别
            mZBarView.startSpot(); // 显示扫描框，并且延迟0.1秒后开始识别
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
            mZBarView.getScanBoxView().setOnlyDecodeScanBoxArea(false); // 仅识别扫描框中的码
//            mZBarView.startCamera(cameraId); // 打开前置摄像头开始预览，但是并未开始识别
            mZBarView.startSpot(); // 显示扫描框，并且延迟0.1秒后开始识别
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
                    Toast.makeText(ZbarOpenAndCloseAct.this, "生成英文二维码失败", Toast.LENGTH_SHORT).show();
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
                    //添加标记，前四位是size标记，后四位是第几个标记
                    int size = orgDatas.size();
                    String strSize = ConvertUtils.int2String(size);//不会处理大于10000的size
                    for (int i = 0; i < size; i++) {
                        String pos = ConvertUtils.int2String(i);
                        //拼接数据-->格式：snd(发送标记)+0022(数据长度)+0001(第几个，从0开始)+数据段
                        sendDatas.add("snd" + strSize + pos + orgDatas.get(i)); //eg 00120001xxstr
                    }
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
                    Toast.makeText(ZbarOpenAndCloseAct.this, s, Toast.LENGTH_SHORT).show();
                    btn_show.setVisibility(View.GONE);
                    return;
                }
                Log.d("SJY", "---orgDatas.size()=" + orgDatas.size());
                btn_show.setVisibility(View.VISIBLE);
            }
        }.execute();
    }


}
