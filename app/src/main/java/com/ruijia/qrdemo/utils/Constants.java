package com.ruijia.qrdemo.utils;

/**
 * 原生摄像头的常量池
 */
public class Constants {
    /**
     * 发送时间间隔
     * <p>
     * 默认150
     */
    public static final String TIME_INTERVAL = "time_interval";
    public static final int DEFAULT_TIME = 130;

    /**
     * 最大文件大小 默认5M
     */
    public static final String FILE_SIZE = "fileSize";
    public static final int DEFAULT_SIZE = 5;

    public static final String CON_TIME_OUT = "connect_timeout";
    public static final int TIMEOUT = 15;

    /**
     * 字符流 截取长度
     * <p>
     * zxing core 3.3.3 最大的传输容量2954,17长度做标记头和标记尾。2952-17=2935
     * <p>
     * 最强性能长度 2938
     */
    public static final int qrSize = 2935;//


    /**
     * 识别过程，最大20次来回传图没有结果，强制结束
     */
    public static final int MAX_TIMES = 20;


    /**
     * 最开始的时间
     * <p>
     * 从链路层的service接收到文件开始的时间，
     * <p>
     * 很重要的标记，当识别完成后，最新时间 减去 该时间，就是文件传输的总耗时。
     */
    public static final String START_TIME = "startTime";

    /**
     * 准备发送二维码的时间
     * <p>
     * 很重要的标记，当识别完成后，最新时间 减去 该时间，就是二维码识别的总耗时。
     */
    public static final String START_SEND_TIME = "sendQrTime";


}
