package com.example.rcapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import android.bluetooth.BluetoothDevice;

public class LeDeviceListAdapter extends ArrayAdapter<BluetoothDevice> {
    private final List<BluetoothDevice> bluetoothlist; // 可用于存储设备列表
    private final Context context;
    public LeDeviceListAdapter(@NonNull Context context, int resource) {
        super(context, resource);
        this.context=context;
        this.bluetoothlist = new ArrayList<>(); // 初始化设备列表
    }

    public void addDevice(BluetoothDevice device) {
        if (!bluetoothlist.contains(device)) {
            bluetoothlist.add(device); // 添加设备到列表
            notifyDataSetChanged(); // 通知适配器数据已更改
        }
    }

    @Override
    public int getCount() {
        return bluetoothlist.size(); // 返回设备数量
    }

    @Override
    public BluetoothDevice getItem(int position) {
        return bluetoothlist.get(position); // 获取特定位置的设备
    }

    @SuppressLint("MissingPermission")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;
        // 检查 convertView 是否为 null，以重用已存在的视图
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.bluetooth_item_layout, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder); // 将ViewHolder与convertView绑定
        } else {
            viewHolder = (ViewHolder) convertView.getTag(); // 重用已存在的ViewHolder
        }

        // 获取当前设备并设置到视图
        BluetoothDevice device = getItem(position);
        BluetoothService.checkBluetoothPermissions((Activity)context );
        assert device != null;
        if (device.getName() != null) {
            viewHolder.BleNameTv.setText(device.getName());
        } else {
            viewHolder.BleNameTv.setText("N/A");
        }
        viewHolder.BleLinkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (context instanceof BluetoothLinkActivity) {
                    ((BluetoothLinkActivity) context).BLEDeviceConnect(position);
                }
            }
        });
        return convertView; // 返回视图
    }

    // ViewHolder 类
    private static final class ViewHolder {
        TextView BleNameTv;
        TextView BleLinkBtn;
        ViewHolder(View view) {
            BleNameTv = view.findViewById(R.id.bluetooth_name_tv); // 初始化控件对象
            BleLinkBtn=view.findViewById(R.id.bluetooth_link_btn);
        }
    }
}
