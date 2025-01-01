package com.example.rcapp.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.rcapp.fragment.MainFragment
import com.example.rcapp.toolbar.MainToolbar
import com.example.rcapp.databinding.ActivityMainBinding

class MainActivity : BleServiceBaseActivity() {
    private var binding: ActivityMainBinding? = null
    private val mainFragment = MainFragment()
    private var mainToolbar: MainToolbar? = null

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
        setContentView(binding!!.root)
        //initMyToolbar用于设置基类Activity的Toolbar
        initMainToolbar(binding!!.myToolbar.id)
        //获取当前自定义toolbar实例
        mainToolbar = binding!!.myToolbar
        //替换初始Fragment
        supportFragmentManager
            .beginTransaction()
            .replace(binding!!.MainPageFragment.id, mainFragment)
            .commit()
        //对toolbar的蓝牙图标进行长按监听，成功则跳转BluetoothLinkActivity
        mainToolbar!!.setBluetoothOnLongClickListener {
            startActivity(
                Intent(
                    this@MainActivity,
                    BluetoothLinkActivity::class.java
                )
            )
            //Lambda 表达式的最后一个表达式就是它的返回值
            true
        }
    }


    companion object {
        private const val TAG = "MainActivityLog"
    }
}