package com.example.rcapp.adapter

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.rcapp.activity.BluetoothLinkActivity
import com.example.rcapp.databinding.ScanDeviceItemLayoutBinding

class LeDeviceListAdapter(private val context: Context) :
    RecyclerView.Adapter<LeDeviceListAdapter.ViewHolder>() {

    private val bluetoothList: MutableList<ScanResult> =
        ArrayList()
    private val seenDevices : MutableList<String> =
        ArrayList()

    /**
     * 添加设备到列表中，bluetoothList.size-1为最新的设备下标号
     */
    @SuppressLint("MissingPermission")
    fun addDevice(result: ScanResult) {
        val deviceAddress = result.device.address
        if (!seenDevices.contains(deviceAddress)) {
            bluetoothList.add(result)
            seenDevices.add(deviceAddress)
            notifyItemInserted((bluetoothList.size - 1))
        }
    }
    fun clearList() {
        val size = bluetoothList.size
        bluetoothList.clear()
        seenDevices.clear()
        notifyItemRangeRemoved(0, size)
    }

    override fun getItemCount(): Int {
        return bluetoothList.size
    }

    fun getItem(position: Int): ScanResult {
        return bluetoothList[position]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // 使用View Binding替代findViewById
        val inflater = LayoutInflater.from(context)
        val binding = ScanDeviceItemLayoutBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = getItem(position)
        result.device
        // 检查BLUETOOTH_CONNECT权限
        if (ActivityCompat.checkSelfPermission(context, permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            (context as BluetoothLinkActivity).requestPermission(permission.BLUETOOTH_CONNECT)
            return
        }
        holder.binding.bluetoothNameTv.text=result.device.name
        holder.binding.bluetoothAddressTv.text=result.device.address
        //holder.binding.bluetoothAddressTv.text=result.rssi.toString()+" dBm"
        holder.binding.executePendingBindings()
        holder.binding.root.setOnClickListener{
            if (context is BluetoothLinkActivity) {
                BluetoothLinkActivity.bleDeviceConnect(context, position)
            }
        }
        // 设置按钮点击事件
//        holder.binding.bluetoothLinkBtn.setOnClickListener {
//            if (context is BluetoothLinkActivity) {
//                BluetoothLinkActivity.bleDeviceConnect(context, position)
//            }
//        }
    }

    // ViewHolder继承自父类RecyclerView.ViewHolder
    class ViewHolder// super用于调用父类RecyclerView.ViewHolder的构造函数，将视图传递给它
    // 构造函数，接受一个 Binding参数
    internal constructor(val binding: ScanDeviceItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)
}