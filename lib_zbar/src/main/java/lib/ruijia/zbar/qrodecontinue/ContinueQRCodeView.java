package lib.ruijia.zbar.qrodecontinue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import lib.ruijia.zbar.R;
import lib.ruijia.zbar.qrcodecore.BarcodeType;

/**
 * 自定义控件，封装相机预览和取帧识别操作
 */
public abstract class ContinueQRCodeView extends RelativeLayout implements Camera.PreviewCallback {
    /**
     * TODO 识别的最小延时，避免相机还未初始化完成
     */
    public static final int SPOT_MIN_DELAY = 100;//100
    protected Camera mCamera;
    protected ContinueCameraPreview mCameraPreview;
    protected ContinueScanBoxView mScanBoxView;//扫描框
    protected Delegate mDelegate;
    protected Handler mHandler;
    protected ContinueProcessDataTask mProcessDataTask;
    //    protected int mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    protected int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;//Camera.CameraInfo.CAMERA_FACING_BACK /Camera.CameraInfo.CAMERA_FACING_BACK
    private PointF[] mLocationPoints;
    private Paint mPaint;
    protected BarcodeType mBarcodeType = BarcodeType.HIGH_FREQUENCY;
    private static long sLastPreviewFrameTime = 0;

    public ContinueQRCodeView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);

    }

    public ContinueQRCodeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Log.e("SJY", "ContinueQRCodeView--xml初始化");
        mHandler = new Handler();
        initView(context, attrs);
        setupReader();
    }

    private void initView(Context context, AttributeSet attrs) {
        Log.e("SJY", "打开预览");
        mCameraPreview = new ContinueCameraPreview(context);

        mScanBoxView = new ContinueScanBoxView(context);
        mScanBoxView.init(this, attrs);
        mCameraPreview.setId(R.id.bgaqrcode_camera_preview);
        addView(mCameraPreview);
        LayoutParams layoutParams = new LayoutParams(context, attrs);
        layoutParams.addRule(RelativeLayout.ALIGN_TOP, mCameraPreview.getId());
        layoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, mCameraPreview.getId());
        addView(mScanBoxView, layoutParams);

        mPaint = new Paint();
        mPaint.setColor(getScanBoxView().getCornerColor());
        mPaint.setStyle(Paint.Style.FILL);
    }

    protected abstract void setupReader();

    /**
     * 设置扫描二维码的代理
     *
     * @param delegate 扫描二维码的代理
     */
    public void setDelegate(Delegate delegate) {
        mDelegate = delegate;
    }

    public ContinueCameraPreview getCameraPreview() {
        return mCameraPreview;
    }

    public ContinueScanBoxView getScanBoxView() {
        return mScanBoxView;
    }

    /**
     * 显示扫描框
     */
    public void showScanRect() {
        if (mScanBoxView != null) {
            mScanBoxView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 隐藏扫描框
     */
    public void hiddenScanRect() {
        if (mScanBoxView != null) {
            mScanBoxView.setVisibility(View.GONE);
        }
    }

    /**
     * 打开后置摄像头开始预览，但是并未开始识别
     */
    public void startCamera() {
        startCamera(mCameraId);
    }

    /**
     * 打开指定摄像头开始预览，但是并未开始识别
     */
    public void startCamera(int cameraFacing) {
        //排除 onResume onCreate 两次初始化的调用，保证打开界面后，只调用一次
        if (mCamera != null) {
            return;
        }
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        //正常手机调用
        for (int cameraId = 0; cameraId < Camera.getNumberOfCameras(); cameraId++) {
            Camera.getCameraInfo(cameraId, cameraInfo);
            Log.d("SJY", "cameraInfo.facing=" + cameraInfo.facing + "--cameraFacing=" + cameraFacing);

            if (cameraInfo.facing == cameraFacing) {
                startCameraById(cameraId);
                break;
            } else {
                // TODO 对于非手机的设备，会走这一步，原因是定制设备的camera不标准，需特殊设置，所以添加了这个方法
                startCameraById(cameraId);
                Log.e("SJY", "ContinueQRCodeView--startCamera(int cameraFacing)--打开相机预览失败");
            }
        }
    }

    private void startCameraById(int cameraId) {
        try {
            Log.d("SJY", "ContinueQRCodeView--startCameraById");
            mCameraId = cameraId;
            mCamera = Camera.open(cameraId);
            mCameraPreview.setCamera(mCamera);
        } catch (Exception e) {
            e.printStackTrace();
            if (mDelegate != null) {
                mDelegate.onScanQRCodeOpenCameraError();
            }
        }
    }

    /**
     * 关闭摄像头预览，并且隐藏扫描框
     */
    public void stopCamera() {
        try {
            stopSpotAndHiddenRect();
            if (mCamera != null) {
                mCameraPreview.stopCameraPreview();
                mCameraPreview.setCamera(null);
                mCamera.release();
                mCamera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 延迟0.1秒后开始识别
     */
    public void startSpot() {
        startSpotDelay(SPOT_MIN_DELAY);
    }

    /**
     * 延迟delay毫秒后开始识别
     */
    public void startSpotDelay(int delay) {
        // 至少延时 SPOT_MIN_DELAY 毫秒，避免相机还未初始化完成
        delay = Math.max(delay, SPOT_MIN_DELAY);

        startCamera();
        // 开始前先移除之前的任务
        if (mHandler != null) {
            mHandler.removeCallbacks(mOneShotPreviewCallbackTask);
            mHandler.postDelayed(mOneShotPreviewCallbackTask, delay);
        }
    }

    /**
     * 停止识别
     */
    public void stopSpot() {

        if (mProcessDataTask != null) {
            mProcessDataTask.cancelTask();
            mProcessDataTask = null;
        }

        if (mCamera != null) {
            try {
                mCamera.setOneShotPreviewCallback(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mHandler != null) {
            mHandler.removeCallbacks(mOneShotPreviewCallbackTask);
        }
    }

    /**
     * 停止识别，并且隐藏扫描框
     */
    public void stopSpotAndHiddenRect() {
        stopSpot();
        hiddenScanRect();
    }

    /**
     * 显示扫描框，并且延迟0.1秒后开始识别
     */
    public void startSpotAndShowRect() {
        startSpot();
        showScanRect();
    }

    /**
     * 打开闪光灯
     */
    public void openFlashlight() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mCameraPreview.openFlashlight();
            }
        }, mCameraPreview.isPreviewing() ? 0 : 500);
    }

    /**
     * 关闭闪光灯
     */
    public void closeFlashlight() {
        mCameraPreview.closeFlashlight();
    }

    /**
     * 销毁二维码扫描控件
     */
    public void onDestroy() {
        stopCamera();
        mHandler = null;
        mDelegate = null;
        mOneShotPreviewCallbackTask = null;
    }

    /**
     * 切换成扫描条码样式
     */
    public void changeToScanBarcodeStyle() {
        if (!mScanBoxView.getIsBarcode()) {
            mScanBoxView.setIsBarcode(true);
        }
    }

    /**
     * 切换成扫描二维码样式
     */
    public void changeToScanQRCodeStyle() {
        if (mScanBoxView.getIsBarcode()) {
            mScanBoxView.setIsBarcode(false);
        }
    }

    /**
     * 当前是否为条码扫描样式
     */
    public boolean getIsScanBarcodeStyle() {
        return mScanBoxView.getIsBarcode();
    }


    private ContinueScanResult processData(final byte[] mData) {
        if (mData == null) {
            return null;
        }

        int width = 0;
        int height = 0;
        byte[] data = mData;
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size size = parameters.getPreviewSize();
            width = size.width;
            height = size.height;

            if (ContinueBGAQRCodeUtil.isPortrait(getContext())) {
                data = new byte[mData.length];
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        data[x * height + height - y - 1] = mData[x + y * width];
                    }
                }
                int tmp = width;
                width = height;
                height = tmp;
            }

            return processData(data, width, height, false);
        } catch (Exception e1) {
            e1.printStackTrace();
            try {
                if (width != 0 && height != 0) {
                    ContinueBGAQRCodeUtil.d("识别失败重试");
                    return processData(data, width, height, true);
                } else {
                    return null;
                }
            } catch (Exception e2) {
                e2.printStackTrace();
                return null;
            }
        }
    }

    /**
     * TODO
     * <p>
     * 相机帧预览
     *
     * @param data
     * @param camera
     */
    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        if (ContinueBGAQRCodeUtil.isDebug()) {
            ContinueBGAQRCodeUtil.d("两次 onPreviewFrame 时间间隔：" + (System.currentTimeMillis() - sLastPreviewFrameTime));
            sLastPreviewFrameTime = System.currentTimeMillis();
        }
        //持续识别
        if (mCamera != null) {
            mCamera.setOneShotPreviewCallback(ContinueQRCodeView.this);
        }

        if (mHandler != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ContinueScanResult scanResult = processData(data);
                    onPostParseData(scanResult);
                }
            });
        }

