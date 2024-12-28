package com.example.rcapp;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public abstract class BleServiceBaseActivity extends AppCompatActivity {
    protected BluetoothService bluetoothService;
    protected MyToolBar myToolbar;
    private boolean isBound = false;

    protected abstract void onBluetoothServiceConnected();

    protected void initMyToolbar(int myToolbarId) {
        myToolbar = findViewById(myToolbarId);
        if (myToolbar != null) {
            setSupportActionBar(myToolbar.getToolbar());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindBluetoothService();
    }

    private void bindBluetoothService() {
        Intent intent = new Intent(this, BluetoothService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bluetoothService != null) {
            bluetoothService.setBaseActivity(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindBluetoothService();
    }

    private void unbindBluetoothService() {
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    public void requestPermission(String permissionRequest) {
        ActivityCompat.requestPermissions(this, new String[]{permissionRequest}, 100);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != 100) {
            finishAffinity();
        }
    }

    public void setBluetoothState(int state) {
        if (state == BluetoothAdapter.STATE_OFF) {
            myToolbar.setBluetoothStatus(null, 0);
            requestEnableBluetooth();
        }
        else if (state == BluetoothAdapter.STATE_ON) {
            myToolbar.setBluetoothStatus(null, 1);
            Toast.makeText(this, "蓝牙已开启", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestEnableBluetooth() {
        new AlertDialog.Builder(this)
                .setTitle("设备蓝牙已关闭")
                .setMessage("请打开蓝牙")
                .setPositiveButton("好的", null)
                .show();
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            bluetoothService = binder.getService();
            isBound = true;
            bluetoothService.setBaseActivity(BleServiceBaseActivity.this);
            onBluetoothServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    public BluetoothService getBluetoothService() {
        return bluetoothService;
    }
}