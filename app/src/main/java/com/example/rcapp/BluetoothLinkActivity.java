package com.example.rcapp;

import static android.Manifest.permission.BLUETOOTH_CONNECT;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.example.rcapp.databinding.ActivityBluetoothLinkBinding;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class BluetoothLinkActivity extends BleServiceBaseActivity {
    private ActivityBluetoothLinkBinding binding;
    private LeDeviceListAdapter leDeviceListAdapter;
    private BluetoothDevice leDevice;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private MyToolBar myToolBar;
    //BluetoothService蓝牙服务绑定回调，在BleServiceBaseActivity中声明的抽象方法，由继承的Activity具体实现
    @Override
    protected void onBluetoothServiceConnected() {
        //设置蓝牙设备扫描结果监听
        setupDeviceListeners();
        if(getBluetoothService().getBluetoothAdapter().isEnabled()){
            //先进行一次蓝牙设备扫描
            getBluetoothService().startScan();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("LifeCycle", "onCreate");
        binding = ActivityBluetoothLinkBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //初始化toolbar
        initToolbar();
        //初始化蓝牙设备列表adapter
        initListAdapter();
        //初始化列表刷新控件
        initSwipeRefresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("LifeCycle", "onResume");
        //Activity可见时，重新获取一遍toolbar的状态，实际蓝牙状态已经被刷新，该函数只用于刷新toolbar
        myToolBar.initBluetoothStatus();
        //Activity的onResume在服务绑定回调之前执行，所以第一次onResume中getBluetoothService为null
        if (getBluetoothService() != null) {
            //当Activity再次可见时，重新扫描获取一次蓝牙设备列表
            scheduler.execute(() -> getBluetoothService().startScan());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("LifeCycle", "onDestroy");
        // 确保调度器被关闭
        scheduler.shutdown();
    }

    private void initToolbar() {
        //设置基类Activity的Toolbar
        initMyToolbar(binding.myToolbar.getId());
        //获取Activity的toolbar
        myToolBar = binding.myToolbar;
    }

    private void initListAdapter() {
        //初始化蓝牙列表adapter，参数为context和表项layout
        leDeviceListAdapter = new LeDeviceListAdapter(this, R.layout.bluetooth_item_layout);
        //设置列表视图的adapter
        binding.bluetoothLv.setAdapter(leDeviceListAdapter);
    }

    private void initSwipeRefresh() {
        //下拉刷新列表监听
        binding.bluetoothRefreshSrl.setOnRefreshListener(() -> {
            //主线程上安排一个延迟执行的任务，延迟时间为500毫秒，500ms后调用refreshDevices
            new Handler(Looper.getMainLooper()).postDelayed(this::refreshDevices, 500);
        });
    }

    //刷新蓝牙列表
    private void refreshDevices() {
        // 停止扫描
        getBluetoothService().stopScan();
        // 清空列表
        leDeviceListAdapter.clearList();
        // 开启扫描
        getBluetoothService().startScan();
        // 停止刷新动画
        binding.bluetoothRefreshSrl.setRefreshing(false);
    }



    private void setupDeviceListeners() {
        //蓝牙设备发现监听，由BluetoothService定义，将查找到的设备加入设备列表
        getBluetoothService().setDeviceFoundListener(device -> runOnUiThread(() ->leDeviceListAdapter.addDevice(device)));
        //蓝牙设备连接监听，由BluetoothService定义
        getBluetoothService().setBLEConnectedListener(gatt -> runOnUiThread(() -> {
            if(gatt!=null){
                Toast.makeText(BluetoothLinkActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                //更改蓝牙状态，这里依然是更改toolbar状态，实际上蓝牙连接状态已改变
                updateBluetoothStatus(gatt);
            }
            else{
                Toast.makeText(BluetoothLinkActivity.this, "连接失败", Toast.LENGTH_SHORT).show();

            }
        }));
    }

    private void updateBluetoothStatus(BluetoothGatt gatt) {
        //权限检查与请求
        if (ActivityCompat.checkSelfPermission(this, BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(BLUETOOTH_CONNECT);
        }
        else {
            //权限足够，则更改连接设备名和蓝牙状态
            myToolBar.setBluetoothStatus(gatt.getDevice().getName(), 1);
        }
    }

    //由LeDeviceListAdapter调用，处理连接设备的点击事件
    public void BLEDeviceConnect(int position) {
        //先获取点击的具体设备
        leDevice = leDeviceListAdapter.getItem(position);
        if (leDevice != null) {
            //更改toolbar状态，正在连接
            myToolBar.setBluetoothStatus("连接中", 2);
            //调用BluetoothService的connectToDevice，连接该蓝牙设备
            //该方法有最终有一个回调分支方法onConnectionStateChange，同时此Activity实现了BluetoothService的onBLEStatusInform
            //BLEConnectionListener在onConnectionStateChang执行onBLEStatusInform，回传连接结果
            scheduler.execute(() -> getBluetoothService().connectToDevice(leDevice));
        }
    }

}