package com.example.rcapp.ui.activity

import android.Manifest.permission
import android.content.pm.PackageManager

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider

import com.example.rcapp.R
import com.example.rcapp.databinding.ActivityCameraBinding
import com.example.rcapp.ui.fragment.poseLandmarker.CameraModelFragment
import com.example.rcapp.ui.viewmodel.PoseCameraViewModel
import com.example.rcapp.ui.viewmodel.RegisterViewModel
import com.example.rcapp.util.InstructionHelper
import com.example.rcapp.util.PermissionConstants.REQUEST_CODE_CAMERA_AND_STORAGE
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.UUID

class CameraActivity  : BLEServiceBaseActivity(),CameraModelFragment.SkeletalListener {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var viewModel: PoseCameraViewModel
    private var t:Boolean=true
    private lateinit var instructionFile: File
    private var bufferedWriter: BufferedWriter? = null

    override fun onBluetoothServiceConnected() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCameraBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(binding.root.id)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[PoseCameraViewModel::class.java]
        supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.running_model_fragment, CameraModelFragment())
                    .commit()
        requestPermission()
        bluetoothState.observe(this) { isBluetoothOn ->
            // 处理蓝牙状态变化
            if (isBluetoothOn) {
                // 蓝牙已打开
                Toast.makeText(this, "Bluetooth is ON", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.setCameraStatus(false)

            }
        }
        binding.cameraModelSwitchIv.setOnClickListener {
            //设置该模式的viewModel中相机状态
            viewModel.setCameraStatus(!binding.cameraModelSwitchIv.isSelected)
            //更新图标ui
            binding.cameraModelSwitchIv.isSelected=!binding.cameraModelSwitchIv.isSelected
        }
        binding.cameraFlipIv.setOnClickListener{
            viewModel.setCameraSelector(!t)
            t=!t
        }
        binding.testBtn.setOnClickListener {
            val ins="5555080301e80301e803"
            sendDataToDevice(ins)
        }


    }
    fun setVideoAndInsFile(textFile:File){
        instructionFile = textFile
        // 打开 BufferedWriter
        try {
            bufferedWriter = BufferedWriter(FileWriter(instructionFile, true)) // true 表示追加模式
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    override fun onSkeletalReceived(poseLandmarkerResults: PoseLandmarkerResult) {
        val instruction=InstructionHelper.instructionCreate(poseLandmarkerResults)
        if(instruction!="")
            Log.d("CameraActivity",instruction)
        // 写入数据到文件
//        try {
//            bufferedWriter?.let {
//                it.write(instruction)
//                it.newLine() // 换行
//                it.flush()   // 立即刷新缓冲区
//            }
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
        //sendDataToDevice(instruction)
    }
    private fun sendDataToDevice(dataHex: String) {
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
            val hexDebug = bytesToHex(data)
            Log.d("CameraActivity", "发送数据 (HEX): $hexDebug")
            bluetoothService!!.writeData(characteristicUUID, data)

        }
    }
    private fun bytesToHex(bytes: ByteArray): String {
        val hexString = StringBuilder()
        for (b in bytes) {
            // 将字节转换为无符号整数（处理负数）
            val unsignedByte = b.toInt() and 0xFF
            // 格式化为两位十六进制，不足补零
            hexString.append(String.format("%02X", unsignedByte))
        }
        return hexString.toString()
    }
    private fun requestPermission(){
        if (ContextCompat.checkSelfPermission(this, permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    permission.CAMERA,
                    permission.READ_EXTERNAL_STORAGE,
                    permission.WRITE_EXTERNAL_STORAGE
                ),
                REQUEST_CODE_CAMERA_AND_STORAGE // 使用不同的请求码
            )
        }
    }
    companion object {
        private const val TAG = "CameraActivity"

    }




}