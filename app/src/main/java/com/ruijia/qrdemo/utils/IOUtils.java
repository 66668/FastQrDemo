package com.ruijia.qrdemo.utils;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 流操作 工具类
 */
public class IOUtils {
    /**
     * File-->String
     * 将txt文件，转成List<String>数据，该数据经过处理，包含了头信息。
     *
     * @param file
     * @return
     */
    public static List<String> textFile2Array(File file) throws Exception {
        List<String> orgDatas = new ArrayList<>();
        String linStr;

        //1
        FileInputStream inputStream = new FileInputStream(file);
        long length = file.length();
        Log.d("SJY", "文件大小=" + length + "B");
        InputStreamReader inputReader = new InputStreamReader(inputStream, "utf-8");
        BufferedReader buffReader = new BufferedReader(inputReader);
        //2
        while ((linStr = buffReader.readLine()) != null) {//读一行
            if (linStr.length() < 2940) {
                orgDatas.add(linStr);
            } else if (linStr.length() > 2940 && linStr.length() < 5880) {
                String a1 = linStr.substring(0, 2940);
                String a2 = linStr.substring(2940, linStr.length());
                orgDatas.add(a1);
                orgDatas.add(a2);
            } else {
                throw new Exception("字符长度超出处理能力lenMax=5880");
            }
        }
        //
        inputStream.close();
        inputReader.close();
        buffReader.close();

        return orgDatas;
    }

    /**
     * String-->File
     * base64 字符串转任何file
     *
     * @return
     */
    public static String base64ToFile(String dataStr, String type) {

        String filePath = null;
        if (type.contains("jpg")) {
            filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/receve.jpg";
        } else if (type.contains("png")) {
            filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/receve.png";
        } else if (type.contains("zip")) {
            filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/receve.zip";
        }
        File file = new File(filePath);

        OutputStream os = null;
        byte[] data = Base64.decode(dataStr, Base64.DEFAULT);
//        if (data[0] < 0) {
//            Log.e("SJY", "String2PngFile数据需要调整base64");
//            for (int i = 0; i < data.length; i++) {
//                if (data[i] < 0) {
//                    //调整异常数据
//                    data[i] += 256;
//                }
//            }
//        }
        try {
            os = new FileOutputStream(file);
            os.write(data);
            os.flush();
            os.close();
            return filePath;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("SJY", "base64ToFile异常：" + e.toString());
            return null;
        }
    }


    /**
     * File-->String
     * string字符串按长度2940切成array
     *
     * @return
     * @throws Exception
     */
    public static List<String> stringToArray(String data) {
        if (TextUtils.isEmpty(data))
            return null;
        int len = 2940;
        List<String> orgDatas = new ArrayList<>();
        //设置list的size
        int size = 0;
        int last = 0;//余数
        if ((data.length() % len) > 0) {//有余数
            size = data.length() / len + 1;
            last = data.length() % len;
        } else {
            size = data.length() / len;
        }
        //添加数据
        for (int i = 0; i < size; i++) {
            if (i == size - 1) {
                String item = data.substring(len * i, len * i + last);
                Log.d("SJY", i + "--数据长度=" + item.length());
                orgDatas.add(item);
            } else {
                String item = data.substring(len * i, len * (i + 1));
                Log.d("SJY", i + "--数据长度=" + item.length());
                orgDatas.add(item);
            }

        }

        return orgDatas;
    }

    /**
     * String-->File
     * <p>
     * array转回String
     *
     * @return
     * @throws Exception
     */
    public static String arrayToString(List<String> array) {
        String data = null;
        for (String s : array) {
            data += s;
        }
        return data;
    }

    /**
     * File-->String
     * <p>
     * （1）文件转base64：图片转base64
     *
     * @return
     */
    public static String pngFile2String(File filePath) {

        InputStream is = null;
        byte[] data = null;
        String result = new String();
        try {
            is = new FileInputStream(filePath);
            data = new byte[is.available()];
            is.read(data);
            //base64
            result = Base64.encodeToString(data, Base64.DEFAULT);

            return result;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * File-->String
     * <p>
     * （1）文件转base64：任何文件转base64
     *
     * @return
     */
    public static String fileToBase64(File file) {
        String result = new String();
        InputStream fis = null;
        ByteArrayOutputStream bos = null;
        try {
            //file转字节流
            fis = new FileInputStream(file);

            //输出 二进制流
            bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            //二进制流转base64
            result = Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT);
            Log.d("SJY", "result=" + result);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
            return null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
