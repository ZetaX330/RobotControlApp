package com.example.rcapp;

import android.content.Intent;

import android.os.Bundle;
import android.widget.Toast;

import com.example.rcapp.databinding.ActivityMainBinding;

public class MainActivity extends BleServiceBaseActivity {
    private static final String TAG = "MainActivityLog";
    private ActivityMainBinding binding;
    private final MainFragment mainFragment = new MainFragment();
    private MyToolBar myToolBar;

    @Override
    protected void onBluetoothServiceConnected() {
        //先对本机进行BLE支持检测，如不支持则结束Activity，直接关闭App
        if (!getBluetoothService().isBluetoothSupported()) {
            Toast.makeText(this, "设备不支持BLE", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        //连上服务后获取一遍蓝牙状态，查看是否开启蓝牙
        int state = getBluetoothService().getBluetoothAdapter().getState();
        //执行setBluetoothState，如果蓝牙开启，修改toolbar蓝牙图标为选中状态，否则设为未选中，然后提醒用户打开蓝牙
        setBluetoothState(state);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //initMyToolbar用于设置基类Activity的Toolbar
        initMyToolbar(binding.myToolbar.getId());
        //获取当前自定义toolbar实例
        myToolBar = binding.myToolbar;
        //替换初始Fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(binding.MainPageFragment.getId(), mainFragment)
                .commit();
        //对toolbar的蓝牙图标进行长按监听，成功则跳转BluetoothLinkActivity
        myToolBar.setBluetoothOnLongClickListener(v -> {
            startActivity(new Intent(MainActivity.this, BluetoothLinkActivity.class));
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Activity再次可见时，重新获取一遍toolbar的状态，实际蓝牙状态已经被刷新，该函数只用于刷新toolbar
        myToolBar.initBluetoothStatus();
    }
}