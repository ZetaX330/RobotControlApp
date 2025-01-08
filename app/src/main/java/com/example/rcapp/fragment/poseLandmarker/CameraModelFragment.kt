package com.example.rcapp.fragment.poseLandmarker

import PoseSettingViewModelFactory
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
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
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.rcapp.R
import com.example.rcapp.databinding.FragmentCameraModelBinding
import com.example.rcapp.model.PoseLandmarkerHelper
import com.example.rcapp.viewmodel.PoseCameraViewModel
import com.example.rcapp.viewmodel.PoseSettingViewModel
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCameraModelBinding.inflate(layoutInflater)

        poseCameraViewModel=ViewModelProvider(requireActivity())[PoseCameraViewModel::class.java]
        poseCameraViewModel.isCameraOn.observe(viewLifecycleOwner) { isCameraOn ->
            when(isCameraOn){
                false -> {
                    unbindImageAnalyzer()
                    binding.cameraTimeChr.stop()
                    binding.cameraTimeChr.base = SystemClock.elapsedRealtime()
                }
                true -> {
                    bindImageAnalyzer()
                    binding.cameraTimeChr.start()
                }
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backgroundExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        binding.cameraPreview.post {
            // Set up the camera and its use cases
            setUpCamera()
            createImageAnalyzer()
        }

        // Create the PoseLandmarkerHelper that will handle the inference
        backgroundExecutor.execute {
            poseLandmarkerHelper = PoseLandmarkerHelper(
                currentModel = PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL,
                currentDelegate = PoseLandmarkerHelper.DELEGATE_CPU,
                runningMode = RunningMode.LIVE_STREAM,
                context = requireContext(),
                poseLandmarkerHelperListener = this
            )
            val factory = PoseSettingViewModelFactory(poseLandmarkerHelper)
            poseSettingViewModel = ViewModelProvider(this, factory)[PoseSettingViewModel::class.java]
        }

    }

    override fun onResume() {
        super.onResume()
        binding.plcSettingIv.setOnClickListener {
            showFragment()
        }
    }

    override fun onPause() {
        super.onPause()
        if(this::poseLandmarkerHelper.isInitialized) {
            poseSettingViewModel.setDetectionConfidence(poseLandmarkerHelper.detectionConfidence)
            poseSettingViewModel.setTrackingConfidence(poseLandmarkerHelper.trackingConfidence)
            poseSettingViewModel.setPresenceConfidence(poseLandmarkerHelper.presenceConfidence)
            poseSettingViewModel.setDelegate(poseLandmarkerHelper.currentDelegate)

            // Close the PoseLandmarkerHelper and release resources
            backgroundExecutor.execute { poseLandmarkerHelper.clearPoseLandmarker() }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
    }

    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext())
        // 异步获取cameraProvider
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
            // 获取主线程的执行器，确保摄像头操作在主线程上执行
        )
    }

    private fun createImageAnalyzer() {
    imageAnalyzer =
        ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(binding.cameraPreview.display.rotation)
            //设置背压策略为 "仅保留最新帧"
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            // The analyzer can then be assigned to the instance
            .also {
                //设置分析器，指定在后台线程（backgroundExecutor）上运行
                it.setAnalyzer(backgroundExecutor) { image ->
                    //检测姿态
                    detectPose(image)
                }
            }
    }
    private fun bindImageAnalyzer() {
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        try {
            cameraProvider.bindToLifecycle(
                this, CameraSelector.DEFAULT_BACK_CAMERA, imageAnalyzer
            )
        } catch (exc: Exception) {
            Log.e(TAG, "ImageAnalyzer binding failed", exc)
        }
    }
    private fun unbindImageAnalyzer() {
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        cameraProvider.unbind(imageAnalyzer)
    }
    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        //选择前后置摄像头
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        // 预览视图设置
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            //设置目标宽高比为 4:3
            //设置目标旋转角度，与设备显示器的旋转方向一致
            .setTargetRotation(binding.cameraPreview.display.rotation)
            .build()

        //解绑相机先前用例
        cameraProvider.unbindAll()

        //绑定新的用力到相机
        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview
            )

            // Attach the viewfinder's surface provider to preview use case
            // 输出预览视频到view
            preview?.surfaceProvider = binding.cameraPreview.surfaceProvider
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectPose(imageProxy: ImageProxy) {
        // 检查 poseLandmarkerHelper 是否已初始化
        if(this::poseLandmarkerHelper.isInitialized) {
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
            if (isAdded) {
                val inferenceTime = resultBundle.inferenceTime
                binding.inferenceTimeVal.text = inferenceTime.toString()

                // Pass necessary information to OverlayView for drawing on the canvas
                binding.overlay.setResults(
                    resultBundle.results.first(),
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    RunningMode.LIVE_STREAM
                )

                // Force a redraw
                binding.overlay.invalidate()
            }
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(activity, error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFragment() {
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

        binding.fragmentContainer.visibility = View.VISIBLE
        binding.fragmentContainer.translationY = -binding.fragmentContainer.height.toFloat()
        binding.fragmentContainer.animate()
            .translationY(0f)
            .setDuration(300)
            .start()
    }

    companion object {
        private const val TAG = "CameraFragment"

    }
}