package com.ruijia.qrdemo;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ruijia.qrdemo.utils.CodeUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import cn.qqtheme.framework.picker.FilePicker;

/**
 * 发送端   生成二维码数据
 */
public class SendMainAct extends AppCompatActivity implements View.OnClickListener {
    private static final int LIM = 200;//发送间隔时间
    private int sendDelay = LIM;
    private TextView tv_show;
    private ImageView img_result;
    private Button btn_select, btn_show, btn_sure, btn_single, btn_add;
    private EditText et_input;
    private Handler handler;

    //test数据
    private List<String> orgDatas = new ArrayList<>();//发送数据
    private int sendTimes = 0;//发送次数，
    String a = "";
    private int length = 1025;//默认1024，最大2952

    /**
     * 发送二维码
     */
    private void startShow() {
        sendTimes = 0;
        handler.postDelayed(sendtask, 3000);
    }

    Runnable sendtask = new Runnable() {
        @Override
        public void run() {
            if (sendTimes < orgDatas.size()) {
                showBitmap(orgDatas.get(sendTimes));
                sendTimes++;
                handler.postDelayed(this, sendDelay);
            } else {
                showBitmap("识别完了识别完了");
            }
        }
    };

    /**
     * 重新初始化流程控制参数
     */
    private void initParams() {
        length = 100;
        sendTimes = 0;
        a = "";
        orgDatas = new ArrayList<>();
        btn_show.setVisibility(View.GONE);
        img_result.setImageBitmap(null);
        img_result.setBackground(ContextCompat.getDrawable(this, R.mipmap.ic_launcher));
        //
        sendDelay = LIM;
        et_input.setText("");
        et_input.setHint("间隔" + sendDelay + "ms");
        tv_show.setText("");
    }


    //=====================================复写==========================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_send);
        initView();
    }

    /**
     * 初始化控件
     */
    private void initView() {
        handler = new Handler();
        //控件
        tv_show = findViewById(R.id.tv_show);
        btn_add = findViewById(R.id.btn_add);
        btn_single = findViewById(R.id.btn_single);
        et_input = findViewById(R.id.et_input);
        btn_select = findViewById(R.id.btn_select);
        btn_sure = findViewById(R.id.btn_sure);
        btn_show = findViewById(R.id.btn_show);
        btn_show.setVisibility(View.GONE);
        btn_show.setOnClickListener(this);
        btn_sure.setOnClickListener(this);
        btn_add.setOnClickListener(this);
        btn_single.setOnClickListener(this);
        btn_select.setOnClickListener(this);
        img_result = (ImageView) findViewById(R.id.barcodePreview);
        img_result.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initParams();
            }
        });
        //默认间隔
        sendDelay = LIM;
        et_input.setText("");
        et_input.setHint("间隔" + sendDelay + "ms");
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_select:
                //文件选择器
                FilePicker picker = new FilePicker(this, FilePicker.FILE);
                picker.setShowHideDir(false);
                picker.setAllowExtensions(new String[]{".txt", ".jpg", ".png", ".zip", ".rar",".jpeg"});
                //TODO 路径配置
//                picker.setRootPath(Environment.getExternalStorageDirectory().getAbsolutePath());
                picker.setRootPath("/storage/emulated/legacy/");
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
            case R.id.btn_add:
                length += 512;
                if (length > 2952) {
                    length = 2952;
                }
                tv_show.setText("字符长度=" + length);
                break;
            case R.id.btn_single:
                a = "";
                for (int i = 0; i < length; i++) {
                    a += "a";
                }
                //显示二维码
                showBitmap(a);
                break;
            case R.id.btn_sure:
                String str = et_input.getText().toString();
                if (TextUtils.isEmpty(str)) {
                    sendDelay = LIM;
                    et_input.setText("");
                    et_input.setHint("选中间隔" + sendDelay);
                } else {
                    sendDelay = Integer.parseInt(str);
                    et_input.setText("");
                    et_input.setHint("选中间隔" + sendDelay);
                }
                break;
        }

    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(sendtask);
        super.onDestroy();
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
                return CodeUtils.createByMultiFormatWriter(content, 800);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    img_result.setImageBitmap(bitmap);
                } else {
                    Log.e("SJY", "生成二维码失败");
                    Toast.makeText(SendMainAct.this, "生成二维码失败", Toast.LENGTH_SHORT).show();
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
                    //添加标记，收尾各一个
                    int size = orgDatas.size();
                    orgDatas.add(0, "listSize=" + size);
                    orgDatas.add("识别完了识别完了识别完了");
                    //打印 可删除。
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
                    Toast.makeText(SendMainAct.this, s, Toast.LENGTH_SHORT).show();
                    btn_show.setVisibility(View.GONE);
                    return;
                }
                Log.d("SJY", "---orgDatas.size()=" + orgDatas.size());
                btn_show.setVisibility(View.VISIBLE);
            }
        }.execute();


    }


}
