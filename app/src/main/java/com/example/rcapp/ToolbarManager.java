package com.example.rcapp;

import java.util.ArrayList;
import java.util.List;

public class ToolbarManager {
    private String bluetoothDeviceName="未连接";
    private int bluetoothStatus;
    private ToolbarManager() {}

    private static final class InstanceHolder {
        private static final ToolbarManager instance = new ToolbarManager();
    }

    public static ToolbarManager getInstance() {
        return InstanceHolder.instance;
    }

    public void setBluetoothStatus(String bluetoothDeviceName,int bluetoothStatus) {
        this.bluetoothDeviceName=bluetoothDeviceName;
        this.bluetoothStatus = bluetoothStatus;
        // 通知所有监听器设备名称已更新
//        notifyBluetoothStatusChanged(bluetoothDeviceName,bluetoothStatus);
    }
    public String getBluetoothDeviceName() {
        return bluetoothDeviceName;
    }
    public int getBluetoothStatus() {
        return bluetoothStatus;
    }
}