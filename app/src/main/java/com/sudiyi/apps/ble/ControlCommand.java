package com.sudiyi.apps.ble;

/**
 * Created by JackZheng on 2017/8/17.
 */

public enum ControlCommand {
    Open0("OPEN0"),
    Open1("OPEN1"),
    Open2("OPEN2"),
    Open3("OPEN3"),
    Unknown("null");

    private String val;

    ControlCommand(String val) {
        this.val = val;
    }

    public static ControlCommand vOf(String val) {
        switch (val) {
            case "OPEN0":
                return Open0;
            case "OPEN1":
                return Open1;
            case "OPEN2":
                return Open2;
            case "OPEN3":
                return Open3;
            default:
                return Unknown;
        }
    }

    public String getValue() {
        return this.val;
    }
}
