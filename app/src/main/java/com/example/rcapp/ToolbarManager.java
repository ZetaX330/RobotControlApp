package com.example.rcapp;

public class ToolbarManager {
    private String bluetoothDeviceName;
    private int bluetoothStatus;

    private ToolbarManager() {}

    private static final class InstanceHolder {
        private static final ToolbarManager instance = new ToolbarManager();
    }

    public static ToolbarManager getInstance() {return InstanceHolder.instance;}

    public String getBluetoothDeviceName() {return bluetoothDeviceName;}

    public int getBluetoothStatus() {
        return bluetoothStatus;
    }

    public void setBluetoothStatus(String bluetoothDeviceName, int bluetoothStatus) {
        this.bluetoothDeviceName = bluetoothDeviceName;
        this.bluetoothStatus = bluetoothStatus;
    }
}