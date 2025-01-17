package com.example.rcapp.data.service

import android.Manifest.permission
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.rcapp.ui.viewmodel.activity.BleServiceBaseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID


class BluetoothService : Service() {
    private val binder: IBinder = LocalBinder()

    //返回蓝牙设备的Adapter
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectionJob: Job? = null
    private var scanJob: Job? = null
    private var isConnected = false
    private val connectFailed : Int = 4

    fun getBluetoothAdapter(): BluetoothAdapter? {
        return bluetoothAdapter
    }
    fun getBluetoothGatt(): BluetoothGatt? {
        return bluetoothGatt
    }

    private var scanning = false
    private var bleServiceBaseActivity: BleServiceBaseActivity? = null
    private var bLEScanListener: BLEScanListener? = null
    private var bleConnectionListener: BLEConnectionListener? = null

    //设备查找回调，传给BluetoothLinkActivity查找到的设备
    fun interface BLEScanListener {
        fun onBLEScan(result: ScanResult?)
    }

    //设备连接回调，传给BluetoothLinkActivity连接结果
    //功能是更新BluetoothLinkActivity的已连接设备页面UI
    fun interface BLEConnectionListener {
        fun onBLEConnection(gatt: BluetoothGatt?)
    }

    /**
     * 下面设置接口的方法都将接口示例当作参数
     * 如果使用Lambda表达式作为参数则应写为
     * this.bleConnectionListener = BLEConnectionListener { gatt, isConnected -> listener(gatt, isConnected) }
     * 但是Kotlin允许用单个抽象方法的接口通过 Lambda 表达式来实例化，详细见setBLEConnectionListener的使用处
     */
    fun setBLEConnectionListener(listener: BLEConnectionListener) {
        this.bleConnectionListener = listener
    }
    fun setBLEScanListener(listener: BLEScanListener) {
        this.bLEScanListener = listener
    }

    //其它Activity共同使用的服务，通过获取基类Activity，调用基类活动的方法实现
    fun setBaseActivity(bleServiceBaseActivity: BleServiceBaseActivity?) {
        this.bleServiceBaseActivity = bleServiceBaseActivity
    }

    override fun onCreate() {
        super.onCreate()
        //先初始化蓝牙
        initBluetooth()
    }

