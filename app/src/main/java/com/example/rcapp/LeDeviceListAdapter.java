package com.example.rcapp;

import static android.Manifest.permission.BLUETOOTH_CONNECT;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;
import android.bluetooth.BluetoothDevice;

public class LeDeviceListAdapter extends ArrayAdapter<BluetoothDevice> {
    private final List<BluetoothDevice> bluetoothList;
    private final Context context;

    public LeDeviceListAdapter(@NonNull Context context, int resource) {
        super(context, resource);
        this.context = context;
        this.bluetoothList = new ArrayList<>();
    }

    public void addDevice(BluetoothDevice device) {
        if (!bluetoothList.contains(device)) {
            bluetoothList.add(device);
            notifyDataSetChanged();
        }
    }

    public void clearList() {
//        bluetoothList.remove(1);
        bluetoothList.clear();
        Log.e("LifeCycle", "scanning1.5");

        notifyDataSetChanged();
        Log.e("LifeCycle", "scanning1.7");

    }

    @Override
    public int getCount() {
        return bluetoothList.size();
    }

    @Override
    public BluetoothDevice getItem(int position) {
        return bluetoothList.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.bluetooth_item_layout, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        BluetoothDevice device = getItem(position);
        if (device != null) {
            if (ActivityCompat.checkSelfPermission(context, BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ((BluetoothLinkActivity) context).requestPermission(BLUETOOTH_CONNECT);
            } else {
                viewHolder.bleNameTv.setText(device.getName() != null ? device.getName() : "N/A");
            }

            viewHolder.bleLinkBtn.setOnClickListener(v -> {
                if (context instanceof BluetoothLinkActivity) {
                    ((BluetoothLinkActivity) context).BLEDeviceConnect(position);
                }
            });
        }

        return convertView;
    }

    private static final class ViewHolder {
        final TextView bleNameTv;
        final TextView bleLinkBtn;

        ViewHolder(View view) {
            bleNameTv = view.findViewById(R.id.bluetooth_name_tv);
            bleLinkBtn = view.findViewById(R.id.bluetooth_link_btn);
        }
    }
}