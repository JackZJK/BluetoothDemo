package com.sudiyi.apps.ble.utils;

import java.security.NoSuchAlgorithmException;

/**
 * Created by JackZheng on 2017/8/17.
 */

public class Md5DataUtil {

    /**
     * 获取加密信息
     *
     * @param id
     * @param open
     * @return
     */
    public static String getMd5Data(String id, String open) {

        String date = DateUtils.getSimpleCurrDate();
        StringBuffer stb = new StringBuffer();
        stb.append(id);
        stb.append(date);
        stb.append(open);
        String sendOpenDate = "";
        try {
            String data = stb.toString();
            LogUtils.d("current Send data：" + data);
            sendOpenDate = Md5Util.get16BitMD5(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return sendOpenDate;
    }
}
