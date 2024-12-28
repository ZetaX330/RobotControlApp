package com.example.rcapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.rcapp.databinding.FragmentMainBinding;

import java.text.DecimalFormat;
import java.util.UUID;
public class MainFragment extends Fragment {
    private FragmentMainBinding binding;
    protected BluetoothService bluetoothService;

    // 绑定服务时的回调接口
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        bluetoothService = ((BleServiceBaseActivity) context).getBluetoothService();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMainBinding.inflate(inflater, container, false);
        return binding.getRoot(); // 返回根视图
    }
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.CameraGoBtn.setOnClickListener(v -> startActivity(new Intent(getActivity(), CameraActivity.class)));
        binding.dataSendBtn.setOnClickListener(v -> {
            String hexString = binding.dataSendEt.getText().toString().trim().replace(" ", "");
            if (!hexString.isEmpty()) {
                String dataHex = binding.dataSendEt.getText().toString();
                SendDataToDevice(dataHex);
            }
        });
        binding.angleAsb.setOnAngleChangeListener(angle -> {
            // 创建 DecimalFormat 实例，指定格式
            DecimalFormat df = new DecimalFormat("#.00");
            String formattedAngle = df.format(angle);
            binding.angleTv.setText(String.format("%s°", formattedAngle));

        });
    }
    private void SendDataToDevice(String dataHex){
        // 调用 Service 方法进行数据传输
        if (bluetoothService != null&&bluetoothService.getServiceStatus()) {
            UUID characteristicUUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
            int len = dataHex.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                int firstDigit = Character.digit(dataHex.charAt(i), 16);
                int secondDigit = Character.digit(dataHex.charAt(i + 1), 16);
                if (firstDigit == -1 || secondDigit == -1) {
                    throw new IllegalArgumentException("Invalid hex string");
                }
                data[i / 2] = (byte) ((firstDigit << 4) + secondDigit);
            }
            bluetoothService.writeData(characteristicUUID, data);
        }
    }

}