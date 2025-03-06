package com.example.rcapp.ui.adapter

import android.Manifest.permission
import android.content.Context
import android.content.pm.PackageManager
import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.rcapp.ui.activity.BluetoothLinkActivity
import com.example.rcapp.databinding.ItemListScanDeviceBinding

class ScanDeviceListAdapter(private val context: Context) :
    RecyclerView.Adapter<ScanDeviceListAdapter.ViewHolder>() {

    private val bluetoothList: MutableList<ScanResult> =
        mutableListOf()
    private val addressList : MutableList<String> =
        mutableListOf()
    /**
     * 添加设备到列表中，bluetoothList.size-1为最新的设备下标号
     * 由于列表项是ScanResult，还需要再声明一个addressList存储设备硬件地址，防止同一设备多次加入bluetoothList
     * 循环判断bluetoothList中的硬件地址也是一种方式，ArrayList的contains时间复杂度是O（n）
     */
    fun addDevice(result: ScanResult) {
        val deviceAddress = result.device.address

        if (!addressList.contains(deviceAddress)) {
            bluetoothList.add(result)
            addressList.add(deviceAddress)
            notifyItemInserted((bluetoothList.size - 1))
        }
    }


    /**
     * 清除bluetoothList的同时还要清除addressList
     */
    fun clearList() {
        val size = bluetoothList.size
        bluetoothList.clear()
        addressList.clear()
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
        val binding = ItemListScanDeviceBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = getItem(position)
        result.device
        // 检查BLUETOOTH_CONNECT权限
        if (ActivityCompat.checkSelfPermission(context, permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            (context as BluetoothLinkActivity).requestBluetoothPermission(permission.BLUETOOTH_CONNECT)
            return
        }
        //设置设备名
        holder.binding.bluetoothScanNameTv.text=result.device.name
        //设置设备硬件地址
        holder.binding.bluetoothScanAddressTv.text=result.device.address
        //设置设备信号强度
        holder.binding.bluetoothScanRssiTv.text= String.format("%s dBm",result.rssi.toString())
        //executePendingBindings()用于快速更新视图
        holder.binding.executePendingBindings()
        //设置点击连接事件监听
        holder.binding.root.setOnClickListener{
            if (context is BluetoothLinkActivity) {
                //交给BluetoothLinkActivity处理
                context.bleDeviceConnect(context, position)
            }
        }

    }

    // ViewHolder继承自父类RecyclerView.ViewHolder
    class ViewHolder
    // 构造函数，接受一个 Binding参数
    internal constructor(val binding: ItemListScanDeviceBinding) :
        RecyclerView.ViewHolder(binding.root)
}