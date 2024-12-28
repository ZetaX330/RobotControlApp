package com.example.rcapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.BluetoothLeScanner;

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;


import com.example.rcapp.databinding.ActivityBluetoothLinkBinding;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import android.bluetooth.BluetoothDevice;
import android.widget.Toast;


public class BluetoothLinkActivity extends BleServiceBaseActivity{
    private ActivityBluetoothLinkBinding binding;
    private LeDeviceListAdapter leDeviceListAdapter;
    private BluetoothDevice leDevice;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final long SCAN_PERIOD = 10000;
    private MyToolBar myToolBar;
    //用于绑定的回调请求接口
    @SuppressLint("MissingPermission")
    @Override
    protected void onBluetoothServiceConnected() {
        myToolBar.setBluetoothService(getBluetoothService());
        //蓝牙设备扫描结果监听
        getBluetoothService().setDeviceFoundListener(new BluetoothService.DeviceFoundListener() {
            @Override
            public void onDeviceFound(BluetoothDevice device) {
                //调用设备列表的adapter的addDevice，将回调传来的扫描到的设备加入列表
                leDeviceListAdapter.addDevice(device);
            }
        });
        getBluetoothService().setBLEConnectedListener(new BluetoothService.BLEConnectedListener() {
            @Override
            public void onBLEStatusInform(BluetoothGatt gatt) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(BluetoothLinkActivity.this,"连接成功",Toast.LENGTH_SHORT).show();
                        myToolBar.setBluetoothStatus(gatt.getDevice().getName(),1);

                    }
                });
            }
        });
        //此Activity初始进行一次蓝牙扫描
        getBluetoothService().startScan();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBluetoothLinkBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initMyToolbar(binding.myToolbar.getId());
        myToolBar=binding.myToolbar;
        leDeviceListAdapter = new LeDeviceListAdapter(this, R.layout.bluetooth_item_layout);
        binding.bluetoothListview.setAdapter(leDeviceListAdapter);
    }
    public void BLEDeviceConnect(int position) {
        leDevice = leDeviceListAdapter.getItem(position);
        if (leDevice == null) {
            return;
        }
        //连接设备的方法不是静态，需要实例化service调用
        myToolBar.setBluetoothStatus("连接中",2);
        getBluetoothService().connectToDevice(leDevice);
    }
    @Override
    protected void onResume(){
        super.onResume();
        myToolBar.initBluetoothStatus();
    }

}