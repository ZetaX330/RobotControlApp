package com.example.rcapp.activity

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rcapp.adapter.LeDeviceListAdapter
import com.example.rcapp.databinding.ActivityBluetoothLinkBinding
import com.example.rcapp.viewmodel.MainToolbarViewModel

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BluetoothLinkActivity : BleServiceBaseActivity() {
    private var binding: ActivityBluetoothLinkBinding? = null
    private var leDeviceListAdapter: LeDeviceListAdapter? = null
    private var leDevice: BluetoothDevice? = null

    /**
     *BluetoothService蓝牙服务绑定回调，在BleServiceBaseActivity中声明的抽象方法，由继承的Activity具体实现
     */
    override fun onBluetoothServiceConnected() {
        //设置蓝牙设备扫描结果监听
        setupDeviceListeners()
        if (bluetoothService?.getBluetoothAdapter()?.isEnabled == true) {
            //先进行一次蓝牙设备扫描
            bluetoothService!!.startScan()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("LifeCycle", "onCreate")
        binding = ActivityBluetoothLinkBinding.inflate(layoutInflater)
        setContentLayout(binding!!)
        initMainToolbar()
        //初始化蓝牙设备列表adapter
        initListAdapter()
        //初始化列表刷新控件
        initSwipeRefresh()
    }

    /**
     * Activity的onResume在服务绑定回调之前执行，所以第一次onResume中getBluetoothService为null
     *当Activity再次可见时，重新扫描获取一次蓝牙设备列表
     * 检查 bluetoothService 是否为 null
     */
    override fun onResume() {
        super.onResume()
        Log.e("LifeCycle", "onResume")
        bluetoothService?.startScan()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e("LifeCycle", "onDestroy")
    }
    private fun initMainToolbar() {
        mainBluetoothToolbar = binding?.myToolbar
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
        leDeviceListAdapter = LeDeviceListAdapter(this)
        //设置列表视图的adapter
        binding!!.bluetoothLv.adapter = leDeviceListAdapter
        //将RecyclerView设置为使用线性布局管理器，以垂直列表的方式显示数据项
        binding!!.bluetoothLv.layoutManager = LinearLayoutManager(this)
    }

    /**
     * 下拉刷新列表监听
     */
    private fun initSwipeRefresh() {
        binding!!.bluetoothRefreshSrl.setOnRefreshListener {
            // 使用Kotlin协程在主线程上延迟执行任务
            lifecycleScope.launch {
                // 延迟500毫秒
                delay(500)
                // 调用refreshDevices
                refreshDevices()
                binding!!.bluetoothRefreshSrl.isRefreshing = false // 停止刷新动画
            }
        }
    }
    /**
     * 刷新蓝牙列表
     */
    private fun refreshDevices() {
        // 停止扫描
        //bluetoothService!!.stopScan()
        // 清空列表
        leDeviceListAdapter!!.clearList()
        // 开启扫描
        bluetoothService!!.startScan()
        // 停止刷新动画
        binding!!.bluetoothRefreshSrl.isRefreshing = false
    }

    /**
     * 蓝牙设备相关监听方法
     */
    private fun setupDeviceListeners() {
        //蓝牙设备发现接口方法监听，接口由 BluetoothService 定义，将查找到的设备加入设备列表
        bluetoothService?.setDeviceFoundListener { result: ScanResult? ->
            if (result != null) {
                leDeviceListAdapter?.addDevice(result)
            }
        }
    }

    /**
     * 定义静态方法bleDeviceConnect
     * 由LeDeviceListAdapter调用，处理连接设备的点击连接事件
     * 不需要获得BluetoothLinkActivity的实例
     */
    companion object {
        fun bleDeviceConnect(bluetoothLinkActivity: BluetoothLinkActivity, position: Int) {
            //先获取点击的具体设备
            bluetoothLinkActivity.leDevice = bluetoothLinkActivity.leDeviceListAdapter!!.getItem(position).device
            //更改toolbar状态，正在连接
            MainToolbarViewModel.setBluetoothStatus("连接中", 3)
            //调用BluetoothService的connectToDevice，连接该蓝牙设备
            //该方法有最终有一个回调分支方法onConnectionStateChange，同时此Activity实现了BluetoothService的onBLEStatusInform
            //BLEConnectionListener在onConnectionStateChang执行onBLEStatusInform，回传连接结果
            bluetoothLinkActivity.bluetoothService!!.connectToDevice(bluetoothLinkActivity.leDevice)
        }
    }
}