package com.ruijia.qrdemo.utils;

public class ConvertUtils {

    //int 转四位String返回
    public static String int2String(int size) {
        if (size > 10000) {
            return "" + 10000;
        }
        String str = new Integer(size).toString();
        if (str.length() <= 0) {
            return "0000";
        } else if (str.length() == 1) {
            return "000" + str;
        } else if (str.length() == 2) {
            return "00" + str;
        } else if (str.length() == 3) {
            return "0" + str;
        } else {
            return str;
        }
    }
}
