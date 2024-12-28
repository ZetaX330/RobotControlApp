package com.example.rcapp;
import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static com.example.rcapp.BluetoothService.REQUEST_CODE;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public abstract class BleServiceBaseActivity extends AppCompatActivity implements BluetoothService.PermissionRequestListener{
    protected BluetoothService bluetoothService;
    protected MyToolBar myToolbar;
    private boolean isBound = false;
    private static final String TAG = "BaseActivityLog";
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
        Intent intent = new Intent(this, BluetoothService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        Intent serviceIntent = new Intent(this, BluetoothService.class);
//        LocalBroadcastManager.getInstance(this).registerReceiver(bluetoothStateReceiver,
//                new IntentFilter("bluetooth-state-changed"));
        startService(serviceIntent);

    }
    @Override
    protected void onStop() {
        super.onStop();
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(bluetoothStateReceiver);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
    public void setBluetoothState(int state){
        if(state==BluetoothAdapter.STATE_OFF){
            myToolbar.setBluetoothStatus(null,0);
            requestEnableBluetooth();
        }
        else if(state==BluetoothAdapter.STATE_ON){
            Toast.makeText(this, "蓝牙已开启", Toast.LENGTH_SHORT).show();
        }
    }
    public void requestEnableBluetooth() {
        new AlertDialog.Builder(this)
                .setTitle("设备蓝牙已关闭")
                .setMessage("是否打开蓝牙")
                .setPositiveButton("是", (dialog, which) -> {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    requestEnableBluetooth.launch(enableIntent);
                })
                .setNegativeButton("否", (dialog, which) -> {
                    // Handle cancellation if needed
                })
                .show();
    }
    private final ActivityResultLauncher<Intent> enableBluetoothLauncher= registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    myToolbar.setBluetoothStatus(null,1);
                    bluetoothService.openBluetooth();
                } else {
                    Toast.makeText(this, "蓝牙未开启", Toast.LENGTH_SHORT).show();
                }
            });
    private final ActivityResultLauncher<Intent> requestEnableBluetooth= registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    enableBluetoothLauncher.launch(enableIntent);
                }
            });



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

//    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//
//        }
//    };

    @Override
    public void onRequestPermission() {
        //BluetoothService向操作发起Activity进行权限请求，由该Activity的基类Activity处理
        //ActivityCompat.requestPermissions为ActivityCompat类的权限请求方法
        ActivityCompat.requestPermissions(this,
                new String[]{BLUETOOTH_CONNECT},
                REQUEST_CODE);
    }
    //requestPermissions的回调结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //表示请求成功，与REQUEST_CODE匹配
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                bluetoothService.openBluetooth();
            } else {
                // 处理权限未授予的情况
                return;
            }
        }
    }
    public BluetoothService getBluetoothService() {
        return bluetoothService;
    }

}