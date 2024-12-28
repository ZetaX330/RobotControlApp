package com.example.rcapp;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.appbar.AppBarLayout;

public class MyToolBar extends AppBarLayout {

    private Toolbar toolbar;
    private TextView bluetoothNameTv;
    private ImageView bluetoothManageIv;
    private ImageView bluetoothIcBackgroundIv;
    private ImageView loadingIv;

    public MyToolBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.toolbar_layout, this, true);
        toolbar = findViewById(R.id.main_tb);
        bluetoothManageIv = findViewById(R.id.bluetooth_manage_iv);
        bluetoothNameTv = findViewById(R.id.bluetooth_name_tv);
        bluetoothIcBackgroundIv = findViewById(R.id.bluetooth_ic_background_iv);
        loadingIv = findViewById(R.id.loading_iv);
        // 设置初始设备名称
        String savedDeviceName = ToolbarManager.getInstance().getBluetoothDeviceName();
        if (savedDeviceName != null) {
            bluetoothNameTv.setText(savedDeviceName);
        }
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    public void setBluetoothOnLongClickListener(OnLongClickListener listener) {
        bluetoothManageIv.setOnLongClickListener(listener);
    }

    public void setBluetoothStatus(String bluetoothDeviceName, int bluetoothStatus) {
        bluetoothNameTv.setText(bluetoothDeviceName);

        switch (bluetoothStatus) {
            case 2:
                showLoadingAnimation();
                break;
            case 1:
                showConnectedStatus();
                break;
            case 0:
                showDisconnectedStatus();
                break;
        }

        ToolbarManager.getInstance().setBluetoothStatus(bluetoothDeviceName, bluetoothStatus);
    }

    private void showLoadingAnimation() {
        Animation fadeInAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.load_anim);
        bluetoothManageIv.setVisibility(View.INVISIBLE);
        bluetoothIcBackgroundIv.setVisibility(View.INVISIBLE);
        loadingIv.setVisibility(View.VISIBLE);
        loadingIv.startAnimation(fadeInAnimation);
    }

    private void showConnectedStatus() {
        loadingIv.clearAnimation();
        loadingIv.setVisibility(View.INVISIBLE);
        bluetoothManageIv.setVisibility(View.VISIBLE);
        bluetoothIcBackgroundIv.setVisibility(View.VISIBLE);
        bluetoothManageIv.setSelected(true);
    }

    private void showDisconnectedStatus() {
        loadingIv.clearAnimation();
        loadingIv.setVisibility(View.INVISIBLE);
        bluetoothManageIv.setVisibility(View.VISIBLE);
        bluetoothIcBackgroundIv.setVisibility(View.VISIBLE);
        bluetoothManageIv.setSelected(false);
    }

    public void initBluetoothStatus() {
        String deviceName = ToolbarManager.getInstance().getBluetoothDeviceName();
        int bluetoothStatus = ToolbarManager.getInstance().getBluetoothStatus();
        bluetoothNameTv.setText(deviceName);

        if (bluetoothStatus == 0) {
            bluetoothManageIv.setSelected(false);
        }
        else if (bluetoothStatus == 1) {
            bluetoothManageIv.setSelected(true);
        }
    }
}