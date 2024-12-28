package com.example.rcapp;

import static androidx.activity.result.ActivityResultCallerKt.registerForActivityResult;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothService extends Service {
    private static final String TAG = "BluetoothService";
    private final IBinder binder = new LocalBinder();
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private boolean isServiceEnable=false;
    private final List<BluetoothDevice> scannedDevices = new ArrayList<>();
    private boolean scanning = false;
    private BleServiceBaseActivity bleServiceBaseActivity;

    public void setBaseActivity(BleServiceBaseActivity  bleServiceBaseActivity) {
        this.bleServiceBaseActivity = bleServiceBaseActivity;
    }
    private DeviceFoundListener deviceFoundListener;
    private BLEConnectedListener bleConnectedListener;
    private PermissionRequestListener permissionRequestListener;
    public interface DeviceFoundListener { void onDeviceFound(BluetoothDevice device);}
    public interface BLEConnectedListener { void onBLEStatusInform(BluetoothGatt gatt);}
    public interface PermissionRequestListener { void onRequestPermission();}
    public void setDeviceFoundListener(DeviceFoundListener listener) {this.deviceFoundListener = listener;}
    public void setBLEConnectedListener(BLEConnectedListener listener) {this.bleConnectedListener = listener;}
    public void setPermissionRequestListener(PermissionRequestListener listener) {this.permissionRequestListener = listener;}
    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null) {
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            }
        }
    }
    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopScan();
        unregisterReceiver(bluetoothReceiver);
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG,"onBind");

        return binder;
    }
    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }
    public boolean getServiceStatus(){
        return  isServiceEnable;
    }
    public BluetoothAdapter getBluetoothAdapter(){
        return  bluetoothAdapter;
    }
    // 权限检查和请求方法
    public static final int REQUEST_CODE = 100;
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                Log.e(TAG,"打开:"+state);
                if (bleServiceBaseActivity != null) {
                    bleServiceBaseActivity.setBluetoothState(state);
                }
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filter);
        return START_STICKY;
    }

    public static void checkBluetoothPermissions(Activity activity) {
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 权限未被授予，请求权限
            ActivityCompat.requestPermissions(activity,
                    new String[]{android.Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE);
        }
    }
    public void openBluetooth(){
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            //权限不够则请求权限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                if (permissionRequestListener != null) {
                    permissionRequestListener.onRequestPermission();
                }
                //不管权限是否获得，结束此次操作
                return;
            }
            //权限足够直接关闭
            else {
                bluetoothAdapter.enable();

            }
        }
    }

    @SuppressLint("MissingPermission")
    public void startScan() {
        if (bluetoothLeScanner != null && !scanning) {
            scanning = true;
            scannedDevices.clear();
            bluetoothLeScanner.startScan(leScanCallback);
            Log.d(TAG, "Scanning started");
        }
    }
    @SuppressLint("MissingPermission")
    public void stopScan() {
        if (bluetoothLeScanner != null && scanning) {
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
            Log.d(TAG, "Scanning stopped");
        }
    }
    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            if (!scannedDevices.contains(device)) {
                // 通过回调接口,返回扫描到的设备给绑定的Activity
                if (deviceFoundListener != null) {
                    deviceFoundListener.onDeviceFound(device);
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "Scan failed with error: " + errorCode);
        }
    };

    @SuppressLint("MissingPermission")
    public void connectToDevice(BluetoothDevice device) {
        if (device != null) {
            bluetoothGatt = device.connectGatt(this, false, gattCallback);

            Log.d(TAG, "Connecting to device: " + device.getAddress());
        }
    }
    @SuppressLint("MissingPermission")
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Connected to GATT server.");
                    if (bleConnectedListener != null) {
                        bleConnectedListener.onBLEStatusInform(gatt);
                    }
                    bluetoothGatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Disconnected from GATT server.");
                    if (bluetoothGatt != null) {
                        bluetoothGatt.close();
                        bluetoothGatt = null;
                    }
                }
            }
            else {
                Log.e(TAG, "Connection error: " + status);
                if (bluetoothGatt != null) {
                    bluetoothGatt.close();
                    bluetoothGatt = null;
                }
            }
            // 关闭GATT
//            if (bluetoothGatt != null) {
//                bluetoothGatt.close();
//                bluetoothGatt = null;
//            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                isServiceEnable=true;
                Log.d(TAG, "Services discovered: " + gatt.getServices());
            } else {
                Log.e(TAG, "Service discovery failed: " + status);
            }
        }
    };
    @SuppressLint("MissingPermission")
    public void writeData(UUID characteristicUUID, byte[] data) {
        if (bluetoothGatt == null) {
            Log.e(TAG, "No connected device");
            return;
        }
        // 查找特性并写入数据
        BluetoothGattCharacteristic characteristic = findCharacteristic(characteristicUUID);
        if (characteristic != null) {
            characteristic.setValue(data);
            Log.e(TAG, characteristic.toString());
            bluetoothGatt.writeCharacteristic(characteristic);
        }
        //Log.e(TAG, "Characteristic not found");
    }
    // 方法：查找特性（帮助函数）
    private BluetoothGattCharacteristic findCharacteristic(UUID uuid) {
        if (bluetoothGatt != null) {
            for (BluetoothGattService service : bluetoothGatt.getServices()) {
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    if (characteristic.getUuid().equals(uuid)) {
                        Log.e(TAG, "Match found. Characteristic UUID: " + characteristic.getUuid());
                        return characteristic;
                    }
                }
            }
            Log.e(TAG, "Characteristic not found for UUID: " + uuid);
        }
        return null;
    }



}