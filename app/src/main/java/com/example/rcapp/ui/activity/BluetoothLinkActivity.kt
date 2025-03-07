package com.example.rcapp.ui.activity

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rcapp.R
import com.example.rcapp.ui.adapter.ScanDeviceListAdapter
import com.example.rcapp.databinding.ActivityBluetoothLinkBinding
import com.example.rcapp.ui.fragment.BluetoothDeviceFragment
import com.example.rcapp.ui.viewmodel.MainToolbarViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class BluetoothLinkActivity : BLEServiceBaseActivity() {
    private lateinit var binding: ActivityBluetoothLinkBinding
    private var scanDeviceListAdapter: ScanDeviceListAdapter? = null
    private var connectionDevice: BluetoothDevice? = null
    /**
     *BluetoothService蓝牙服务绑定回调，在BleServiceBaseActivity中声明的抽象方法，由继承的Activity具体实现
     */
    override fun onBluetoothServiceConnected() {
        //设置蓝牙设备扫描结果监听
        setDeviceScanListener()
        if (bluetoothService?.getBluetoothAdapter()?.isEnabled == true) {
            //先进行一次蓝牙设备扫描
            CoroutineScope(Dispatchers.IO).launch {
                bluetoothService?.startScan()
            }
            val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.load_anim)
            binding.bluetoothScanIv.startAnimation(fadeInAnimation)
        }
        if (bluetoothService?.getBluetoothGatt() !=null){
            setDeviceConnected(bluetoothService!!.getBluetoothGatt()!!.device)
        }
        /**
         *此处setBLEConnectionListener传入的参数为Lambda表达式
         */
        bluetoothService?.setBLEConnectionListener { gatt: BluetoothGatt?->
            runOnUiThread {
                if(gatt!=null){
                    //更新UI为连接上的设备
                    setDeviceConnected(gatt.device)
                    connectionDevice=gatt.device
                } else{
                    binding.bluetoothConnected.visibility=View.GONE
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("LifeCycle", "onCreate")
        binding = ActivityBluetoothLinkBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(binding.root.id)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setContentView(binding.root)
        initMainToolbar()
        //初始化蓝牙设备列表adapter
        initListAdapter()
        //初始化列表刷新控件
        initSwipeRefresh()
        setDeviceConnectedListener()
    }

    /**
     * Activity的onResume在服务绑定回调之前执行，所以第一次onResume中getBluetoothService为null
     *当Activity再次可见时，重新扫描获取一次蓝牙设备列表
     * 检查 bluetoothService 是否为 null
     */
    override fun onResume() {
        super.onResume()
        Log.e("LifeCycle", "onResume")
        CoroutineScope(Dispatchers.IO).launch {
            bluetoothService?.startScan()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.e("LifeCycle", "onDestroy")
    }
    private fun initMainToolbar() {
        mainBluetoothToolbar = binding.myToolbar
        //如果mainToolbar不为空
        mainBluetoothToolbar?.let {
            //设置该Activity的toolbar为mainToolbar中的toolbar
            setSupportActionBar(it.toolbar)
            //进行mainToolbar的ViewModel的绑定
            it.setViewModel()
        }
    }

    private fun initListAdapter() {
        //初始化蓝牙列表adapter，参数为context和表项layout
        scanDeviceListAdapter = ScanDeviceListAdapter(this)
        //设置列表视图的adapter
        binding.bluetoothScanRv.adapter = scanDeviceListAdapter
        //将RecyclerView设置为使用线性布局管理器，以垂直列表的方式显示数据项
        binding.bluetoothScanRv.layoutManager = LinearLayoutManager(this)
        binding.bluetoothScanRv.itemAnimator = DefaultItemAnimator()
    }

    /**
     * 下拉刷新列表监听
     */
    private fun initSwipeRefresh() {
        binding.bluetoothRefreshSrl.setOnRefreshListener {
            // 使用Kotlin协程在主线程上延迟执行任务
            lifecycleScope.launch {
                // 延迟500毫秒
                delay(500)
                // 调用refreshDevices
                refreshDevices()
                binding.bluetoothRefreshSrl.isRefreshing = false // 停止刷新动画
            }
        }
    }
    /**
     * 刷新蓝牙列表
     */
    private fun refreshDevices() {
        binding.bluetoothScanIv.clearAnimation()
        // 清空列表
        scanDeviceListAdapter!!.clearList()
        // 开启扫描
        CoroutineScope(Dispatchers.IO).launch {
            bluetoothService?.startScan()
        }
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.load_anim)
        binding.bluetoothScanIv.startAnimation(fadeInAnimation)
        // 停止刷新动画
        binding.bluetoothRefreshSrl.isRefreshing = false
    }

    /**
     * 蓝牙设备相关监听方法,传入非null表示扫描到设备，传入null表示扫描时间到
     */
    private fun setDeviceScanListener() {

        bluetoothService?.setBLEScanListener { result: ScanResult? ->
            if (result != null) {
                scanDeviceListAdapter?.addDevice(result)
            } else{
                binding.bluetoothScanIv.clearAnimation()
            }
        }
    }

    /**
     * 连接设备点击事件监听
     */
    private fun setDeviceConnectedListener() {
        binding.bluetoothConnected.setOnClickListener {
            val dialog = BluetoothDeviceFragment()
            dialog.show(supportFragmentManager, "BluetoothDeviceFragment")
        }
        binding.bluetoothMoreIv.setOnClickListener {
            //
        }
    }
    /**
     * 蓝牙设备相关监听方法
     */
    private fun setDeviceConnected(device: BluetoothDevice) {
        //蓝牙设备发现接口方法监听，接口由 BluetoothService 定义，将查找到的设备加入设备列表
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        binding.bluetoothConnectedNameTv.text=device.name
        Log.i("device name",device.name)
        binding.bluetoothConnectedAddressTv.text=device.address
        binding.bluetoothConnectedStatusTv.text="已连接"
        binding.bluetoothConnected.visibility=View.VISIBLE
    }

    /**
     * 由ScanDeviceListAdapter调用
     */
    fun bleDeviceConnect(bluetoothLinkActivity: BluetoothLinkActivity, position: Int) {
        //先获取点击的具体设备
        val device = bluetoothLinkActivity.scanDeviceListAdapter!!.getItem(position).device
        if(connectionDevice?.address==device.address){
//            Toast.makeText(this,"蓝牙已连接",Toast.LENGTH_SHORT).show()
            return
        }
        //更改toolbar状态，正在连接
        MainToolbarViewModel.setBluetoothStatus("连接中", 3)
        //调用BluetoothService的connectToDevice，连接该蓝牙设备
        //该方法有最终有一个回调分支方法onConnectionStateChange，同时此Activity实现了BluetoothService的onBLEStatusInform
        //BLEConnectionListener在onConnectionStateChang执行onBLEStatusInform，回传连接结果
        CoroutineScope(Dispatchers.IO).launch {
            bluetoothService?.connectToDevice(device)
        }
    }
    companion object {

    }
}