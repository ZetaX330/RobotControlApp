package com.example.rcapp.fragment

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rcapp.data.service.BluetoothService

import com.example.rcapp.databinding.FragmentMainBinding
import com.example.rcapp.model.InstructionSend
import com.example.rcapp.ui.adapter.InsSendListAdapter
import com.example.rcapp.ui.viewmodel.activity.BleServiceBaseActivity
import com.example.rcapp.ui.activity.PoseLandmarkerActivity
import java.time.LocalTime
import java.util.UUID

/**
 * 暂时不用该fragment
 */
class MainFragment : Fragment() {
    private var binding: FragmentMainBinding? = null
    private var bluetoothService: BluetoothService? = null
    private var insSendListAdapter: InsSendListAdapter? = null
    private var context: Context? = null

    // 绑定服务时的回调接口
    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context
        if (context is BleServiceBaseActivity) {
            bluetoothService = context.bluetoothService
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        initListAdapter()
        return binding!!.root // 返回根视图
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding!!.CameraGoBtn.setOnClickListener {
            startActivity(
                Intent(
                    activity,
                    PoseLandmarkerActivity::class.java
                )
            )
        }

        binding!!.dataSendBtn.setOnClickListener {
            val hexString = binding!!.dataSendEt.text.toString().trim { it <= ' ' }.replace(" ", "")
            if (hexString.isNotEmpty()) {
                val dataHex = binding!!.dataSendEt.text.toString()
                //sendDataToDevice(dataHex)
                // 添加新的数据行
                val currentTime = LocalTime.now()
                val hour = currentTime.hour
                val minute = currentTime.minute
                val second = currentTime.second
                val instruction = InstructionSend("$hour:$minute:$second", dataHex)
                insSendListAdapter!!.addIns(instruction)

            }
        }

//        binding!!.angleAsb.setOnAngleChangeListener { angle: Float ->
//            // 创建 DecimalFormat 实例，指定格式
//            val df = DecimalFormat("#.00")
//            val formattedAngle = df.format(angle.toDouble())
//            binding!!.angleTv.text = String.format("%s°", formattedAngle)
//            sendDataToDevice(formattedAngle)
//            // 添加新的数据行
//            val currentTime = LocalTime.now()
//            val hour = currentTime.hour
//            val minute = currentTime.minute
//            val second = currentTime.second
//            val instruction = InstructionSend("$hour:$minute:$second",formattedAngle)
//            insSendListAdapter!!.addIns(instruction)
//            binding!!.dataSendShowRv.post {
//                binding!!.dataSendShowRv.smoothScrollToPosition(
//                    insSendListAdapter!!.itemCount - 1
//                )
//            }
//        }
    }

    private fun initListAdapter() {
        // 初始化发送指令列表adapter，参数为context
        insSendListAdapter = InsSendListAdapter(getContext())
        // 设置列表视图的adapter
        binding!!.dataSendShowRv.adapter = insSendListAdapter
        binding!!.dataSendShowRv.layoutManager = LinearLayoutManager(getContext())
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun sendDataToDevice(dataHex: String) {
        bluetoothService = (context as BleServiceBaseActivity).bluetoothService
        // 调用 Service 方法进行数据传输
        if (bluetoothService != null ) {
            val characteristicUUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
            val len = dataHex.length
            // 数据字节数等于字符数/2,一个16进制字符为4位，即半字节
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                val firstDigit = dataHex[i].digitToIntOrNull(16) ?: -1
                val secondDigit = dataHex[i + 1].digitToIntOrNull(16) ?: -1
                require(!(firstDigit == -1 || secondDigit == -1)) { "Invalid hex string" }
                data[i / 2] = ((firstDigit shl 4) + secondDigit).toByte()
                i += 2
            }
            bluetoothService!!.writeData(characteristicUUID, data)
        }
    }
}