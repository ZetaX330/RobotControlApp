package com.example.rcapp.activity

import android.Manifest.permission
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.Image
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCapture.OutputFileResults
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.rcapp.R
import com.example.rcapp.databinding.ActivityCameraBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Request.Builder
import okio.ByteString
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity  : BleServiceBaseActivity() {
    private var binding: ActivityCameraBinding? = null
    private var previewView: PreviewView? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraExecutor: ExecutorService? = null
    private var webSocket: WebSocket? = null
    private var context: Context? = null

    override fun onBluetoothServiceConnected() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        previewView = findViewById(R.id.viewFinder)
        //先设置网络通信接口
        setupWebSocket()

        if (allPermissionsGranted()) {
            //权限足够则开启相机
            startCamera()
        } else {
            //否则请求权限
            requestPermissions()
        }
        //创建一个相机线程
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    //开启相机
    private fun startCamera() {
        /**
         * ListenableFuture 是一个接口，来自 Guava 库，允许异步操作的结果在完成后被监听
         * ProcessCameraProvider是 CameraX 提供的一个类，用于管理相机生命周期和绑定相机用例。
         * context作为参数，是ProcessCameraProvider提供者
         */
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        // 获取 cameraProviderFuture 的异步结果，即ProcessCameraProvider.getInstance(this)的获取结果
        cameraProviderFuture.addListener({
            try {
                // 获取 ProcessCameraProvider 实例，即cameraProvider
                cameraProvider = cameraProviderFuture.get()
                // 绑定相机用例（如预览、拍照）
                bindCameraUseCases()
            } catch (e: ExecutionException) {
                Log.e(TAG, "Error starting camera: ", e)
            } catch (e: InterruptedException) {
                Log.e(TAG, "Error starting camera: ", e)
            }
        }, ContextCompat.getMainExecutor(this)) // 在主线程上执行监听器
    }

    @OptIn(ExperimentalGetImage::class)//实验性API声明
    private fun bindCameraUseCases() {
        //PreviewView 是 UI 中用于显示相机预览的视图
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView!!.surfaceProvider)

        //创建一个图像分析用例，用于处理实时相机帧
        val imageAnalysis = ImageAnalysis.Builder()
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        //设置分析结果处理
        imageAnalysis.setAnalyzer(
            cameraExecutor!!
        ) { imageProxy ->
            //ImageProxy 是图像帧
            //图像帧转化为Image对象
            val image = imageProxy.image
            if (image != null) {
                processImage(image)
            }
            imageProxy.close()
        }
        //创建一个图像捕获用例，用于拍摄照片
        val imageCapture = ImageCapture.Builder().build()
        //解绑先前相机的所有用例
        cameraProvider!!.unbindAll()
        //CameraSelector.DEFAULT_BACK_CAMERA表示选择后置相机
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        //绑定各个用例到相机生命周期
        cameraProvider!!.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, imageCapture)
        //拍照事件监听
        binding?.cameraPhotoBtn?.setOnClickListener {
            takePhoto(imageCapture)
        }
    }

    /**
     * 拍照功能具体实现
     */
    private fun takePhoto(imageCapture: ImageCapture) {
        // 使用当前时间创建一个唯一的文件名
        val name = SimpleDateFormat("yyyyMMdd_msys", Locale.US).format(System.currentTimeMillis())
        // 创建 ContentValues 来存储图片的元数据
        val contentValues = ContentValues().apply {
            // 设置图片的显示名称
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
            // 设置图片的 MIME 类型
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            // 指定图片存储的相对路径（相册目录）
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyApp")
        }

        // 创建 OutputFileOptions，指定内容解析器和存储位置
        val outputOptions = OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        // 调用 takePicture 方法拍摄照片
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                // 当图片成功保存时调用
                override fun onImageSaved(outputFileResults: OutputFileResults) {
                    Toast.makeText(
                        this@CameraActivity,
                        "拍照成功",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // 当拍摄过程中发生错误时调用
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "拍照失败: ", exception) // 记录错误日志
                }
            })
    }

    private fun processImage(image: Image) {
        // 获取图像平面的缓冲区
        val buffer = image.planes[0].buffer
        // 创建一个位图，指定宽度、高度和颜色配置
        val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        // 将缓冲区中的像素数据复制到位图中
        bitmap.copyPixelsFromBuffer(buffer)
        // 将位图发送到服务器进行处理
        sendImageToServer(bitmap)
    }

    private fun sendImageToServer(bitmap: Bitmap) {
        // 创建一个字节数组输出流来存储压缩后的图像数据
        val stream = ByteArrayOutputStream()
        // 将位图压缩为JPEG格式，质量为90%
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        // 将输出流转换为字节数组
        val byteArray = stream.toByteArray()
        // 将字节数组转换为 ByteString，以便通过 WebSocket 发送
        val byteString = ByteString.of(*byteArray)
        // 通过 WebSocket 发送字节数据
        webSocket?.send(byteString)
    }

    //请求摄像机相关权限
    private fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    /**
     * 相机权限请求
     */
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
    }

    /**
     * 相机权限请求回调结果
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    /**
     * 关闭相机线程
     */
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor!!.shutdown()
    }

    //WebSocket设置，与后端通信，传输视频流
    private fun setupWebSocket() {
        val client = OkHttpClient()
        val request: Request = Builder().url(WEBSOCKET_URL).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connection opened")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Message from server: $text")
            }

            //16进制数据接收
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "Message from server: " + bytes.hex())
                // 调用 Service 方法进行数据传输
                if (bluetoothService != null) {
                    val characteristicUUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
                    bluetoothService!!.writeData(characteristicUUID, bytes.toByteArray())
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(
                    TAG,
                    "WebSocket connection closed: $reason"
                )
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error: ", t)
            }
        })
    }
    companion object {
        private const val TAG = "CameraActivity"
        //后端地址端口
        private const val WEBSOCKET_URL = "ws://192.168.31.180:8766"
        //请求编号
        private const val REQUEST_CODE_PERMISSIONS = 10
        //要请求的相机权限
        private val REQUIRED_PERMISSIONS = arrayOf(permission.CAMERA)
    }
}