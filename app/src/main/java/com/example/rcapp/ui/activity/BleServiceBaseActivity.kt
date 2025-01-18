package com.example.rcapp.ui.viewmodel.activity

import android.Manifest.permission
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import com.example.rcapp.R
import com.example.rcapp.databinding.ActivityBleServiceBaseBinding
import com.example.rcapp.data.service.BluetoothService
import com.example.rcapp.data.service.BluetoothService.LocalBinder
import com.example.rcapp.ui.toolbar.MainBluetoothToolbar
import com.example.rcapp.ui.viewmodel.MainToolbarViewModel
/**
 *绑定该基类的Activity，执行时生命周期如下
 *onCreate（基类） -> onCreate -> onResume（基类） -> onResume -> serviceConnection

 *由于MainToolbar采用单例模式（类似于静态类），同时DataBinding提供UI主动更新功能
 *所以无需初始化MainToolbar或主动更新MainToolbar

 *此基类Activity实现了服务绑定，权限请求，Toolbar管理等多个Activity共同需要实现的功能
 */
abstract class BleServiceBaseActivity: AppCompatActivity() {
    var bluetoothService: BluetoothService? = null
    var mainBluetoothToolbar: MainBluetoothToolbar? = null
    private val connectFailed : Int = 4
    private var isBound = false
    //创建一个抽象方法，用于绑定蓝牙Service后ServiceConnection回调中不同Activity的处理
    protected abstract fun onBluetoothServiceConnected()
    private lateinit var binding : ActivityBleServiceBaseBinding
    /**
     * 绑定Service后的回调，使用匿名对象实现 ServiceConnection 接口
     */
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        //绑定上蓝牙Service的情况处理
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.e("LifeCycle","serviceConnectionBase")
            val binder = service as LocalBinder
            bluetoothService = binder.service
            isBound = true
            //传递此基类Activity实例到绑定的蓝牙Service
            bluetoothService!!.setBaseActivity(this@BleServiceBaseActivity)
            //由继承此基类Activity的Activity实现此方法
            onBluetoothServiceConnected()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
        // 蓝牙设备连接接口方法监听，由 BluetoothService 定义
    }
    /**
     * onCreate中进行蓝牙Service的绑定
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBleServiceBaseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestLocationPermission()
        Log.e("LifeCycle","onCreateBase")
        bindBluetoothService()
    }


    override fun onResume() {
        super.onResume()
        bluetoothService?.setBaseActivity(this@BleServiceBaseActivity)
        Log.e("LifeCycle","onResumeBase")
    }

    //Activity销毁时进行Service解绑
    override fun onDestroy() {
        super.onDestroy()
        unbindBluetoothService()
    }
//    protected fun setContentLayout(binding: ViewBinding) {
//        val container = findViewById<FrameLayout>(R.id.base_container)
//        container.addView(binding.root)
//    }

    /**
     * 蓝牙Service绑定
     * serviceConnection为绑定的回调处理
     * BIND_AUTO_CREATE表示Service不存在则自动创建
     */
    private fun bindBluetoothService() {
        //声明绑定对象和发起者
        val intent = Intent(
            this,
            BluetoothService::class.java
        )
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        startService(intent)
    }

    private fun unbindBluetoothService() {
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    /**
     * 蓝牙相关权限请求
     * @permissionRequest 要请求的权限
     * @arrayOf(permissionRequest 可以一次请求多个权限
     *@requestCode 请求标识码
     */
    fun requestPermission(permissionRequest: String) {
        ActivityCompat.requestPermissions(this, arrayOf(permissionRequest), 100)
    }

    /**
     * 蓝牙权限请求回调
     * @permissionRequest 要请求的权限
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != 100)
            return
        // 权限被拒绝
        if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(this, "权限被拒绝", Toast.LENGTH_SHORT).show()
            finishAffinity()
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(
                    permission.ACCESS_FINE_LOCATION,
                    permission.ACCESS_COARSE_LOCATION,
                    permission.ACCESS_BACKGROUND_LOCATION
                ),
                101
            )
        }
    }
    /**
     * 该方法在BluetoothService获得此基类Activity的实例后，由BluetoothService调用
     * BluetoothService接收设备蓝牙状态变化，通过此方法通知此基类Activity更新MainToolbar的蓝牙UI状态并发送Toast
     */
    fun updateBluetoothUI(state: Int,gatt: BluetoothGatt?) {
        runOnUiThread {
            when (state) {
                //蓝牙关闭时
                BluetoothAdapter.STATE_OFF -> {
                    MainToolbarViewModel.setBluetoothStatus(null, 0)
                    requestEnableBluetooth()
                }
                //蓝牙开启时
                BluetoothAdapter.STATE_ON -> {
                    MainToolbarViewModel.setBluetoothStatus(null, 1)
                    //Toast.makeText(this, "蓝牙已开启", Toast.LENGTH_SHORT).show()
                }
                //蓝牙连上时
                BluetoothProfile.STATE_CONNECTED ->{
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestPermission(permission.BLUETOOTH_CONNECT)
                        return@runOnUiThread
                    }
                    MainToolbarViewModel.setBluetoothStatus(gatt!!.device.name, 2)
                    Toast.makeText(this, "连接成功", Toast.LENGTH_SHORT).show()
                }
                //蓝牙断连时
                BluetoothProfile.STATE_DISCONNECTED ->{
                    MainToolbarViewModel.setBluetoothStatus(null, 1)
                    Toast.makeText(this, "连接丢失", Toast.LENGTH_SHORT).show()
                }
                //连接失败时
                connectFailed->{
                    MainToolbarViewModel.setBluetoothStatus(null, 1)
                    Toast.makeText(this, "连接失败", Toast.LENGTH_SHORT).show()
                }


            }
        }
    }

    /**
     * 由于新版API不允许应用打开或关闭设备蓝牙，此Activity接收到蓝牙关闭信息后只做蓝牙打开提醒，需用户手动开启蓝牙
     */
    private fun requestEnableBluetooth() {
        AlertDialog.Builder(this)
            .setTitle("设备蓝牙已关闭")
            .setMessage("请打开蓝牙")
            .setPositiveButton("好的", null)
            .show()
    }

    /**
     * 先检查权限，权限足够则通过MainToolbarViewModel更新UI状态（先更新UI数据）
     */

}