package com.ruijia.qrdemo.function;

import android.Manifest;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.ruijia.qrdemo.R;
import com.ruijia.qrdemo.persmission.PermissionHelper;
import com.ruijia.qrdemo.persmission.PermissionInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import cn.qqtheme.framework.picker.FilePicker;
import lib.ruijia.zbar.ZBarView;
import lib.ruijia.zbar.qrcodecore.BarcodeType;
import lib.ruijia.zbar.qrcodecore.QRCodeView;

/**
 * zbar识别方式（只有后置）
 * 读取txt文件
 * https://github.com/journeyapps/zxing-android-embedded
 * 使用功能：持续识别+前置摄像头+margin
 */
public class ZbarCloseAct extends AppCompatActivity implements View.OnClickListener, QRCodeView.Delegate {
    //权限相关
    private String[] permissionArray;
    PermissionHelper permissionHelper;

    //zbar
    private ZBarView mZBarView;

    //相机
        int cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;//
//    int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;//

    private String lastText;
    private ImageView img_result;
    private Button btn_select, btn_show;
    //生成二维码使用
    private Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    private int size = 8000;

    //test数据
    List<String> orgDatas = new ArrayList<>();//发送数据
    List<String> receiveDatas = new ArrayList<>();//接收数据,用于保存到txt中
    File saveFile;//识别内容保存到txt中

    //操作流程控制
    long startTime;//发送开始时间
    long overTime;//接收结束时间
    long receiveTime;//摄像头一直识别，记录识别时间使用，如果识别时间超过MAX_TIME，自动结束识别流程
    public static final long TIME_SIZE = 1000;//转s单位
    public static final long MAX_TIME = 1000 * 50;//最大一个发送+识别循环时间
    int sendTimes = 0;//发送次数，一次发送，到下一次再发送，为一次，
    int receiveTimes = 0;//接收次数
    String sendStr = "请发送新数据";//接收端接收数据，用该参数告诉发送端重新发送数据
    String a = "BitmapcreateBitmappickersetCameraSettingssettingsetFileIcongetResourcesgetDrawableandroidRdrawabBitmapcreateBitmappickersetCameraSettingssettingsetFileIcongetResourcesgetDrawableandroidRdrawableimenuagendapickersetOnFilePickListenernewFilePickerOnFilePickListenerbarcodeViewgetBarcodeViewssettingssetRequestedCameraIdCameraCameraInfoCAMERAFACINGFRONpermissionHelpernewPermissionHelperthisnewPermissionInterfaceonRequestPermissionsResultinrequestCodeonNullStringpermissionsNonNullintgrantResultsuestPermissionsResultrequestCodepermissionsgrantResultstrixnewMultiFormatWriterencodecontentBarcodeFormatQRCODEsizesizehintssespermissionandroidnameandroidpermissionREADEXTERNALSTORAGEusesfeatureandroidnameandroidhardwarewifiandoidrequiredfalseactionandroidnameandroidintentactionMAINtWriterencodecontentBarcodeFormatQRCODEizesizehintskldkjafijfoaOdsjaoijdaoivjodvjaVaovjaodbhalvknovnpbvnaobdndasbvpnLkanlkbvnaobnaobvjhavodnasVlandkvnhabvonabdojknabvojanbVknalkvnaobnadokbvnaobvknalvknadbokanbKvandkvnalkvndalkvnaohjovnaoivnavnoaivjpajnvpVlnaovndlknvalknb";

    //=====================================识别流程控制==========================================

    /**
     * 识别流程说明：
     * （1）选择文件读取内容，文件只支持txt格式
     * <p>
     * （2）识别list数据最后都加了"识别完了"内容做标记
     * <p>
     * （3）
     */

