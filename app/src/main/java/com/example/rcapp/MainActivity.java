package com.example.rcapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.rcapp.databinding.ActivityMainBinding;

public class MainActivity extends BleServiceBaseActivity  {
    private static final String TAG = "MainActivityLog";
    private ActivityMainBinding binding;
    private final MainFragment mainFragment=new MainFragment();
    private MyToolBar myToolBar;
    MenuItem item;
    @Override
    protected void onBluetoothServiceConnected() {
        int state=getBluetoothService().getBluetoothAdapter().getState();
        if(state==BluetoothAdapter.STATE_ON){
            myToolBar.setBluetoothStatus(null,1);
        }
        else{
            requestEnableBluetooth();
        }
    }
    // 定义 ActivityResultLauncher，用于处理蓝牙启用请求的结果

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initMyToolbar(binding.myToolbar.getId());
        myToolBar=binding.myToolbar;
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        // 检查设备是否支持 BLE
        if (bluetoothAdapter == null || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(binding.MainPageFragment.getId(), mainFragment)
                .commit();

        myToolBar.setBluetoothOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startActivity(new Intent(MainActivity.this, BluetoothLinkActivity.class));
                return false;
            }
        });
    }
    @Override
    protected void onResume(){
        super.onResume();
        myToolBar.initBluetoothStatus();
    }

}