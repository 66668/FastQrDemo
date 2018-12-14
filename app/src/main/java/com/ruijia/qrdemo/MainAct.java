package com.ruijia.qrdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.ruijia.qrdemo.function.QrSizeChangeAct;
import com.ruijia.qrdemo.function.ZbarCloseAct;
import com.ruijia.qrdemo.function.ZbarFrameAct;
import com.ruijia.qrdemo.function.ZbarFrameAct2;
import com.ruijia.qrdemo.function.ZbarMinTimeAct;
import com.ruijia.qrdemo.function.ZbarMinTimeAct2;
import com.ruijia.qrdemo.function.ZbarOpenAct;
import com.ruijia.qrdemo.function.ZbarOpenAndCloseAct;
import com.ruijia.qrdemo.function.ZbarPicAct;
import com.ruijia.qrdemo.function.ZbarZIPAct;
import com.ruijia.qrdemo.function.ZxingCloseAct;
import com.ruijia.qrdemo.function.ZxingMinTimeAct;
import com.ruijia.qrdemo.function.ZxingOpenAct;

public class MainAct extends AppCompatActivity implements View.OnClickListener {
    Button btn_zxing_close, btn_zbar_close, btn_zxing_open, btn_zbar_open, btn_open, btn_zxing_time, btn_zbar_time,
            btn_zbar_time2, btn_zbar_open_close, btn_zbar_pic, btn_zbar_zip, btn_zbar_frame, btn_zbar_frame2, btn_qrchangeSize;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //
        setContentView(R.layout.act_main);

        //
        btn_qrchangeSize = findViewById(R.id.btn_qrchangeSize);
        btn_zbar_zip = findViewById(R.id.btn_zbar_zip);
        btn_zbar_pic = findViewById(R.id.btn_zbar_pic);
        btn_open = findViewById(R.id.btn_open);
        btn_zbar_time = findViewById(R.id.btn_zbar_time);
        btn_zbar_time2 = findViewById(R.id.btn_zbar_time2);
        btn_zbar_open_close = findViewById(R.id.btn_zbar_open_close);
        btn_zxing_time = findViewById(R.id.btn_zxing_time);
        btn_zxing_close = findViewById(R.id.btn_zxing_close);
        btn_zbar_close = findViewById(R.id.btn_zbar_close);
        btn_zxing_open = findViewById(R.id.btn_zxing_open);
        btn_zbar_open = findViewById(R.id.btn_zbar_open);
        btn_zbar_frame = findViewById(R.id.btn_zbar_frame);
        btn_zbar_frame2 = findViewById(R.id.btn_zbar_frame2);
        btn_zxing_close.setOnClickListener(this);
        btn_zbar_time.setOnClickListener(this);
        btn_zbar_zip.setOnClickListener(this);
        btn_zbar_pic.setOnClickListener(this);
        btn_qrchangeSize.setOnClickListener(this);
        btn_zbar_time2.setOnClickListener(this);
        btn_zbar_open_close.setOnClickListener(this);
        btn_zxing_time.setOnClickListener(this);
        btn_zbar_close.setOnClickListener(this);
        btn_zxing_open.setOnClickListener(this);
        btn_zbar_open.setOnClickListener(this);
        btn_open.setOnClickListener(this);
        btn_zbar_frame.setOnClickListener(this);
        btn_zbar_frame2.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == btn_open) {//发送图片
            startActivity(new Intent(MainAct.this, SendMainAct.class));
        } else if (v == btn_qrchangeSize) {//二维码显示优化
            startActivity(new Intent(MainAct.this, QrSizeChangeAct.class));
        } else if (v == btn_zbar_frame) {//zbar取帧拍照
            startActivity(new Intent(MainAct.this, ZbarFrameAct.class));
        } else if (v == btn_zbar_frame2) {//zbar取帧识别
            startActivity(new Intent(MainAct.this, ZbarFrameAct2.class));
        } else if (v == btn_zxing_close) {//zxing 闭路
            startActivity(new Intent(MainAct.this, ZxingCloseAct.class));
        } else if (v == btn_zbar_close) {//zbar闭路
            startActivity(new Intent(MainAct.this, ZbarCloseAct.class));
        } else if (v == btn_zxing_open) {//zxing 开路
            startActivity(new Intent(MainAct.this, ZxingOpenAct.class));
        } else if (v == btn_zbar_open) {//zbar开路
            startActivity(new Intent(MainAct.this, ZbarOpenAct.class));
        } else if (v == btn_zxing_time) {//zxing极限单张测试
            startActivity(new Intent(MainAct.this, ZxingMinTimeAct.class));
        } else if (v == btn_zbar_time) {//zbar极限单张测试
            startActivity(new Intent(MainAct.this, ZbarMinTimeAct.class));
        } else if (v == btn_zbar_time2) {//zbar极限单张测试2
            startActivity(new Intent(MainAct.this, ZbarMinTimeAct2.class));
        } else if (v == btn_zbar_open_close) {//zbar开路+闭路高效传输
            startActivity(new Intent(MainAct.this, ZbarOpenAndCloseAct.class));
        } else if (v == btn_zbar_pic) {//传输图片
            startActivity(new Intent(MainAct.this, ZbarPicAct.class));
        } else if (v == btn_zbar_zip) {//传输zip
            startActivity(new Intent(MainAct.this, ZbarZIPAct.class));
        }
    }
}
