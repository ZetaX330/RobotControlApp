package com.example.rcapp.ui.fragment.poseLandmarker

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import com.example.rcapp.ui.viewmodel.PoseSettingViewModelFactory
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.rcapp.R
import com.example.rcapp.data.database.UserDatabaseHelper
import com.example.rcapp.databinding.FragmentCameraModelBinding
import com.example.rcapp.ui.activity.CameraActivity
import com.example.rcapp.util.PoseLandmarkerHelper
import com.example.rcapp.ui.viewmodel.PoseCameraViewModel
import com.example.rcapp.ui.viewmodel.PoseSettingViewModel
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max
import kotlin.properties.Delegates

/**
 * 相机及存储相关权限在父Activity中申请
 * Control负责更新ViewModel，Model通过观察ViewModel更新UI，接受控制
 */
class CameraModelFragment() : Fragment(), PoseLandmarkerHelper.LandmarkerListener  {
    private lateinit var binding: FragmentCameraModelBinding
    private lateinit var cameraActivity: CameraActivity
    private lateinit var poseSettingViewModel: PoseSettingViewModel
    private lateinit var poseCameraViewModel: PoseCameraViewModel
    private lateinit var sharedPreferences:SharedPreferences

    // CoroutineScope 用于管理 Kotlin 协程的生命周期，该 CoroutineScope 用于在主线程上调度协程
    private val mainScope = CoroutineScope(Dispatchers.Main)
    // ExecutorService 用于管理和控制异步任务的执行
    private lateinit var backgroundExecutor: ExecutorService
    // PoseLandmarkerHelper 有关 PoseLandmarker 的参数配置和姿态检测和检测结果回调接口
    lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private val poseLandmarkerSettingFragment = PoseLandmarkerSettingFragment()
    // 相机提供者，用于将相机的生命周期绑定到应用程序进程中的任何 LifecycleOwner （Activity Fragment等等）
    private lateinit var cameraProvider: ProcessCameraProvider
    // 相机预览视图，用于提供相机预览流以在屏幕上显示
    private lateinit var preview: Preview
    // 相机帧分析用例，由于设备旋转后需要在 onConfigurationChanged 里重新配置 imageAnalyzer，所以为全局变量
    private lateinit var imageAnalyzer: ImageAnalysis
    // 相机接口实例，通过 camera 通过 CameraControl 控制相机 或 CameraInfo 获得相机信息
    private lateinit var camera: Camera
    // 相机选择实例，用于选择前置或后置相机
    private lateinit var cameraSelector:CameraSelector
    // 相机视频拍摄实例，为视频提供相机流
    private lateinit var videoCapture: VideoCapture<Recorder>
    // 提供对当前视频录制的控制
    private var recording: Recording? = null
    //
    private lateinit var videoAndInsName: String
    // 表示相机的前置或后置，初始为后置相机
    private var cameraFacing = CameraSelector.LENS_FACING_BACK
    // 相机计时器闪烁效果任务
    private  var trackingJob: Job? = null
    // 相机控制器，camera.cameraControl
    private lateinit var cameraControl: CameraControl
    // 存储相机焦距范围
    private lateinit var focusLengthRange: Range<Float>
    // 存储相机曝光范围
    private lateinit var exposureRange: Range<Int>
    // 相机曝光缩放比例，相机曝光由控件 seekbar 进行调整，seekbar范围为0——100，需要从 seekbar 的值映射到 相机曝光值
    private var exposureRation by Delegates.notNull<Float>()
    // 相机初始焦距，取1f
    private val initialZoomRatio = 1f
    // 相机初始曝光值，取0
    private val initialExposure = 0
    // 曝光值 seekbar 的初始进度，调整为一半
    private val initialProgress = 50
    // 相机当前焦距，初值为 initialZoomRatio
    private var currentZoomRatio = initialZoomRatio
    // 相机帧计数器，用于设置自动追焦模式下追焦间隔
    private var frameCount = 0
    // 相机录像计时器时间
    private var startTime: Long = 0
    // 相机变焦手势检测
    private lateinit var zoomScaleGestureDetector: ScaleGestureDetector
    // 相机对焦手势检测
    private lateinit var focusGestureDetector: GestureDetector
    /**
     * 由于此相机开启后的目的是检测人体姿态，所以主开关并不是录制视频开关
     * 如果需要保存录制的视频，需要单独打开视频录制开关（camera_recording_iv）
     * 由于姿态检测 在 imageAnalyzer中调用，而 imageAnalyzer 在 backgroundExecutor 线程上运行
     * 所以姿态检测开关需要用线程安全的 Boolean 类型 AtomicBoolean ，否则无法正确在线程间传递Boolean变量
     */
    // 姿态识别开关
    private var isDetectPose = AtomicBoolean(false)
    // 骨架图绘制开关
    private var isDrawSkeletal = true
    // 自动追焦开关
    private var isTrackFocus = false
    // 视频录制开关
    private var isRecording =false
    // 录制完成的视频的 uri
    private var videoUri: Uri? = null
    private lateinit var insFile:File
    private var skeletalListener: SkeletalListener? = null
    interface SkeletalListener {
        fun onSkeletalReceived(poseLandmarkerResults: PoseLandmarkerResult)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // 确保 context 是 CameraActivity 类型
        if (context is CameraActivity) {
            cameraActivity = context
            skeletalListener = context
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //声明后台线程
        backgroundExecutor = Executors.newSingleThreadExecutor()
        sharedPreferences=requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        Log.i(TAG,"onCreate")

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(TAG,"onCreateView")
        binding = FragmentCameraModelBinding.inflate(layoutInflater)

        // 获得 PoseCameraViewModel，由于要和 CameraControlFragment 交互，通过父Activity获取 ViewModel
        poseCameraViewModel=ViewModelProvider(requireActivity())[PoseCameraViewModel::class.java]
        // 绑定生命周期
        binding.lifecycleOwner = this
        binding.viewModel = poseCameraViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG,"onViewCreated")
        // CameraModelFragment 相关控制监听
        setCameraFragment()
        // CameraControlFragment 相关控制监听
        setCameraControlListeners()
        // 变焦事件监听
        setCameraZoomListener()
        // 对焦事件监听
        setCameraFocusListener()
        // 曝光调整事件监听
        setCameraExposureListener()
        //在后台线程创建该模式下对应 poseLandmarkerHelper 实例
        backgroundExecutor.execute {
            poseLandmarkerHelper = PoseLandmarkerHelper(
                //识别模型默认为FULL
                currentModel = PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL,
                //委托模式默认为CPU
                currentDelegate = PoseLandmarkerHelper.DELEGATE_CPU,
                //相机模式下为LIVE_STREAM
                runningMode = RunningMode.LIVE_STREAM,
                context = requireContext(),
                //该 fragment 实现 poseLandmarkerHelperListener
                poseLandmarkerHelperListener = this
            )
            //将 poseLandmarkerHelper 实例当作参数传递给 poseSettingViewModel
            val factory = PoseSettingViewModelFactory(poseLandmarkerHelper)
            poseSettingViewModel = ViewModelProvider(this, factory)[PoseSettingViewModel::class.java]
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i(TAG,"onStart")
    }

