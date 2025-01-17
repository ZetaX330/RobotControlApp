package com.example.rcapp.ui.fragment.poseLandmarker

import com.example.rcapp.ui.viewmodel.PoseSettingViewModelFactory
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.rcapp.R
import com.example.rcapp.databinding.FragmentCameraModelBinding
import com.example.rcapp.model.PoseLandmarkerHelper
import com.example.rcapp.ui.viewmodel.PoseCameraViewModel
import com.example.rcapp.ui.viewmodel.PoseSettingViewModel
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 注意！暂时未动态申请相机权限
 * 三种识别模式下，每种模式分别有一个Control Fragment和一个Model Fragment
 * Control负责更新ViewModel实现控制功能，Model通过观察ViewModel更新UI
 */
class CameraModelFragment : Fragment() , PoseLandmarkerHelper.LandmarkerListener {
    private lateinit var binding: FragmentCameraModelBinding
    private lateinit var poseSettingViewModel: PoseSettingViewModel
    private lateinit var poseCameraViewModel: PoseCameraViewModel


    lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private val poseLandmarkerSettingFragment = PoseLandmarkerSettingFragment()
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_BACK
    private lateinit var backgroundExecutor: ExecutorService
    private var isDetectPose = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG,"onCreate")

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(TAG,"onCreateView")
        binding = FragmentCameraModelBinding.inflate(layoutInflater)
        //获得PoseCameraViewModel，由于要和CameraControlFragment交互，通过父Activity获取ViewModel
        poseCameraViewModel=ViewModelProvider(requireActivity())[PoseCameraViewModel::class.java]
        //观察相机状态的改变
        poseCameraViewModel.isCameraOn.observe(viewLifecycleOwner) { isCameraOn ->
            when(isCameraOn){
                false -> {
                    binding.overlay.clear()
                    //相机关闭时，停止检测
                    isDetectPose.set(false)
                    //停止相机计时器，重置计时
                    binding.cameraTimeChr.stop()
                    binding.cameraTime.visibility=View.INVISIBLE
                    handler.removeCallbacksAndMessages(null)
//                    binding.cameraTimeChr.base = SystemClock.elapsedRealtime()

                }
                true -> {
                    binding.overlay.initPaints()
                    isDetectPose.set(true)
                    binding.cameraTimeChr.base = SystemClock.elapsedRealtime()
                    binding.cameraTime.visibility=View.VISIBLE
                    binding.cameraTimeChr.start()
                    startCameraTracking()
                }
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG,"onViewCreated")
        backgroundExecutor = Executors.newSingleThreadExecutor()

        //post确定cameraPreview设置好后进行setUpCamera
        binding.cameraPreview.post {
            setUpCamera()
        }

        //在后台线程创建该模式下对应poseLandmarkerHelper实例
        backgroundExecutor.execute {
            poseLandmarkerHelper = PoseLandmarkerHelper(
                //识别模型默认为FULL
                currentModel = PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL,
                //委托模式默认为CPU
                currentDelegate = PoseLandmarkerHelper.DELEGATE_CPU,
                //相机模式下为LIVE_STREAM
                runningMode = RunningMode.LIVE_STREAM,
                context = requireContext(),
                poseLandmarkerHelperListener = this
            )
            //将poseLandmarkerHelper实例当作参数传递给poseSettingViewModel
            val factory = PoseSettingViewModelFactory(poseLandmarkerHelper)
            poseSettingViewModel = ViewModelProvider(this, factory)[PoseSettingViewModel::class.java]
        }

    }

    override fun onStart() {
        super.onStart()
        Log.i(TAG,"onStart")
    }
    override fun onResume() {
        super.onResume()
        Log.i(TAG,"onResume")
        //poseLandmarkerSettingFragment的呼出监听
        binding.plcSettingIv.setOnClickListener {
            showFragment()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG,"onPause")
//        if(this::poseLandmarkerHelper.isInitialized) {
//            //暂时不清楚为什么要重新设置一遍参数
//            poseSettingViewModel.setDetectionConfidence(poseLandmarkerHelper.detectionConfidence)
//            poseSettingViewModel.setTrackingConfidence(poseLandmarkerHelper.trackingConfidence)
//            poseSettingViewModel.setPresenceConfidence(poseLandmarkerHelper.presenceConfidence)
//            poseSettingViewModel.setDelegate(poseLandmarkerHelper.currentDelegate)
//
//            backgroundExecutor.execute { poseLandmarkerHelper.clearPoseLandmarker() }
//        }
    }
    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG,"onDestroy")
        //关闭后台线程
        backgroundExecutor.shutdown()
        //确保后台任务全部完成
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
    }
    private fun startCameraTracking() {
        val runnable = object : Runnable {
            override fun run() {
                binding.cameraTickingIv.visibility = if (!binding.cameraTickingIv.isVisible) View.VISIBLE else View.INVISIBLE

                handler.postDelayed(this, 500) // 1000 milliseconds = 1 second
            }
        }

        // Start the task
        handler.post(runnable)
    }
    private fun setUpCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext())
        // 异步获取cameraProvider
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()

                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
            // 获取主线程的执行器，确保摄像头操作在主线程上执行
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        //选择前后置摄像头
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing).build()
        // 创建 ResolutionSelector
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(AspectRatio.RATIO_4_3,
                    AspectRatioStrategy.FALLBACK_RULE_AUTO)//参数2为备选比例
            ).build()
        // 预览视图设置
        preview = Preview.Builder().
            setResolutionSelector(resolutionSelector)
            //设置目标旋转角度，与设备显示器的旋转方向一致
            .setTargetRotation(binding.cameraPreview.display.rotation)
            .build()
        val imageAnalyzer = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(binding.cameraPreview.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(backgroundExecutor) { image ->
                    image.use { processedImage ->
                        if (isDetectPose.get()) {
                            Log.d("ImageAnalyzer", "Detecting pose")
                            detectPose(processedImage)
                        } else {
                            Log.d("ImageAnalyzer", "No Detecting pose")
                        }
                    }
                }
            }
        //解绑相机先前用例
        cameraProvider.unbindAll()

        //绑定新的用例到相机
        try {

            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview,imageAnalyzer
            )

            // 输出预览视频到view
            preview?.surfaceProvider = binding.cameraPreview.surfaceProvider
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    /**
     * 检测姿态
     */
    private fun detectPose(imageProxy: ImageProxy) {
        // 检查 poseLandmarkerHelper 是否已初始化
        if(this::poseLandmarkerHelper.isInitialized) {
            //两个参数，图像帧和相机是否前置
            poseLandmarkerHelper.detectLiveStream(
                imageProxy = imageProxy,
                isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
            )
        }
    }

    //摄像头旋转后对分析用例的更新
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            binding.cameraPreview.display.rotation
    }

    // Update UI after pose have been detected. Extracts original
    // image height/width to scale and place the landmarks properly through
    // OverlayView
    // 姿态识别回调
    override fun onResults(
        resultBundle: PoseLandmarkerHelper.ResultBundle
    ) {
        activity?.runOnUiThread {
            //isAdded确保回调处理时fragment没有被移除
            if (isAdded) {
                //更新延迟时间
                val inferenceTime = resultBundle.inferenceTime
                binding.inferenceTimeVal.text = inferenceTime.toString()
                if(isDetectPose.get()){
                    //更新骨架图
                    binding.overlay.setResults(
                        resultBundle.results.first(),
                        resultBundle.inputImageHeight,
                        resultBundle.inputImageWidth,
                        RunningMode.LIVE_STREAM
                    )
                    binding.overlay.invalidate()
                }
            }
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(activity, error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFragment() {
        //先设置poseLandmarkerHelper当前的参数
        poseLandmarkerSettingFragment.setPoseLandmarkerSettingChangedListener{
            backgroundExecutor.execute {
                poseLandmarkerHelper.clearPoseLandmarker()
                poseLandmarkerHelper.setupPoseLandmarker()
            }
            binding.overlay.clear()

        }
        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, poseLandmarkerSettingFragment)
            .commit()
        //设置poseLandmarkerSettingFragment的容器可见,添加呼出时的动画效果
        binding.fragmentContainer.visibility = View.VISIBLE
        binding.fragmentContainer.animate()
            .translationY(0f)
            .setDuration(300)
            .start()
    }

    companion object {
        private const val TAG = "CameraFragment"

    }
}