package com.ruijia.qrdemo;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.RemoteException;
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
import com.ruijia.qrdemo.utils.Constants;
import com.ruijia.qrdemo.utils.ConvertUtils;
import com.ruijia.qrdemo.utils.IOUtils;
import com.ruijia.qrdemo.utils.SPUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import cn.qqtheme.framework.picker.FilePicker;

/**
 * 发送端   生成二维码数据
 */
public class SendMainAct extends AppCompatActivity implements View.OnClickListener {
    private static final int LIM = 200;//发送间隔时间
    private int sendDelay = LIM;
    private TextView tv_show;
    private ImageView img_result;
    private Button btn_select, btn_show, btn_sure, btn_single, btn_add, btn_length;
    private EditText et_input, et_length;
    private Handler handler;

    private static final int qrSize = 800;//该值和屏幕宽度尺寸相关
    private Timer timer;//倒计时类
    //test数据
    private List<String> newDatas = new ArrayList<>();
    private List<Bitmap> maps = new ArrayList<>();
    private int size = 0;//当前文件的list长度
    private long fileSize = 0;//文件大小
    private int sendCounts = 0;//发送次数，
    String a = "";
    private int length = 100;//默认1024，最大2952
    //统计类
    private long ioStartTime;
    private long ioTime;
    private long sendStartTime;

    /**
     * 重新初始化流程控制参数
     */
    private void initParams() {
        length = 100;
        sendCounts = 0;
        a = "";
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
        et_length = findViewById(R.id.et_length);
        btn_length = findViewById(R.id.btn_length);
        tv_show = findViewById(R.id.tv_show);
        btn_add = findViewById(R.id.btn_add);
        btn_single = findViewById(R.id.btn_single);
        et_input = findViewById(R.id.et_input);
        btn_select = findViewById(R.id.btn_select);
        btn_sure = findViewById(R.id.btn_sure);
        btn_show = findViewById(R.id.btn_show);
        btn_show.setVisibility(View.GONE);
        btn_length.setOnClickListener(this);
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
//        picker.setShowUpDir(true);
                picker.setRootPath(Environment.getExternalStorageDirectory().getAbsolutePath());
//        picker.setAllowExtensions(new String[]{".txt"});
                picker.setAllowExtensions(null);//没有过滤
                picker.setFileIcon(getResources().getDrawable(android.R.drawable.ic_menu_agenda));
                picker.setFolderIcon(getResources().getDrawable(android.R.drawable.ic_menu_upload_you_tube));
                //picker.setArrowIcon(getResources().getDrawable(android.R.drawable.arrow_down_float));
                picker.setOnFilePickListener(new FilePicker.OnFilePickListener() {
                    @Override
                    public void onFilePicked(String currentPath) {
                        Log.d("SJY", "currentPath=" + currentPath);
                        if (!TextUtils.isEmpty(currentPath)) {
                            getRealFile(currentPath);
                        }
                    }
                });
                picker.show();
                break;
            case R.id.btn_show:
                //显示二维码
                startSend();
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

            case R.id.btn_length://
                String str1 = et_length.getText().toString();
                if (TextUtils.isEmpty(str1)) {
                    length = 100;
                    et_length.setText("");
                    et_length.setHint("二维码长度" + length);
                } else {
                    length = Integer.parseInt(str1);
                    et_length.setText("");
                    et_length.setHint("二维码长度" + length);
                }
                break;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.gc();
    }