    //QRCodeView.Delegate
    @Override
    public void onScanQRCodeSuccess(String result) {
        Log.d("SJY", "扫描结果为：" + result);
        //结果相同不处理
        if (result == null || result.equals(lastText)) {
            //长时间无识别判断
            receiveTime = System.currentTimeMillis();
            //MAX_TIMEms无结果，结束
            if ((receiveTime - overTime) > MAX_TIME) {
                handOver(true);
            }
            return;
        }
        //显示扫描str结果
        lastText = result;
        vibrate();
        mZBarView.startSpot(); // 延迟0.1秒后开始识别
        //判断发送端/接收端
        if (orgDatas == null || orgDatas.size() <= 0) {//接收端识别
            //打印
            Log.d("SJY", "接收端识别--等待下一次接收的时间=" + (System.currentTimeMillis() - overTime));
            receiveTimes++;
            overTime = System.currentTimeMillis();
            if (result.equals("识别完了")) {

                showBitmap("识别完了");
                handOver(true);
                return;
            }
            receiveDatas.add(result);//保存接收数据，
            //告诉发送端，发送新数据
            if (sendStr.equals("请发送新数据")) {
                sendStr = "请重新发送新数据";
                showBitmap(sendStr);
            } else if (sendStr.equals("请重新发送新数据")) {
                sendStr = "请发送新数据";
                showBitmap(sendStr);
            }

        } else {//发送端识别
            Log.d("SJY", "发送端识别--等待下一次接收的时间=" + (System.currentTimeMillis() - overTime));
            overTime = System.currentTimeMillis();
            //发送新数据
            if (result.equals("请发送新数据") || result.equals("请重新发送新数据")) {
                if (sendTimes < orgDatas.size()) {
                    showBitmap(orgDatas.get(sendTimes));
                } else {
                    Log.d("SJY", "统计sendTimes超过数据=" + sendTimes);
                }
                sendTimes++;
            } else if (result.equals("识别完了")) {
                handOver(false);
            } else {
                Log.d("SJY", "识别自己的二维码，错误！");
                handOver(false);
            }

        }

    }

    //QRCodeView.Delegate
    @Override
    public void onScanQRCodeOpenCameraError() {

    }

    /**
     * 发送二维码
     */
    private void startShow() {
        startTime = System.currentTimeMillis();
        if (sendTimes < orgDatas.size()) {
            showBitmap(a);
//            showBitmap(orgDatas.get(sendTimes));
        } else {
            Log.d("SJY", "统计sendTimes超过数据=" + sendTimes);
        }
        sendTimes++;
    }

    /**
     * 异常或正常结束，都走这一步，结束识别,但是识别端不会关闭二维码预览，只会清空数据，等待再次识别新数据
     *
     * @param isReceive true 接收端
     */
    private void handOver(boolean isReceive) {
        //统计结果
        StringBuffer buffer = new StringBuffer();
        if (isReceive) {
            buffer.append("接收端无统计");
            if (receiveDatas != null && receiveDatas.size() > 0) {
                for (String str : receiveDatas) {
                    buffer.append(str).append("\n");
                }
            }
            Log.d("SJY", buffer.toString());
            initParams();
        } else {
            long time = (overTime - startTime);
            buffer.append("发送端统计：总耗时 = " + time + "ms").append("\n")
                    .append("发送list.size=" + orgDatas.size() + "次").append("\n")
                    .append("发送次数 = " + sendTimes + "次");
            Log.d("SJY", buffer.toString());
            initParams();
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
        receiveTimes = 0;
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
        setContentView(R.layout.act_zbar);
        initView();
        Log.d("SJY", "字符长度=" + a.length());
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

        //生成二维码使用
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 0);
        //设置宽高
        size = 400;
        //保存识别内容-->根目录/save.txt
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "save.txt";
        saveFile = new File(path);
        if (saveFile != null) {
            saveFile.delete();
        }
        saveFile = new File(path);
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
        if(mZBarView!=null){
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
                picker.setAllowExtensions(new String[]{".txt"});
                picker.setRootPath(Environment.getExternalStorageDirectory().getAbsolutePath());
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
                return createBitmap(content);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    img_result.setImageBitmap(bitmap);
                } else {
                    Log.e("SJY", "生成英文二维码失败");
                    Toast.makeText(ZbarCloseAct.this, "生成英文二维码失败", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    /**
     * 最大容量qrcode生成
     *
     * @param content
     * @return
     */
    private Bitmap createBitmap(String content) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            int[] pixels = new int[size * size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    if (matrix.get(x, y)) {
                        pixels[y * size + x] = 0xff000000;//黑
                    } else {
                        pixels[y * size + x] = 0xffffffff;//白
                    }
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("SJY", e.toString());
            return null;
        }
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
                    orgDatas.add("识别完了");
                    //打印
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
                    Toast.makeText(ZbarCloseAct.this, s, Toast.LENGTH_SHORT).show();
                    btn_show.setVisibility(View.GONE);
                    return;
                }
                Log.d("SJY", "---orgDatas.size()=" + orgDatas.size());
                btn_show.setVisibility(View.VISIBLE);
            }
        }.execute();


    }


}
