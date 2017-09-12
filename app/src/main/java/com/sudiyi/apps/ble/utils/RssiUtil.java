package com.sudiyi.apps.ble.utils;

/**
 * 功能：根据rssi计算距离
 * Created by JackZheng on 2017/9/1.
 */

public class RssiUtil {
    //A和n的值，需要根据实际环境进行检测得出
    private static final double A_Value = 59;

    /**
     * A - 发射端和接收端相隔1米时的信号强度
     */
    private static final double n_Value = 2.0;/** n - 环境衰减因子*/

    /**
     * 根据Rssi获得返回的距离,返回数据单位为m
     *
     * @param rssi
     * @return
     */
    public static double getDistance(int rssi) {
        int iRssi = Math.abs(rssi);
        double power = (iRssi - A_Value) / (10 * n_Value);
        return Math.pow(10, power);
    }
    //15828574006

    static final char[] hexArray = "0123456789ABCDEF".toCharArray();


    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static double calculateAccuracy(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }

        double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            double accuracy = (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
            return accuracy;
        }
    }


    public static double getValue(byte[] scanRecord, int rssi) {
        double distance = -1;
        int startByte = 2;
        boolean patternFound = false;
        // 寻找ibeacon
        while (startByte <= 5) {
            if (((int) scanRecord[startByte + 2] & 0xff) == 0x02 && // Identifies
                    // an
                    // iBeacon
                    ((int) scanRecord[startByte + 3] & 0xff) == 0x15) { // Identifies
                // correct
                // data
                // length
                patternFound = true;
                break;
            }
            startByte++;
        }
        // 如果找到了的话
        //if (patternFound) {
        // 转换为16进制
        byte[] uuidBytes = new byte[16];
        System.arraycopy(scanRecord, startByte + 4, uuidBytes, 0, 16);
        String hexString = bytesToHex(uuidBytes);

        // ibeacon的UUID值
        String uuid = hexString.substring(0, 8) + "-"
                + hexString.substring(8, 12) + "-"
                + hexString.substring(12, 16) + "-"
                + hexString.substring(16, 20) + "-"
                + hexString.substring(20, 32);

        // ibeacon的Major值
        int major = (scanRecord[startByte + 20] & 0xff) * 0x100
                + (scanRecord[startByte + 21] & 0xff);

        // ibeacon的Minor值
        int minor = (scanRecord[startByte + 22] & 0xff) * 0x100
                + (scanRecord[startByte + 23] & 0xff);

        int txPower = (scanRecord[startByte + 24]);

        LogUtils.d("distance：" + calculateAccuracy(txPower, rssi));
        return calculateAccuracy(-55, rssi);

        //return distance;
    }
}