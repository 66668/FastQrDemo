package com.ruijia.qrdemo.function;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ruijia.qrdemo.R;
import com.ruijia.qrdemo.utils.CodeUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cn.qqtheme.framework.picker.FilePicker;

/**
 * 优化显示尺寸，保证800px*800px的图片显示最大bitmap（大容量），或最容易识别的bitmap（小容量）
 * 优化方向分两种
 * （1）改bitmap大小，测试改bitmap大小的耗时情况，判断是否可行
 * （2）改ImageView尺寸，用于小容量二维码，保证更容易识别。（实际情况是：连续识别大容量二维码后，突然来一张小容量二维码，小容量二维码不被识别，所以就改小一点的测试方案）
 */
public class QrSizeChangeAct extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "SJY";
    private static int defaultImgSize = 800;//默认800宽高
    private static int defaultQrSize = 800;//默认800宽高
    private static int defaultQrlength = 100;//默认100宽高
    private Handler handler;
    //控件
    private Button btn_reset, btn_qr, btn_show, btn_imgSize, btn_qrSize;
    private EditText et_qr, et_img, et_qrSize;
    private ImageView img_result;
    private Bitmap orgBitmap;
    private Bitmap newBitmap;

    //参数
    private int qrlength = 100;//默认1024，最大2952
    private int imgSize = 800;//默认800宽高
    private int qrSize = 800;//默认800宽高
    String content = "";

    //打印时间
    private long createBitmapStartTime = 0;
    private long changeBitmapStartTime = 0;
    private long changeImgStartTime = 0;

    //=====================================核心测试==========================================

    //步骤1
    private void createQrBitmap() {
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... voids) {
                createBitmapStartTime = System.currentTimeMillis();
                for (int i = 0; i < qrlength; i++) {
                    content += "a";
                }
                Bitmap bitmap = CodeUtils.createByMultiFormatWriter(content, 400);
                return bitmap;
            }

            @Override
            protected void onPostExecute(final Bitmap bitmap) {
                orgBitmap = bitmap;
                Log.d(TAG, "(1)二维码生成时间=" + (System.currentTimeMillis() - createBitmapStartTime));
                changeBitmapSize();

            }
        }.execute();
    }

    //步骤2
    private void changeBitmapSize() {
        if (orgBitmap == null) {
            Log.e(TAG, "没有生成二维码");
            return;
        }
        changeBitmapStartTime = System.currentTimeMillis();
        //获取宽高
        int width = orgBitmap.getWidth();
        int height = orgBitmap.getHeight();
        Log.d(TAG, "(2)orgBitmap原始尺寸=" + width + "*" + height);

        if (width > 0 && width < 800) {//400级别
            // 计算缩放比例.
            float scaleWidth = ((float) qrSize) / width;
            float scaleHeight = ((float) qrSize) / height;
            Log.d(TAG, "修改bitmap尺寸--缩放比例=" + scaleHeight);

            // 取得想要缩放的matrix参数.
            Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth, scaleHeight);
            newBitmap = Bitmap.createBitmap(orgBitmap, 0, 0, width, height, matrix, true);
            Log.d(TAG, "(2)修改bitmap尺寸耗时=" + (System.currentTimeMillis() - changeBitmapStartTime));
        } else if (width >= 400 && width < 600) {
            Log.d(TAG, "(2)修改bitmap尺寸耗时=" + (System.currentTimeMillis() - changeBitmapStartTime));
        } else if (width >= 600 && width < 800) {
            Log.d(TAG, "(2)修改bitmap尺寸耗时=" + (System.currentTimeMillis() - changeBitmapStartTime));
        } else if (width >= 800) {
            Log.d(TAG, "(2)修改bitmap尺寸耗时=" + (System.currentTimeMillis() - changeBitmapStartTime));
        }
        if (newBitmap != null) {
            //获取宽高
            int newwidth = newBitmap.getWidth();
            int newheight = newBitmap.getHeight();
            Log.d(TAG, "(2)newBitmap尺寸=" + newwidth + "*" + newheight);
            //下一步
            changeImgSize();
        } else {
            Log.e(TAG, "没有生成新的二维码");
        }

    }

    //步骤三
    private void changeImgSize() {

        changeImgStartTime = System.currentTimeMillis();
        ViewGroup.LayoutParams params = img_result.getLayoutParams();

        if (imgSize == defaultImgSize) {
            Log.d(TAG, "（3）原始ImageView尺寸=" + params.height + "*" + params.width);
            //执行下一步
            showResult();
        } else {
            params.height = imgSize;
            params.width = imgSize;
            img_result.setLayoutParams(params);
            Log.d(TAG, "（3）修改ImageView尺寸=" + params.height + "*" + params.width);
            Log.d(TAG, "（3）修改ImageView尺寸耗时=" + (System.currentTimeMillis() - changeImgStartTime));
            //执行下一步
            showResult();
        }
    }

    //步骤4
    private void showResult() {
        img_result.setImageBitmap(newBitmap);
    }

    //=====================================复写==========================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_qrsize_change);
        initView();
    }

    /**
     * 初始化控件
     */
    private void initView() {
        handler = new Handler();
        //控件
        btn_qrSize = findViewById(R.id.btn_qrSize);
        et_qrSize = findViewById(R.id.et_qrSize);
        img_result = findViewById(R.id.img_result);
        et_qr = findViewById(R.id.et_qr);
        et_img = findViewById(R.id.et_img);
        btn_qr = findViewById(R.id.btn_qr);
        btn_reset = findViewById(R.id.btn_reset);
        btn_imgSize = findViewById(R.id.btn_imgSize);
        btn_show = findViewById(R.id.btn_show);

        btn_qrSize.setOnClickListener(this);
        btn_reset.setOnClickListener(this);
        btn_qr.setOnClickListener(this);
        btn_show.setOnClickListener(this);
        btn_imgSize.setOnClickListener(this);

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_reset:
                //显示二维码
                resetParams();
                break;
            case R.id.btn_show:
                createQrBitmap();
                break;

            case R.id.btn_qr://二维码
                String str = et_qr.getText().toString();
                if (TextUtils.isEmpty(str)) {
                    qrlength = defaultQrlength;
                    et_qr.setText("");
                    et_qr.setHint("二维码长度设置" + qrlength);
                } else {
                    qrlength = Integer.parseInt(str);
                    if (qrlength > 2952) {
                        qrlength = 2952;
                    }
                    et_qr.setText("");
                    et_qr.setHint("二维码长度设置" + qrlength);
                }
                break;

            case R.id.btn_imgSize://尺寸
                String str1 = et_img.getText().toString();
                if (TextUtils.isEmpty(str1)) {
                    imgSize = defaultImgSize;
                    et_img.setText("");
                    et_img.setHint("图尺寸设置" + imgSize);
                } else {
                    imgSize = Integer.parseInt(str1);
                    et_img.setText("");
                    et_img.setHint("图尺寸设置" + imgSize);
                }
                break;
            case R.id.btn_qrSize://qrBitmap图尺寸
                String str2 = et_qrSize.getText().toString();
                if (TextUtils.isEmpty(str2)) {
                    qrSize = defaultQrSize;
                    et_qrSize.setText("");
                    et_qrSize.setHint("bitmap尺寸设置" + imgSize);
                } else {
                    imgSize = Integer.parseInt(str2);
                    et_qrSize.setText("");
                    et_qrSize.setHint("bitmap尺寸设置" + imgSize);
                }
                break;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    //=====================================private处理==========================================

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
                return CodeUtils.createByMultiFormatWriter(content, 400);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    img_result.setImageBitmap(bitmap);
                } else {
                    Log.e("SJY", "生成二维码失败");
                    Toast.makeText(QrSizeChangeAct.this, "生成二维码失败", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }


    /**
     * 重置
     */
    private void resetParams() {
        orgBitmap = null;
        newBitmap = null;
        resetImg();
        handler = new Handler();
        //输入重置
        imgSize = defaultImgSize;
        qrlength = defaultQrlength;
        qrSize = defaultQrSize;
        content = "";
    }

    private void resetImg() {
        img_result.setImageBitmap(null);
        ViewGroup.LayoutParams params = img_result.getLayoutParams();
        params.height = defaultImgSize;
        params.width = defaultImgSize;
        img_result.setLayoutParams(params);
        Log.d(TAG, "重置ImageView： " + params.height + "*" + params.width);
    }


}