//        if ((mProcessDataTask != null && (mProcessDataTask.getStatus() == AsyncTask.Status.PENDING
//                || mProcessDataTask.getStatus() == AsyncTask.Status.RUNNING))) {
//            return;
//        }
//
//        mProcessDataTask = new ContinueProcessDataTask(camera, data, this, ContinueBGAQRCodeUtil.isPortrait(getContext())).perform();
    }


    protected abstract ContinueScanResult processData(byte[] data, int width, int height, boolean isRetry);

    protected abstract ContinueScanResult processBitmapData(Bitmap bitmap);

    /**
     * TODO 处理结果 继续预览还是 返回结果
     *
     * @param scanResult
     */
    void onPostParseData(ContinueScanResult scanResult) {

        String result = scanResult == null ? null : scanResult.result;
        if (TextUtils.isEmpty(result)) {
            try {
                //持续识别
                if (mCamera != null) {
                    mCamera.setOneShotPreviewCallback(ContinueQRCodeView.this);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                if (mDelegate != null) {
                    mDelegate.onScanQRCodeSuccess(result);
                    //持续识别
                    if (mCamera != null) {
                        mCamera.setOneShotPreviewCallback(ContinueQRCodeView.this);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void onPostParseBitmapOrPicture(ContinueScanResult scanResult) {
        if (mDelegate != null) {
            String result = scanResult == null ? null : scanResult.result;
            mDelegate.onScanQRCodeSuccess(result);
        }
    }

    private Runnable mOneShotPreviewCallbackTask = new Runnable() {
        @Override
        public void run() {
            if (mCamera != null) {
                try {
                    mCamera.setOneShotPreviewCallback(ContinueQRCodeView.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public void onScanBoxRectChanged(Rect rect) {
        mCameraPreview.onScanBoxRectChanged(rect);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);


        if (!isShowLocationPoint() || mLocationPoints == null) {
            return;
        }

        for (PointF pointF : mLocationPoints) {
            canvas.drawCircle(pointF.x, pointF.y, 10, mPaint);
        }
        mLocationPoints = null;
        postInvalidateDelayed(2000);
    }

    /**
     * 是否显示定位点
     */
    protected boolean isShowLocationPoint() {
        return mScanBoxView != null && mScanBoxView.isShowLocationPoint();
    }

    protected void transformToViewCoordinates(final PointF[] pointArr, final Rect scanBoxAreaRect) {
        if (pointArr == null || pointArr.length == 0) {
            return;
        }

        new Thread() {
            @Override
            public void run() {
                try {
                    // 不管横屏还是竖屏，size.width 大于 size.height
                    Camera.Size size = mCamera.getParameters().getPreviewSize();
                    boolean isMirrorPreview = mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT;
                    int statusBarHeight = ContinueBGAQRCodeUtil.getStatusBarHeight(getContext());

                    PointF[] transformedPoints = new PointF[pointArr.length];
                    int index = 0;
                    for (PointF qrPoint : pointArr) {
                        transformedPoints[index] = transform(qrPoint.x, qrPoint.y, size.width, size.height, isMirrorPreview, statusBarHeight, scanBoxAreaRect);
                        index++;
                    }
                    mLocationPoints = transformedPoints;
                    postInvalidate();
                } catch (Exception e) {
                    mLocationPoints = null;
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private PointF transform(float originX, float originY, float cameraPreviewWidth, float cameraPreviewHeight, boolean isMirrorPreview, int statusBarHeight,
                             final Rect scanBoxAreaRect) {
        int viewWidth = getWidth();
        int viewHeight = getHeight();

        PointF result;
        float scaleX;
        float scaleY;

        if (ContinueBGAQRCodeUtil.isPortrait(getContext())) {
            scaleX = viewWidth / cameraPreviewHeight;
            scaleY = viewHeight / cameraPreviewWidth;
            result = new PointF((cameraPreviewHeight - originX) * scaleX, (cameraPreviewWidth - originY) * scaleY);
            result.y = viewHeight - result.y;
            result.x = viewWidth - result.x;

            if (scanBoxAreaRect == null) {
                result.y += statusBarHeight;
            }
        } else {
            scaleX = viewWidth / cameraPreviewWidth;
            scaleY = viewHeight / cameraPreviewHeight;
            result = new PointF(originX * scaleX, originY * scaleY);
            if (isMirrorPreview) {
                result.x = viewWidth - result.x;
            }
        }

        if (scanBoxAreaRect != null) {
            result.y += scanBoxAreaRect.top;
            result.x += scanBoxAreaRect.left;
        }

        return result;
    }

    public interface Delegate {
        /**
         * 处理扫描结果
         *
         * @param result 摄像头扫码时只要回调了该方法 result 就一定有值，不会为 null。解析本地图片或 Bitmap 时 result 可能为 null
         */
        void onScanQRCodeSuccess(String result);

        /**
         * 处理打开相机出错
         */
        void onScanQRCodeOpenCameraError();
    }

    /**
     * 解析本地图片二维码。返回二维码图片里的内容 或 null
     *
     * @param picturePath 要解析的二维码图片本地路径
     */
    public void decodeQRCode(String picturePath) {
        mProcessDataTask = new ContinueProcessDataTask(picturePath, this).perform();
    }

    /**
     * 解析 Bitmap 二维码。返回二维码图片里的内容 或 null
     *
     * @param bitmap 要解析的二维码图片
     */
    public void decodeQRCode(Bitmap bitmap) {
        mProcessDataTask = new ContinueProcessDataTask(bitmap, this).perform();
    }
}