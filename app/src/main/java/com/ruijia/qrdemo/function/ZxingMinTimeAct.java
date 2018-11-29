package com.ruijia.qrdemo.function;

import android.Manifest;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.zxing.ResultPoint;
import com.ruijia.qrdemo.R;
import com.ruijia.qrdemo.persmission.PermissionHelper;
import com.ruijia.qrdemo.persmission.PermissionInterface;

import java.util.List;

import lib.ruijia.zxing.scan.BarcodeCallback;
import lib.ruijia.zxing.scan.BarcodeResult;
import lib.ruijia.zxing.scan.DecoratedBarcodeView;
import lib.ruijia.zxing.scan.camera.CameraSettings;

/**
 * zxing 后置 连续识别,极限测试,单张二维码速度
 */
public class ZxingMinTimeAct extends AppCompatActivity {
    //权限相关
    private String[] permissionArray;
    PermissionHelper permissionHelper;

    private DecoratedBarcodeView barcodeView;
    CameraSettings settings = new CameraSettings();
    //    int cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;//
    int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;//
    private long receiveTime;


    //=====================================识别流程控制==========================================

    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            Log.d("SJY", "扫描结果为：" + result.getText());
            Log.d("SJY", "极限速度=" + (System.currentTimeMillis() - receiveTime));
            barcodeView.setStatusText("极限速度=" + (System.currentTimeMillis() - receiveTime));
            receiveTime = System.currentTimeMillis();

        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_zxing_time);
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
        barcodeView = (DecoratedBarcodeView) findViewById(R.id.zxing_barcode_scanner);
    }


    /**
     * 开始识别（其实布局绑定就已经识别，此处设置识别样式）
     */
    private void startPreview() {
        //前置摄像头(不加显示后置)
        settings = new CameraSettings();
        settings.setRequestedCameraId(cameraId);
        barcodeView.getBarcodeView().setCameraSettings(settings);
        barcodeView.decodeContinuous(callback);//持续识别
        barcodeView.resume();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (barcodeView != null) {
            //前置摄像头(不加显示后置)
            settings = new CameraSettings();
            settings.setRequestedCameraId(cameraId);
            barcodeView.getBarcodeView().setCameraSettings(settings);
            barcodeView.decodeContinuous(callback);
            barcodeView.resume();
            //
            receiveTime = System.currentTimeMillis();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (barcodeView != null) {
            //前置摄像头(不加显示后置)
            settings = new CameraSettings();
            settings.setRequestedCameraId(cameraId);
            barcodeView.getBarcodeView().setCameraSettings(settings);
            barcodeView.pause();
        }
    }

    @Override
    protected void onDestroy() {

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
