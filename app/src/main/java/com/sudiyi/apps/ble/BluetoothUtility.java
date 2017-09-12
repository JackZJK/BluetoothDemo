package com.sudiyi.apps.ble;

import android.app.Activity;

import com.sudiyi.apps.ble.utils.LogUtils;

import java.io.UnsupportedEncodingException;

public class BluetoothUtility {
    private static final String TAG = "BLE";

    public static final String SERVICE_UUID_1 = "00001802-0000-1000-8000-00805f9b34fb";
    public static final String CHAR_UUID_1 = "afdf39cc-e249-4397-b342-295408ac33bc";

    BluetoothUtility(Activity a) {
    }

    public static String byteArraytoString(byte[] byteArray) {
        try {
            if (byteArray != null) {
                String str = new String(byteArray, "UTF-8");
                return str;
            } else {
                return "";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static byte[] stringToByte(String value) {
        try {
            if (value != null && value.length() > 0)
                return value.getBytes("UTF-8");
            else
                return null;
        } catch (UnsupportedEncodingException ex) {
            LogUtils.d("stringToByte UnsupportedEncodingException.");
            ex.printStackTrace();
            return null;
        } catch (Exception e) {
            LogUtils.d("stringToByte exception. " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