    private fun initBluetooth() {
        //通过bluetoothManager获取bluetoothAdapter和bluetoothAdapter
        val bluetoothManager =
            getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            return
        }
        bluetoothLeScanner = bluetoothAdapter!!.bluetoothLeScanner
    }

    override fun onDestroy() {
        super.onDestroy()
        //停止扫描
        stopScan()
        //停止设备蓝牙状态广播接收
        unregisterReceiver(bluetoothReceiver)
        //关闭与设备的连接
        closeGatt()
    }

    //关闭设备的gatt
    private fun closeGatt() {
        if (bluetoothGatt != null) {
            //检查权限
            if (checkPermission(permission.BLUETOOTH_CONNECT)) {
                //关闭蓝牙的Gatt
                bluetoothGatt!!.close()
            } else {
                requestPermission(permission.BLUETOOTH_CONNECT)
            }
            bluetoothGatt = null
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    inner class LocalBinder : Binder() {
        val service: BluetoothService
            get() = this@BluetoothService
    }

    //启动服务后的服务初始化
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        //注册一个广播接收器，接收设备蓝牙状态的改变，bluetoothReceiver中为接收后具体的处理
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        return START_STICKY
    }

    val isBluetoothSupported: Boolean
        //返回设备是否支持BLE
        get() = bluetoothAdapter != null && packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    //权限检查，返回检查结果
    private fun checkPermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            bleServiceBaseActivity!!,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    //请求权限，由Service交付给基类Activity检查，permission为附上要请求的权限类型
    private fun requestPermission(permission: String) {
        if (bleServiceBaseActivity != null) {
            bleServiceBaseActivity!!.requestPermission(permission)
        }
    }

    //设备蓝牙状态改变广播接收器
    private val bluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //如果接收到的广播动作是蓝牙状态改变的广播
            if (BluetoothAdapter.ACTION_STATE_CHANGED == intent.action) {
                //获得广播中BluetoothAdapter.EXTRA_STATE的值，即设备的蓝牙状态，默认为BluetoothAdapter.ERROR
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (bleServiceBaseActivity != null) {
                    //更新BleServiceBaseActivity中蓝牙状态，实际上最终改变的是toolbar状态，以及相应状态的处理（比如弹出提醒框）
                    if(state==BluetoothAdapter.STATE_OFF){
                        if (!checkPermission(permission.BLUETOOTH_CONNECT)) {
                            requestPermission(permission.BLUETOOTH_CONNECT)
                            Log.e(TAG, "No Permission")
                            return
                        }
                        //取消扫描超时处理任务
                        scanJob?.cancel()
                        //关闭Gatt
                        closeGatt()
                        //传递null，更新已连接设备UI
                        bleConnectionListener?.onBLEConnection(null)
                    }
                }
                //更新toolbarUI和发送Toast
                bleServiceBaseActivity!!.updateBluetoothUI(state, bluetoothGatt)
            }
        }
    }

    //开始扫描蓝牙设备
    fun startScan() {
        //确保bluetoothLeScanner不为null,为null则初始化scanner
        bluetoothLeScanner = bluetoothLeScanner ?: bluetoothAdapter?.bluetoothLeScanner
        //直接结束上次扫描
        stopScan()
        //取消连接超时任务
        connectionJob?.cancel()
        if (!checkPermission(permission.BLUETOOTH_SCAN)) {
            requestPermission(permission.BLUETOOTH_SCAN)
            return
        }
        //开启扫描操作
        //SCAN_MODE_LOW_LATENCY为高频率扫描，设备发现速度快，但是耗电高
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        Log.d(TAG, "Scanning will start")
        if(!scanning){
            scanning = true
            bluetoothLeScanner!!.startScan(null, scanSettings, leScanCallback)
            //一个定时任务，扫描最多持续10秒，十秒后停止扫描
            scanJob = CoroutineScope(Dispatchers.Main).launch {
                delay(10000)
                stopScan()
                bLEScanListener!!.onBLEScan(null)

            }
            Log.d(TAG, "Scanning start")
        }

    }

    //停止扫描蓝牙设备
    private fun stopScan() {
        //检查bluetoothLeScanner是否为null，同时保证有扫描正在进行
        bluetoothLeScanner?.takeIf { scanning } ?: return
        if (!checkPermission(permission.BLUETOOTH_SCAN)) {
            requestPermission(permission.BLUETOOTH_SCAN)
            return
        }
        //停止扫描操作
        bluetoothLeScanner!!.stopScan(leScanCallback)
        scanning = false
        scanJob?.cancel()
        Log.d(TAG, "Scanning stop")
    }

    //蓝牙设备扫描回调
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            //result.getRssi():返回信号强度指示 (RSSI)。
            //result.getScanRecord():获取设备的广播数据。
            //获取扫描到的设备
            val device = result.device
            if (bLEScanListener != null) {
                if (!checkPermission(permission.BLUETOOTH_CONNECT)) {
                    requestPermission(permission.BLUETOOTH_CONNECT)
                    Log.e(TAG, "No Permission")
                    return
                }
                if (!device.name.isNullOrEmpty()) {
                    //BluetoothLinkActivity实现了该接口，接收扫描到的结果
                    bLEScanListener!!.onBLEScan(result)
                }

            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error: $errorCode")
        }
    }

    //尝试连接蓝牙设备
    fun connectToDevice(device: BluetoothDevice?) {
        if (device == null) {
            return
        }
        if (!checkPermission(permission.BLUETOOTH_CONNECT)) {
            requestPermission(permission.BLUETOOTH_CONNECT)
            return
        }
        //连接蓝牙设备实际上是连接设备的Gatt
        //device.connectGatt最终返回一个BluetoothGatt对象
        //第一个参数Context只做连接用，不影响连接完成后的BluetoothGatt，所以使用基类Activity作为参数
        bluetoothGatt = device.connectGatt(bleServiceBaseActivity, true, gattCallback)
        //连接持续时间最多为5秒，5秒内未连接成功则连接失败
        connectionJob = CoroutineScope(Dispatchers.Main).launch {
            delay(5000)
            if (!isConnected) {
                bluetoothGatt?.close()
                isConnected=false
                //超时后若未连接上，更新toolbarUI并发生toast
                bleServiceBaseActivity?.updateBluetoothUI(connectFailed,bluetoothGatt)
                closeGatt()
            }
        }
        //Log.d(TAG, "Connecting to device: " + device.address)
    }

    //连接设备Gatt结果回调
    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            //无论状态如何，直接更新UI
            bleServiceBaseActivity?.updateBluetoothUI(newState,gatt)
            //如果连接成功
            if (status == BluetoothGatt.GATT_SUCCESS&&newState == BluetoothGatt.STATE_CONNECTED) {
                //连接成功后取消连接超时任务
                connectionJob?.cancel()
                isConnected = true
                bleConnectionListener?.onBLEConnection(gatt)
                if (bleServiceBaseActivity?.let {
                        ActivityCompat.checkSelfPermission(
                            it,
                            permission.BLUETOOTH_CONNECT
                        )
                    } != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermission(permission.BLUETOOTH_CONNECT)
                    return
                }
                //查找蓝牙服务
                bluetoothGatt?.discoverServices()
            }
