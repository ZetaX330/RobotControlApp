package com.example.rcapp;

import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_SCAN;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.UUID;

public class BluetoothService extends Service {
    private static final String TAG = "BluetoothService";
    private final IBinder binder = new LocalBinder();
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private boolean isServiceEnable = false;
    private boolean scanning = false;
    private BleServiceBaseActivity bleServiceBaseActivity;
    private DeviceFoundListener deviceFoundListener;
    private BLEConnectionListener bleConnectionListener;

    //设备查找回调，传给BluetoothLinkActivity查找到的设备
    public interface DeviceFoundListener {
        void onDeviceFound(BluetoothDevice device);
    }

    //设备连接回调，传给BluetoothLinkActivity连接结果
    public interface BLEConnectionListener {
        void onBLEStatusInform(BluetoothGatt gatt);
    }


    public void setDeviceFoundListener(DeviceFoundListener listener) {
        this.deviceFoundListener = listener;
    }

    public void setBLEConnectedListener(BLEConnectionListener listener) {
        this.bleConnectionListener = listener;
    }

    //其它Activity共同使用的服务，通过获取基类Activity，调用基类活动的方法实现
    public void setBaseActivity(BleServiceBaseActivity bleServiceBaseActivity) {
        this.bleServiceBaseActivity = bleServiceBaseActivity;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        //先初始化蓝牙
        initBluetooth();
    }

