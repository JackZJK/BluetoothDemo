package com.sudiyi.apps.ble.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by JackZheng on 2017/8/17.
 */

public class Md5Util {

    public static String get32BitMD5(String val) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(val.getBytes());
        byte[] m = md5.digest();//加密  
        return get32BitString(m);
    }

    public static String get16BitMD5(String val) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(val.getBytes());
        byte[] m = md5.digest();//加密
        return get16BitString(m);
    }


    private static String get32BitString(byte[] b) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            int a = b[i];
            if (a < 0)
                a += 256;
            if (a < 16)
                buf.append("0");
            buf.append(Integer.toHexString(a));
        }
        return buf.toString(); //32位   
    }

    private static String get16BitString(byte[] b) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            int a = b[i];
            if (a < 0)
                a += 256;
            if (a < 16)
                buf.append("0");
            buf.append(Integer.toHexString(a));
        }
        return buf.toString().substring(8, 24);  //16位
    }
}