    /**
     * 1.onResume阶段设置初始焦距显示
     * 2.由于fragment在app进入后台，重新返回时，相机焦距会重置为1.0f
     * 会经历生命周期onPause onStart onResume，所以需要在onResume中重置焦距显示
     */
    override fun onResume() {
        super.onResume()
        binding.cameraFocalLengthTv.text=String.format(Locale.CHINA,"%.1fx",initialZoomRatio)
        Log.i(TAG,"onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG,"onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG,"onDestroy")
        // 取消定时任务
        trackingJob?.cancel()
        // 取消 mainScope 及其所有子协程
        mainScope.cancel()
        // 关闭后台线程
        backgroundExecutor.shutdown()
        // 确保后台任务全部完成
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
    }

    /**
     *  cameraProvider 是单例模式，整个项目中只能存在一个用例
     *  ProcessCameraProvider.getInstance()是一个异步操作，必须等 cameraProviderFuture 完成后执行后续代码
     */
    private fun mInitCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        // 异步获取 cameraProvider
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()
                mInitCameraSetting()
            },
            // 获取主线程的执行器，确保相机操作在主线程上执行
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    /**
     * 方法包含相机参数设置，用例构建，用例绑定等等，fragment产生时以及摄像头翻转时都会执行该方法
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun mInitCameraSetting() {

        // 检查 cameraSelector 是否初始化
        if (!::cameraSelector.isInitialized) {
            // 如果未初始化，选择后置相机（ cameraFacing 初值为后置）
            // 如果初始化了，说明相机经历了翻转事件
            cameraSelector = CameraSelector.Builder()
                .requireLensFacing(cameraFacing)
                .build()
        }

        // 创建 ResolutionSelector
        // 暂不支持调整摄像屏幕宽高比
        val resolutionSelector = ResolutionSelector.Builder()
            // 设置视频比例为4比3
            .setAspectRatioStrategy(
                AspectRatioStrategy(AspectRatio.RATIO_4_3,
                AspectRatioStrategy.FALLBACK_RULE_AUTO)//参数2为备选比例
            ).build()

        // 预览用例设置
        preview = Preview.Builder().
            // 将刚刚设置好的 ResolutionSelector 作为参数填入
            setResolutionSelector(resolutionSelector)
            //设置目标旋转角度，与视图 preview 的旋转方向一致
            .setTargetRotation(binding.cameraPreview.display.rotation)
            .build()

        // 分析用例设置
        imageAnalyzer = ImageAnalysis.Builder()
            // 将刚刚设置好的 ResolutionSelector 作为参数填入
            .setResolutionSelector(resolutionSelector)
            // 设置目标旋转角度，与视图 preview 的旋转方向一致
            .setTargetRotation(binding.cameraPreview.display.rotation)
            // 当分析器处理图像的速度跟不上摄像头生成图像的速度时，选择只保留最新的图像帧
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            // 指定图像分析器输出图像的格式，RGBA_8888表示图像以32位存储，每个像素包含红、绿、蓝和透明度（Alpha）四个通道，每个通道占8位
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                // 在 backgroundExecutor 线程进行图像帧分析
                // setAnalyzer 方法的第一个参数必须为 java. util. concurrent. Executor
                // 使用 Kotlin协程改写不太方便
                it.setAnalyzer(backgroundExecutor) { image ->
                    // imageAnalyzer规定每帧图像访问后必须手动执行image.close()，防止阻塞后续的图像帧
                    // user 方法可以在代码块执行完毕后，自动调用 close()关闭资源
                    image.use { processedImage ->
                        // 当检测开关开启时，才进行检测
                        if (isDetectPose.get()) {
                            Log.d("ImageAnalyzer", "Detecting pose")
                            // 检测图像帧
                            detectPose(processedImage)
                        }
                        else {
                            Log.d("ImageAnalyzer", "No Detecting pose")
                        }
                    }
                }
            }

        // 视频录制器设置
        val recorder = Recorder.Builder()
            // 选择最低质量的视频记录，减小视频存储空间
            // TODO: 可能会开放视频质量选择权给用户
            .setQualitySelector(QualitySelector.from(Quality.LOWEST))
            .build()
        // 创建 VideoCapture 实例，并将上面配置好的 Recorder 作为输出目标
        videoCapture = VideoCapture.withOutput(recorder)

        // 解绑相机先前用例
        cameraProvider.unbindAll()

        try {
            //绑定相机生命周期到该 fragment，同时绑定各个用例到相机
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview,imageAnalyzer,videoCapture
            )
            // 初始化 cameraControl
            cameraControl = camera.cameraControl
            // 获取变焦范围
            focusLengthRange=Range.create(
                camera.cameraInfo.zoomState.value?.minZoomRatio?: 1f,
                camera.cameraInfo.zoomState.value?.maxZoomRatio?: 10f
            )
            // 获取曝光范围
            exposureRange=camera.cameraInfo.exposureState.exposureCompensationRange
            // 计算 seekbar 到曝光值的缩放比例
            // 一般相机的曝光范围为一对相反数区间，缩放比例为区间边界绝对值之和 比 seekbar 的max值
            exposureRation= ((abs(exposureRange.lower)+abs(exposureRange.upper))/binding.cameraExposeSb.max.toFloat())
            // 设置相机初始焦距为 initialZoomRatio
            cameraControl.setZoomRatio(initialZoomRatio)
            // 设置相机初始曝光值为 initialExposure
            cameraControl.setExposureCompensationIndex(initialExposure)
            // 设置焦距显示为initialZoomRatio，与相机初始焦距统一
            binding.cameraFocalLengthTv.text=String.format(Locale.CHINA,"%.1fx",initialZoomRatio)
            // 设置 seekbar 初始进度为 initialProgress，与相机初始曝光值统一
            binding.cameraExposeSb.progress=initialProgress
            // 将相机的预览输出连接到 cameraPreview
            preview.surfaceProvider = binding.cameraPreview.surfaceProvider
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    /**
     *方法用于开关录制，存储视频
     */
    private fun captureVideo() {
        // 如果录制开关为关闭状态，直接返回
        if (!isRecording)
            return

        // 如果当前有录制在进行
        if (recording != null) {
            //停止录制
            recording?.stop()
            // 重置 recording 变量
            recording = null
            // 显示 Dialog 让用户决定是否保存视频以及设置视频名称
            showSaveVideoDialog()
            return
        }
        // 如果没有正在进行的录制,则开始一个新的录制会话
        // 使用当前时间创建一个唯一的文件名
        videoAndInsName = SimpleDateFormat(FILENAME_FORMAT, Locale.CHINESE)
            .format(System.currentTimeMillis())
        // 创建 ContentValues 对象,用于设置录制视频的元数据
        val contentValues = ContentValues().apply {
            // 设置显示名称
            put(MediaStore.MediaColumns.DISPLAY_NAME, videoAndInsName)
            // 设置MIME类型
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            // 设置保存路径
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/RCApp")
        }

        // 创建 MediaStore 输出选项
        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(requireContext().contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()


        // 准备并开始录制
        recording = videoCapture.output
            .prepareRecording(requireContext(), mediaStoreOutputOptions)
            .apply {
                // 检查是否有录音权限,如果有则启用音频
                if (PermissionChecker.checkSelfPermission(requireContext(),
                        Manifest.permission.RECORD_AUDIO) ==
                    PermissionChecker.PERMISSION_GRANTED)
                {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
                // 录制事件回调
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        // 录制开始时的处理逻辑

                        val phone=sharedPreferences.getString("phone",null)
                        // 先构建指令文件，把文件路径传给Activity，Activity设置好路径后，一边生成指令一边保存指令到文件中
                        val directory = File(requireContext().filesDir, phone!!)

                        insFile = File(directory, "$videoAndInsName.txt")
                        if (!insFile.exists()) {
                            insFile.createNewFile()
                        }
                        cameraActivity.setVideoAndInsFile(insFile)
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            // 录制成功完成时，保存视频URI
                            videoUri = recordEvent.outputResults.outputUri
                        } else {
                            // 录制出错时的处理逻辑
                            recording?.close()
                            recording = null
                            Log.e(TAG, "视频录制出错: ${recordEvent.error}")
                        }
                    }
                }
            }

    }

    /**
     * 保存视频询问框，用户可决定是否保存视频及指令，以及设置自定义视频指令名称
     */
    private fun showSaveVideoDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("是否保存该视频及对应机器人指令")

        val input = EditText(context)
        input.hint = "视频指令1"
        builder.setView(input)

        // 设置按钮
        builder.setPositiveButton("是") { dialog, _ ->
            val videoInstructionName = input.text.toString()
            // 处理用户输入,保存视频
            saveVideo(videoInstructionName)
            dialog.dismiss()
        }

        builder.setNegativeButton("否") { dialog, _ ->
            // 用户取消，删除录制的视频
            deleteVideo()
            dialog.cancel()
        }

        // 显示对话框
        builder.show()
    }

    /**
     * 保存用户录制的视频及其指令
     */
    private fun saveVideo(userInput: String) {
        // 这里实现保存视频的逻辑
        // 可以使用 videoUri 和 userInput 来处理和保存视频
        videoUri?.let { uri ->
            // 例如，你可以更新视频的元数据，将用户输入作为视频的描述
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DESCRIPTION, userInput)
            }
            requireContext().contentResolver.update(uri, contentValues, null, null)
        }
        val videoPath = getFilePathFromName(requireContext(), videoAndInsName) ?: ""
        // 数据库保存用户录制视频路径和指令路径
        UserDatabaseHelper(requireContext()).createUserInstruction(
            sharedPreferences.getString("phone",null)!!,
            userInput,
            videoPath,
            insFile.absolutePath)
    }

    private fun deleteVideo() {
        // 删除录制的视频
        videoUri?.let { uri ->
            requireContext().contentResolver.delete(uri, null, null)
            Log.d(TAG, "视频已删除，URI: $uri")
        }
        videoUri = null
        if(insFile.exists()){
            insFile.delete()
        }
    }

    /**
     * 根据视频名称获取录制视频的文件路径
     */
    private fun getFilePathFromName(context: Context, name: String): String? {
        val projection = arrayOf(MediaStore.Video.Media.DATA)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf("$name.mp4")

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                return cursor.getString(columnIndex)
            }
        }
        return null
    }

    private fun getPrivateFilePath(context: Context, phone: String?, fileName: String): String? {
        // 1. 构建目标目录路径
        val baseDir = context.filesDir
        val targetDir = File(baseDir, phone!!)

        // 2. 构建完整文件路径
        val targetFile = File(targetDir, fileName)

        // 3. 验证文件是否存在
        return if (targetFile.exists()) {
            targetFile.absolutePath
        } else {
            null
        }
    }

    /**
     *  呼出 PoseLandmarkerSettingFragment，并进行相应设置
     */
    private fun showPoseLandmarkerSettingFragment() {
        // 对 PoseLandmarker 的配置更新的监听
        poseLandmarkerSettingFragment.setPoseLandmarkerSettingChangedListener{
            backgroundExecutor.execute {
                // 清除原有的配置
                poseLandmarkerHelper.clearPoseLandmarker()
                // 更新配置
                poseLandmarkerHelper.setupPoseLandmarker()
            }
            // 清除原有的图像
            binding.cameraOverlay.clear()
            binding.focusFrame.clear()

        }
        // 添加 poseLandmarkerSettingFragment
        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, poseLandmarkerSettingFragment)
            .commit()

        //设置 poseLandmarkerSettingFragment 的容器可见,添加呼出时的动画效果
        binding.fragmentContainer.visibility = View.VISIBLE
        binding.fragmentContainer.animate()
            .translationY(0f)
            .setDuration(300)
            .start()
    }
    private fun startCameraTracking() {
        trackingJob = mainScope.launch {
            while (isActive) {
                binding.cameraTickingIv.visibility =
                    if (!binding.cameraTickingIv.isVisible) View.VISIBLE else View.INVISIBLE

                delay(500)
            }
        }
    }

    /**
     * CameraModelFragment 设置及视图相关事件监听
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setCameraFragment(){
        binding.run {
            // poseLandmarkerSettingFragment 的呼出监听
            plcSettingIv.setOnClickListener {
                showPoseLandmarkerSettingFragment()
            }

            // post确定 cameraPreview 设置好后进行 mInitCamera
            cameraPreview.post {
                mInitCamera()
            }

            // 绑定 zoomScaleGestureDetector 和 focusGestureDetector 手势事件到 cameraPreview 触摸监听器上
            cameraPreview.setOnTouchListener { _, event ->
                zoomScaleGestureDetector.onTouchEvent(event)
                focusGestureDetector.onTouchEvent(event)
                // 返回 true 表示消费事件
                true
            }

            // 骨架图开关监听
            cameraSkeletalIv.apply {
                //初始为显示骨架图
                isSelected = true
                setOnClickListener {
                    isSelected = !isSelected
                    isDrawSkeletal =!isDrawSkeletal
                    if (!isDrawSkeletal) {
                        // 如果不显示骨架图，清除已绘制骨架图
                        binding.cameraOverlay.clear()
                    }
                }
            }

            // 自动追焦开关监听
            cameraAutoFocusIv.apply {
                setOnClickListener {
                    isSelected = !isSelected
                    isTrackFocus = !isTrackFocus
                    if(!isTrackFocus){
                        // 如果关闭自动追焦，清除已绘制自动追焦框
                        binding.focusFrame.clear()
                    }
                }
            }

            // 视频录制开关监听
            cameraRecordingIv.apply {
                setOnClickListener {
                    // 非拍摄时才能切换
                    if(!isDetectPose.get()){
                        isSelected =!isSelected
                        isRecording=!isRecording
                    }
                }
            }
        }
    }

    /**
     *  CameraControlFragment 相关控制事件监听
     *  该方法中都是通过 ViewModel 观察对应 LiveData 实现监听
     *  控件的 Visibility 状态更新通过单向绑定 ViewModel 实现
     */
    private fun setCameraControlListeners(){

        // 相机开关状态监听
        poseCameraViewModel.isCameraOn.observe(viewLifecycleOwner) { isCameraOn ->
            when(isCameraOn){
                false -> {
                    // 清除骨架图
                    binding.run {
                        cameraOverlay.clear()
                        // 清除自动追焦框
                        focusFrame.clear()
                        // 停止相机计时器
                        cameraTimeChr.stop()
                    }
                    // 取消计时器闪烁任务
                    trackingJob?.cancel()

                }
                true -> {
                    binding.run {
                        // 重置计时
                        cameraTimeChr.base = SystemClock.elapsedRealtime()
                        // 开启计时
                        cameraTimeChr.start()
                    }
                    // 计时器开启闪烁
                    startCameraTracking()
                }
            }
            // 重置 isDetectPose
            isDetectPose.set(isCameraOn)
            // 开启录像（如果录像开关开启的话）
            captureVideo()
        }

        // 相机翻转事件监听
        poseCameraViewModel.isCameraFacingBack.observe(viewLifecycleOwner){ isCameraFacingBack ->
            cameraFacing = when(isCameraFacingBack){
                // 非后置（前置）
                false -> {
                    CameraSelector.LENS_FACING_FRONT
                }
                // 后置
                true -> {
                    CameraSelector.LENS_FACING_BACK
                }
            }
            // 重新设置 cameraSelector
            cameraSelector=CameraSelector.Builder()
                .requireLensFacing(cameraFacing)
                .build()
            // 重新初始化相机
            mInitCamera()
        }
    }

    /**
     * 方法包含点击对焦和长按锁定对焦两种方式
     * GestureDetector 是一个手势监听器
     * 点击对焦可以暂时打断自动追焦，长按锁焦不行
     */
    private fun setCameraFocusListener() {
        focusGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            // onSingleTapUp 表示单指抬起
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                // 通过设置 isAutoFocus 进行互斥访问
                // 先暂停自动追焦
                var isTrack=false
                if(isTrackFocus){
                    isTrackFocus = false
                    isTrack=true
                }
                // 取消之前的定时任务
                mainScope.coroutineContext.cancelChildren()
                // 点击对焦框会存在三秒，三秒后消失，自动追焦也恢复
                mainScope.launch {
                    delay(3000)
                    binding.focusFrame.clear()
                    if(isTrack){
                        isTrackFocus = true
                    }
                }
                // 根据手指抬起的位置，在该点绘制对焦框
                binding.focusFrame.drawFocusFrame(e.x, e.y)
                // 相机在该点对焦
                singleFocus(e.x, e.y)
                return true
            }
            // onLongPress 表示长按
            override fun onLongPress(e: MotionEvent) {
                // 如果没有进行自动追焦
                if(!isTrackFocus){
                    mainScope.coroutineContext.cancelChildren()
                    // 在该点绘制锁焦框
                    binding.focusFrame.drawLockFocusFrame(e.x,e.y)
                    // 相机在该点锁焦
                    lockFocus(e.x,e.y)
                }
            }
        })
    }

    /**
     * 方法关于变焦手势事件监听
     * ScaleGestureDetector 是一个专门处理缩放手势的手势监听器
     */
    private fun setCameraZoomListener() {
        zoomScaleGestureDetector = ScaleGestureDetector(requireContext(), object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                // 从缩放手势检测器中获取当前的缩放因子
                val scaleFactor = detector.scaleFactor
                // 根据缩放因子计算缩放后的新焦距
                val newZoomRatio = currentZoomRatio * scaleFactor
                // 保证新焦距在该范围内
                currentZoomRatio = newZoomRatio.coerceIn(focusLengthRange.lower, focusLengthRange.upper)
                //设置相机新的焦距
                cameraControl.setZoomRatio(currentZoomRatio)
                // 更新 UI 为新的焦距
                binding.cameraFocalLengthTv.text = String.format(Locale.CHINA,"%.1fx", currentZoomRatio)
                return true
            }
        })
    }

    /**
     * 方法关于曝光 seekbar 滑动监听
     */
    private fun setCameraExposureListener(){
        binding.cameraExposeSb.setOnSeekBarChangeListener(object :SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                //根据曝光缩放率计算新的曝光值
                camera.cameraControl.setExposureCompensationIndex(
                    (progress*exposureRation+exposureRange.lower).toInt()
                )
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
    }
    /**
     * 创建对焦测光点
     */
    private fun createFocusPoint(x: Float, y: Float): MeteringPoint {
        val meteringPointFactory = SurfaceOrientedMeteringPointFactory(
            binding.cameraPreview.width.toFloat(),
            binding.cameraPreview.height.toFloat()
        )
        return meteringPointFactory.createPoint(x, y)
    }

    /**
     * 构建对焦测光动作
     * @param focusPoint 对焦点
     * @param autoCancelDuration 自动取消时长（单位：秒），null表示不设置
     * @param disableAutoCancel 是否禁用自动取消
     */
    private fun buildFocusAction(
        focusPoint: MeteringPoint,
        autoCancelDuration: Long? = null,
        disableAutoCancel: Boolean = false
    ): FocusMeteringAction {
        // 合并AF和AE的flags
        val flags = FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        return FocusMeteringAction.Builder(focusPoint, flags).apply {
            if (disableAutoCancel) {
                disableAutoCancel()
            } else {
                autoCancelDuration?.let { setAutoCancelDuration(it, TimeUnit.SECONDS) }
            }
        }.build()
    }

    /**
     * 点击对焦（3秒后自动取消）
     */
    private fun singleFocus(x: Float, y: Float) {
        cameraControl.startFocusAndMetering(
            buildFocusAction(
                createFocusPoint(x, y),
                autoCancelDuration = 3
            )
        )
    }

    /**
     * 锁定对焦（禁用自动取消）
     */
    private fun lockFocus(x: Float, y: Float) {
        cameraControl.startFocusAndMetering(
            buildFocusAction(
                createFocusPoint(x, y),
                disableAutoCancel = true
            )
        )
    }

    /**
     * 自动追焦（1秒后自动取消）
     */
    private fun trackFocus(x: Float, y: Float) {
        cameraControl.startFocusAndMetering(
            buildFocusAction(
                createFocusPoint(x, y),
                autoCancelDuration = 1
            )
        )
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
    override fun onError(error: String, errorCode: Int) {
        //
    }

    /**
     * 检测结果回调
     * resultBundle.results包含多个目标的骨架图，只取第一个对象的骨架图传递
     */
    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (isDetectPose.get()) {
                skeletalListener?.onSkeletalReceived(resultBundle.results.first())

                // 更新延迟时间
                val inferenceTime = resultBundle.inferenceTime
                binding.inferenceTimeVal.text = String.format(inferenceTime.toString())
                if(isDrawSkeletal){
                    // 绘制骨架图
                    binding.cameraOverlay.setResults(
                        resultBundle.results.first(),
                        resultBundle.inputImageHeight,
                        resultBundle.inputImageWidth,
                        RunningMode.LIVE_STREAM
                    )
                }

                if(isTrackFocus){
                    // 绘制自动追焦框
                    binding.focusFrame.drawAutoFocusFrame(
                        resultBundle.results.first(),
                        resultBundle.inputImageHeight,
                        resultBundle.inputImageWidth
                    )
                    // 计算追焦框的中心点
                    resultBundle.results.first().let { poseLandmarkerResult ->
                        for (landmark in poseLandmarkerResult.landmarks()) {
                            // 遍历 landmark 中的关节点
                            val scaleFactor = max(
                                binding.cameraOverlay.width * 1f / resultBundle.inputImageWidth,
                                binding.cameraOverlay.height * 1f / resultBundle.inputImageHeight
                            )
                            var minX: Float = Float.MAX_VALUE
                            var minY: Float = Float.MAX_VALUE
                            var maxX: Float = Float.MIN_VALUE
                            var maxY: Float = Float.MIN_VALUE
                            for (normalizedLandmark in landmark) {
                                val x =
                                    normalizedLandmark.x() * resultBundle.inputImageWidth * scaleFactor
                                val y =
                                    normalizedLandmark.y() * resultBundle.inputImageHeight * scaleFactor
                                // 更新最小和最大值
                                if (x < minX) minX = x
                                if (y < minY) minY = y
                                if (x > maxX) maxX = x
                                if (y > maxY) maxY = y
                            }
                            val centerX = (minX + maxX) / 2
                            val centerY = (minY + maxY) / 2
                            // 统计帧数
                            frameCount++
                            if (startTime == 0L) {
                                startTime = System.currentTimeMillis()
                            }
                            // 检查是否经过 1 秒
                            if (System.currentTimeMillis() - startTime >= 1000) {
                                // 调用 focusOnAutoPoint
                                trackFocus(centerX, centerY)
                                // 重置计数器和计时器
                                frameCount = 0
                                startTime = System.currentTimeMillis()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 摄像头旋转后对分析用例的更新
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer.targetRotation =
            binding.cameraPreview.display.rotation
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}