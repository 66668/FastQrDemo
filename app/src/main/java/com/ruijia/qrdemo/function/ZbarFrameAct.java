package com.ruijia.qrdemo.function;

import android.Manifest;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.ruijia.qrdemo.R;
import com.ruijia.qrdemo.persmission.PermissionHelper;
import com.ruijia.qrdemo.persmission.PermissionInterface;

import lib.ruijia.zbar.ZBarContinueView;
import lib.ruijia.zbar.qrcodecore.BarcodeType;
import lib.ruijia.zbar.qrodecontinue.ContinueQRCodeView;

/**
 * TODO 未做
 * <p>
 * 优化后，lib库zbar持续识别
 * <p>
 * zbar取帧拍照
 * <p>
 * 功能用途：用于比较不同设备的性能，判断是设备问题还是代码问题，导致的识别慢或者取帧慢问题
 * <p>
 * 结果保存在跟目录，保存一帧的data数据（txt保存），和一帧的图片（jpg保存），
 * 如果用于设备B，请将设别A下的两个数据拷贝到设备B根目录下
 */
public class ZbarFrameAct extends AppCompatActivity implements ContinueQRCodeView.Delegate {
    //权限相关
    private String[] permissionArray;
    PermissionHelper permissionHelper;

    //控件
    private ZBarContinueView mZBarView; //zbar
    private TextView tv_show;
    private long receiveTime;


    //=====================================识别流程控制==========================================

    /**
     * zbar极限速度 60~90ms
     */

    //ContinueQRCodeView.Delegate
    @Override
    public void onScanQRCodeSuccess(String resultStr) {
        Log.d("SJY", "扫描结果为：" + resultStr);
        Log.d("SJY", "极限耗时=" + (System.currentTimeMillis() - receiveTime) + "ms---长度=" + resultStr.length());
        tv_show.setText("极限耗时=" + (System.currentTimeMillis() - receiveTime) + "ms");
        receiveTime = System.currentTimeMillis();
    }

    //QRCodeView.Delegate
    @Override
    public void onScanQRCodeOpenCameraError() {
        Log.e("SJY", "QRCodeView.Delegate--ScanQRCodeOpenCameraError()");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_zbar_time2);
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
        //zbar
        tv_show = findViewById(R.id.tv_show);
        mZBarView = (ZBarContinueView) findViewById(R.id.zbarview);
        mZBarView.setDelegate(this);
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
        mZBarView.getScanBoxView().setOnlyDecodeScanBoxArea(false); // 仅识别扫描框中的码
//        mZBarView.startCamera(cameraId); // 打开前置摄像头开始预览，但是并未开始识别
        mZBarView.startSpot(); // 显示扫描框，并且延迟0.1秒后开始识别
    }

    @Override
    protected void onStart() {
        Log.d("SJY", "onStart");
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
    protected void onResume() {
        Log.d("SJY", "onResume");
        super.onResume();
        if (mZBarView != null) {
            //前置摄像头(不加显示后置)
            mZBarView.startCamera(); // 打开后置摄像头开始预览，但是并未开始识别
            mZBarView.setType(BarcodeType.ONLY_QR_CODE, null); // 只识别 QR_CODE
            mZBarView.getScanBoxView().setOnlyDecodeScanBoxArea(false); // 仅识别扫描框中的码
//            mZBarView.startCamera(cameraId); // 打开前置摄像头开始预览，但是并未开始识别
            mZBarView.startSpot(); // 显示扫描框，并且延迟0.1秒后开始识别
            //
            receiveTime = System.currentTimeMillis();
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

}