    private void initBluetooth() {
        //通过bluetoothManager获取bluetoothAdapter和bluetoothAdapter
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            return;
        }
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            return;
        }
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //停止扫描
        stopScan();
        //停止设备蓝牙状态广播接收
        unregisterReceiver(bluetoothReceiver);
        //关闭与设备的连接
        closeGatt();
    }

    private void closeGatt() {
        if (bluetoothGatt != null) {
            //检查权限
            if (checkPermission(BLUETOOTH_CONNECT)) {
                //关闭蓝牙的Gatt
                bluetoothGatt.close();
            }
            else {
                requestPermission(BLUETOOTH_CONNECT);
            }
            bluetoothGatt = null;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    //启动服务后的服务初始化
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //注册一个广播接收器，接收设备蓝牙状态的改变，bluetoothReceiver中为接收后具体的处理
        registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        return START_STICKY;
    }

    //返回设备蓝牙服务是否可用
    public boolean getServiceStatus() {
        return isServiceEnable;
    }

    //返回蓝牙设备的Adapter
    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    //返回设备是否支持BLE
    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null && getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    //权限检查，返回检查结果
    private boolean checkPermission(String permission) {
        return ActivityCompat.checkSelfPermission(bleServiceBaseActivity, permission) == PackageManager.PERMISSION_GRANTED;
    }

    //请求权限，由Service交付给基类Activity检查，permission为附上要请求的权限类型
    private void requestPermission(String permission) {
        if (bleServiceBaseActivity != null) {
            bleServiceBaseActivity.requestPermission(permission);
        }
    }

    //设备蓝牙状态改变广播接收器
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //如果接收到的广播动作是蓝牙状态改变的广播
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                //获得广播中BluetoothAdapter.EXTRA_STATE的值，即设备的蓝牙状态，默认为BluetoothAdapter.ERROR
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if(state==BluetoothAdapter.STATE_ON){
                    initBluetooth();
                }
                if (bleServiceBaseActivity != null) {
                    //更新BleServiceBaseActivity中蓝牙状态，实际上最终改变的是toolbar状态，以及相应状态的处理（比如弹出提醒框）
                    bleServiceBaseActivity.setBluetoothState(state);
                }
            }
        }
    };

    //开始扫描蓝牙设备
    public void startScan() {
        //检查bluetoothLeScanner是否为null，同时保证上次扫描没有结束
        if (bluetoothLeScanner == null || scanning) {
            return;
        }
        if (!checkPermission(BLUETOOTH_SCAN)) {
            requestPermission(BLUETOOTH_SCAN);
            return;
        }
        scanning = true;
        //开启扫描操作
        bluetoothLeScanner.startScan(leScanCallback);
    }

    //停止扫描蓝牙设备
    public void stopScan() {
        //检查bluetoothLeScanner是否为null，同时保证有扫描正在进行
        if (bluetoothLeScanner == null || !scanning) {
            return;
        }
        if (!checkPermission(BLUETOOTH_SCAN)) {
            requestPermission(BLUETOOTH_SCAN);
            return;
        }
        scanning = false;
        //停止扫描操作
        bluetoothLeScanner.stopScan(leScanCallback);
        Log.d(TAG, "Scanning stopped");
    }

    //蓝牙设备扫描回调
    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //result.getRssi():返回信号强度指示 (RSSI)。
            //result.getScanRecord():获取设备的广播数据。
            //获取扫描到的设备
            BluetoothDevice device = result.getDevice();
            if (deviceFoundListener != null) {
                //BluetoothLinkActivity实现了该接口，接收扫描到的设备
                deviceFoundListener.onDeviceFound(device);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Scan failed with error: " + errorCode);
        }
    };

    //尝试连接蓝牙设备
    public void connectToDevice(BluetoothDevice device) {
        if (device == null) {
            return;
        }
        if(!checkPermission(BLUETOOTH_CONNECT)){
            requestPermission(BLUETOOTH_CONNECT);
            return;
        }
        //连接蓝牙设备实际上是连接设备的Gatt
        //device.connectGatt最终返回一个BluetoothGatt对象
        //第一个参数Context只做连接用，不影响连接完成后的BluetoothGatt，所以使用基类Activity作为参数
        bluetoothGatt = device.connectGatt(bleServiceBaseActivity, true, gattCallback);
        Log.d(TAG, "Connecting to device: " + device.getAddress());
    }

    //连接设备Gatt结果回调
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            //如果连接结果是连接成功GATT_SUCCESS
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //向Activity传递设备Gatt以及告知连接结果
                handleConnectionStateChange(gatt, newState);
            }
            else {
                Log.e(TAG, "Connection error: " + status);
                //否则关闭设备Gatt
                closeGatt();
            }
        }

        //连接状态传递
        private void handleConnectionStateChange(BluetoothGatt gatt, int newState) {
            if(!checkPermission(BLUETOOTH_CONNECT)){
                requestPermission(BLUETOOTH_CONNECT);
                return;
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //如果设备新状态是已连接状态STATE_CONNECTED
                Log.d(TAG, "Connected to GATT server.");
                if (bleConnectionListener != null) {
                    //BluetoothLinkActivity实现此接口，接收连接设备的Gatt
                    bleConnectionListener.onBLEStatusInform(gatt);
                }
                //连接上后查找设备的蓝牙服务
                bluetoothGatt.discoverServices();
            }
            //如果没有连接上
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server.");
                //发送null，表示连接失败
                bleConnectionListener.onBLEStatusInform(null);
                //关闭设备Gatt，并不是连接上才有Gatt，Gatt可以表示连接结果
                closeGatt();
            }
        }

        //对应bluetoothGatt.discoverServices()的回调结果
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                isServiceEnable = true;
                Log.d(TAG, "Services discovered: " + gatt.getServices());
            }
            else {
                Log.e(TAG, "Service discovery failed: " + status);
            }
        }
    };
    public void writeData(UUID characteristicUUID, byte[] data) {
        if (bluetoothGatt == null) {
            Log.e(TAG, "No connected device");
            return;
        }
        // 检查蓝牙权限
        if(!checkPermission(BLUETOOTH_CONNECT)){
            requestPermission(BLUETOOTH_CONNECT);
            Log.e(TAG, "No Permission");
            return;
        }
        BluetoothGattCharacteristic characteristic = findCharacteristic(characteristicUUID);
        if (characteristic == null) {
            Log.e(TAG, "Characteristic not found");
            return;
        }
        try {
            int writeStatus = bluetoothGatt.writeCharacteristic(characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            if (writeStatus == BluetoothStatusCodes.SUCCESS) {
                Log.d(TAG, "Write operation initiated successfully");
            }
            else {
                Log.e(TAG, "Write operation failed to initiate, status: " + writeStatus);
            }
        }
        catch (SecurityException e) {
            Log.e(TAG, "Security exception during write operation: " + e.getMessage());
            // 在这里处理权限被拒绝的情况
        }
    }
    private BluetoothGattCharacteristic findCharacteristic(UUID uuid) {
        for (BluetoothGattService service : bluetoothGatt.getServices()) {
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                if (characteristic.getUuid().equals(uuid)) {
                    return characteristic;
                }
            }
        }
        Log.e(TAG, "Characteristic not found for UUID: " + uuid);
        return null;
    }
}