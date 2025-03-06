package com.example.rcapp.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.rcapp.databinding.ActivityMainBinding

class MainActivity : BLEServiceBaseActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onBluetoothServiceConnected() {
        //先对本机进行BLE支持检测，如不支持则结束Activity，直接关闭App
        if (!bluetoothService?.isBluetoothSupported!!) {
            Toast.makeText(this, "设备不支持BLE", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        //连上服务后获取一遍蓝牙状态，查看是否开启蓝牙
        //变量或方法后加!!代表为null则抛出空指针错误，加?代表为null则返回
        val state = bluetoothService!!.getBluetoothAdapter()?.state
        //执行setBluetoothState，如果蓝牙开启，修改toolbar蓝牙图标为选中状态，否则设为未选中，然后提醒用户打开蓝牙
        if (state != null) {
            updateBluetoothState(state,null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        initMainNav()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
        //初始化 MainToolbar
    private fun initMainNav() {
        // 初始化 Toolbar

        // 初始化 BottomNavigationView 并绑定 NavController
        // 注意！当navHostFragment的容器为 FragmentContainerView时，navController的获取有所不同，即当前使用的方法
        // 需要先获取navHostFragment的实例，通过navHostFragment实例获取navController
        val navHostFragment = supportFragmentManager.findFragmentById(binding.navHostFragment.id) as NavHostFragment
        val navController = navHostFragment.navController
        binding.mainBottomNav.setupWithNavController(navController)
    }




    companion object {
        private const val TAG = "MainActivityLog"
    }
}