//            //如果连接失败
//            if(newState==BluetoothGatt.STATE_DISCONNECTED){
//                bleConnectionListener?.onBLEConnection(null)
//            }
        }

        //对应bluetoothGatt.discoverServices()的回调结果
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //this.serviceStatus = true
                Log.d(TAG, "Services discovered: " + gatt.services)
            } else {
                Log.e(
                    TAG,
                    "Service discovery failed: $status"
                )
            }
        }
    }

    /**
     * 对蓝牙设备的操作需要对应设备的特征值，写入操作则需要写入特征值，可以传入特征码来查找特征值
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun writeData(characteristicUUID: UUID, data: ByteArray) {
        if (bluetoothGatt == null) {
            Log.e(TAG, "No connected device")
            return
        }
        // 检查蓝牙权限
        if (!checkPermission(permission.BLUETOOTH_CONNECT)) {
            requestPermission(permission.BLUETOOTH_CONNECT)
            Log.e(TAG, "No Permission")
            return
        }
        //查找对应特征值
        val characteristic = findCharacteristic(characteristicUUID)
        if (characteristic == null) {
            Log.e(TAG, "Characteristic not found")
            return
        }
        //写入数据
        try {
            val writeStatus = bluetoothGatt!!.writeCharacteristic(
                characteristic,
                data,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
            if (writeStatus == BluetoothStatusCodes.SUCCESS) {
                Log.d(TAG, "Message from server: ")
                Log.d(TAG, "Write operation initiated successfully")
            } else {
                Log.e(
                    TAG,
                    "Write operation failed to initiate, status: $writeStatus"
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during write operation: " + e.message)
            // 在这里处理权限被拒绝的情况
        }
    }

    /**
     * 该方法会在连接上的设备的服务中查找传递来的特征码，如果某服务的特征值的特征码与之匹配，则返回该特征值
     */
    private fun findCharacteristic(uuid: UUID): BluetoothGattCharacteristic? {
        for (service in bluetoothGatt!!.services) {
            for (characteristic in service.characteristics) {
                if (characteristic.uuid == uuid) {
                    return characteristic
                }
            }
        }
        Log.e(
            TAG,
            "Characteristic not found for UUID: $uuid"
        )
        return null
    }

    companion object {
        private const val TAG = "BluetoothService"
    }
}