    //=====================================private处理==========================================
    private void startSend() {
        sendStartTime = System.currentTimeMillis();
        sendCounts = 0;
        img_result.setImageBitmap(null);
        //开启倒计时
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //
                        if (sendCounts < maps.size()) {
                            img_result.setImageBitmap(maps.get(sendCounts));
                            sendCounts++;

                        } else {
                            try {
                                //结束倒计时
                                if (timer != null) {
                                    timer.cancel();
                                    timer = null;
                                }
                                long endTime = System.currentTimeMillis()-sendStartTime;
                                Log.d("SJY", "二维码发送结束总耗时=" + endTime + "ms" + "--二维码速率="
                                        + (fileSize / endTime) + "KB/s" + "--文件发送速率（文件转换+二维码）=" + (fileSize / (endTime + ioTime)) + "KB/s");
                            } catch (Exception e) {
                                //已处理
                                e.printStackTrace();
                                if (timer != null) {
                                    timer.cancel();
                                    timer = null;
                                }
                            }
                        }
                    }
                });

            }
        }, 100, Constants.DEFAULT_TIME);
    }

    /**
     * 将绝对路径转成文件
     *
     * @param absolutePath
     */
    private void getRealFile(String absolutePath) {
        File file = new File(absolutePath);
        if (file == null || !file.exists()) {
            Toast.makeText(this, "没有文件", Toast.LENGTH_SHORT).show();
        } else {
            split2IO(file);
        }
    }


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

    //-----------------------文件拆分操作，耗时操作----------------------

    /**
     * (1)文件分解成字符流
     *
     * @param file
     */
    private void split2IO(final File file) {
        ioStartTime = System.currentTimeMillis();
        final int maxSize = SPUtil.getInt(Constants.FILE_SIZE, 0);
        if (maxSize == 0) {
            Log.e("SJY", "service无法使用SharedPreferences");
            return;
        }
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... voids) {
                long startTime = System.currentTimeMillis();
                //File转String
                String data = IOUtils.fileToBase64(file);
                fileSize = data.length();
                long time = System.currentTimeMillis() - startTime;

                //文件长度是否超出最大传输
                boolean isTrans = false;
                if ((fileSize / 1024 / 1024) > maxSize) {
                    isTrans = false;
                } else {
                    isTrans = true;
                }

                return data;

            }

            @Override
            protected void onPostExecute(String data) {
                super.onPostExecute(data);
                //拿到文件的字符流
                createArray(data);
                fileSize = data.length();//字符流大小
            }
        }.execute();

    }

    /**
     * (2)字符流-->List<String>
     */
    private void createArray(final String data) {
        new AsyncTask<Void, Void, List<String>>() {

            @Override
            protected List<String> doInBackground(Void... voids) {
                long startTime = System.currentTimeMillis();
                //String切割成list
                List<String> orgDatas = IOUtils.stringToArray(data);
                if (orgDatas == null) {
                    return null;
                }
                long time = System.currentTimeMillis() - startTime;
                //

                return orgDatas;
            }

            @Override
            protected void onPostExecute(List<String> list) {
                super.onPostExecute(list);
                //拿到原始list,转成bitmap
                if (list == null || list.size() <= 0) {
                    //回调客户端
                    return;
                } else {
                    //判断数据长度是否超过处理能力
                    if (list.size() > 9999999) {
                        return;
                    } else {
                        createNewArray(list);
                    }
                }
            }
        }.execute();

    }


    /**
     * (3) 原始List转有标记的List数据
     * <p>
     * 说明：String数据段头标记：snd1234567,长度10;尾标记：RJQR,长度4
     * <p>
     * 头标记：
     * <p>
     * snd：长度3：表示发送 长度3
     * <p>
     * 12345:长度7：表示list第几个片段
     * <p>
     * 尾标记：长度4，表示这段数据是否解析正确 RJQR
     *
     * @param orgDatas
     */
    private void createNewArray(final List<String> orgDatas) {
        new AsyncTask<Void, Void, List<String>>() {

            @Override
            protected List<String> doInBackground(Void... voids) {
                List<String> sendDatas = new ArrayList<>();
                long startTime = System.currentTimeMillis();
                try {
                    //添加标记，
                    // 7位的位置标记
                    int size = orgDatas.size();
                    for (int i = 0; i < size; i++) {
                        String pos = ConvertUtils.int2String(i);
                        //拼接数据-->格式：snd(发送标记)+1234567(第几个，从0开始)+数据段
                        sendDatas.add("snd" + pos + orgDatas.get(i) + "RJQR");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }

                //回调客户端
                long time = System.currentTimeMillis() - startTime;
                return sendDatas;
            }

            @Override
            protected void onPostExecute(List<String> list) {
                super.onPostExecute(list);
                //拿到有标记的List,再转qr bitmap
                if (list == null || list.size() <= 0) {
                    //已处理
                    return;
                } else {
                    /**
                     * 集中转qrbitmap，测试发现，线程分成2个最佳，再多也没用
                     */
                    newDatas = list;
                    size = newDatas.size();

                    //转qrbitmap 方式1
//                      createQrBitmap();

//                    方式2:
                    try {
                        if (size < 50) {//150KB
                            createQrBitmap2(newDatas, 1);
                        } else if (size < 100) {//300KB
                            createQrBitmap2(newDatas, 1);
                        } else if (size < 500) {//1.5M左右
                            createQrBitmap2(newDatas, 1);
                        } else {//大于1.5M
                            createQrBitmap2(newDatas, 1);
                        }
                        //测试ArrayList的非线程安全
                        Log.d("SJY", "原数据大小=" + newDatas.size() + "结果大小=" + maps.size());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("SJY", "createQrBitmap异常：" + e.toString());
                    }
                }

            }
        }.execute();
    }

    /**
     * (4)list转qrbitmap
     * 方式1：list大段直接转，耗时长
     *
     * <p>
     */
    private void createQrBitmap() {

        new AsyncTask<Void, Void, List<Bitmap>>() {

            @Override
            protected List<Bitmap> doInBackground(Void... voids) {
                //
                List<Bitmap> sendImgs = new ArrayList<>();
                long startTime = System.currentTimeMillis();
                //sendDatas 转qrbitmap
                for (int i = 0; i < size; i++) {
                    long start = System.currentTimeMillis();
                    Bitmap bitmap = CodeUtils.createByMultiFormatWriter(newDatas.get(i), qrSize);
                    sendImgs.add(bitmap);
                    //回调客户端
                    long end = System.currentTimeMillis() - start;
                }
                //回调客户端
                long time = System.currentTimeMillis() - startTime;
                return sendImgs;
            }

            @Override
            protected void onPostExecute(List<Bitmap> bitmapList) {
                super.onPostExecute(bitmapList);
                maps = new ArrayList<>();
                maps = bitmapList;
            }
        }.execute();
    }

    /**
     * (4)list转qrbitmap
     * <p>
     * 方式2：并发线程池方式
     * <p>
     * 结论：ArrayList 非线程安全，容易导致size变大，线程数再大速度只能缩短一倍。
     *
     * @param list
     * @param nThreads
     * @throws Exception
     */
    List<String> subList = new ArrayList<>();

    public void createQrBitmap2(List<String> list, final int nThreads) throws Exception {
        maps = new ArrayList<>();
        subList = new ArrayList<>();
        int size = list.size();
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        List<Future<List<Bitmap>>> futures = new ArrayList<Future<List<Bitmap>>>(nThreads);
        long startTime = System.currentTimeMillis();//统计
        for (int i = 0; i < nThreads; i++) {
            if (i == (nThreads - 1)) {
                subList = list.subList(size / nThreads * i, list.size());
            } else {
                subList = list.subList(size / nThreads * i, size / nThreads * (i + 1));
            }

            final int finalI = i;
            Callable<List<Bitmap>> task = new Callable<List<Bitmap>>() {
                @Override
                public List<Bitmap> call() throws Exception {
                    List<Bitmap> unitLists = new ArrayList<>();
                    long startTime = System.currentTimeMillis();//统计
                    for (int j = 0; j < subList.size(); j++) {
                        long start = System.currentTimeMillis();//统计
                        Bitmap bitmap = CodeUtils.createByMultiFormatWriter(subList.get(j), qrSize);
                        unitLists.add(bitmap);
                        //回调客户端
                        long end = System.currentTimeMillis() - start;
                    }
                    //回调客户端
                    long time = System.currentTimeMillis() - startTime;
                    return unitLists;
                }
            };
            futures.add(executorService.submit(task));
        }
        for (Future<List<Bitmap>> mfuture : futures) {
            maps.addAll(mfuture.get());
        }
        //清空临时数据。
        subList = new ArrayList<>();
        //发送二维码
        btn_show.setVisibility(View.VISIBLE);
        ioTime = System.currentTimeMillis() - ioStartTime;
        Log.d("SJY", "字符流文件大小-" + fileSize + "B" + "\n文件转换总耗时=" + ioTime);
        startSend();
    }


}
