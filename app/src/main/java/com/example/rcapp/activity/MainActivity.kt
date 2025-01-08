package com.example.rcapp.activity

import android.os.Bundle
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.rcapp.databinding.ActivityMainBinding
import com.example.rcapp.fragment.RobotMainFragment

class MainActivity : BleServiceBaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private val robotMainFragment = RobotMainFragment()
    private var navController:NavController ? = null
    override fun onBluetoothServiceConnected() {
        //先对本机进行BLE支持检测，如不支持则结束Activity，直接关闭App
        if (!bluetoothService!!.isBluetoothSupported) {
            Toast.makeText(this, "设备不支持BLE", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        //连上服务后获取一遍蓝牙状态，查看是否开启蓝牙
        //变量或方法后加!!代表为null则抛出空指针错误，加?代表为null则返回
        val state = bluetoothService!!.getBluetoothAdapter()?.state
        //执行setBluetoothState，如果蓝牙开启，修改toolbar蓝牙图标为选中状态，否则设为未选中，然后提醒用户打开蓝牙
        if (state != null) {
            setBluetoothStatus(state)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        //setContentView(binding!!.root)
        setContentLayout(binding)
        initMainNav()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
        //初始化MainToolbar
    private fun initMainNav() {
        // 初始化 Toolbar
        binding.myToolbar.let { toolbar ->
            mainBluetoothToolbar=toolbar
            setSupportActionBar(toolbar.toolbar) // 设置 Toolbar
            toolbar.setViewModel()              // 绑定 ViewModel
        }
        // 初始化 BottomNavigationView 并绑定 NavController
        val navHostFragment = supportFragmentManager.findFragmentById(binding.navHostFragment.id) as NavHostFragment
        val navController = navHostFragment.navController
        binding.mainBottomNav.setupWithNavController(navController)
    }




    companion object {
        private const val TAG = "MainActivityLog"
    }
}