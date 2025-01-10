package com.example.rcapp.fragment.poseLandmarker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.rcapp.activity.QuestionActivity
import com.example.rcapp.databinding.FragmentPoseLandmarkerSettingBinding
import com.example.rcapp.model.PoseLandmarkerHelper
import com.example.rcapp.viewmodel.PoseSettingViewModel

class PoseLandmarkerSettingFragment  : Fragment() {
    private lateinit var binding: FragmentPoseLandmarkerSettingBinding
    private val viewModel: PoseSettingViewModel by viewModels({ requireParentFragment() })
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private var poseLandmarkerSettingChangedListener: PoseLandmarkerSettingChangedListener? = null
    fun  interface PoseLandmarkerSettingChangedListener{
        fun onPoseLandmarkerSettingChanged()

    }
    fun setPoseLandmarkerSettingChangedListener(listener: PoseLandmarkerSettingChangedListener) {
        this.poseLandmarkerSettingChangedListener = listener
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding= FragmentPoseLandmarkerSettingBinding.inflate(layoutInflater)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        initSettingControls()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 获取父 Fragment 并访问 poseLandmarkerHelper
        val parentFragment = requireParentFragment() as? CameraModelFragment
        poseLandmarkerHelper = parentFragment!!.poseLandmarkerHelper
    }
    override fun onDestroy() {
        super.onDestroy()
    }

    //收起该fragment
    private fun hideFragment() {
        parentFragment?.let {
            val rootView = view ?: return
            rootView.animate()
                .translationY(-rootView.height.toFloat()) // 向上滑动
                .setDuration(300) // 动画时长 300ms
                .withEndAction {
                    // 动画结束后移除 Fragment
                    parentFragment?.childFragmentManager?.beginTransaction()
                        ?.remove(this)
                        ?.commit()
                }
                .start()
        }
    }
    private fun initSettingControls() {
        //点击前往setting的说明页面
        binding.plcSettingQuestionIv.setOnClickListener {
            startActivity(Intent(context,QuestionActivity::class.java))
        }
        //收起该fragment
        binding.plcSettingCloseIv.setOnClickListener {
            hideFragment()
        }
        //下面都是参数的调整监听，每次调整完都要更新plm的实际参数
        binding.detectionThresholdMinus.setOnClickListener {
            if (poseLandmarkerHelper.detectionConfidence >= 0.2) {
                poseLandmarkerHelper.detectionConfidence -= 0.1f
                viewModel.setDetectionConfidence(poseLandmarkerHelper.detectionConfidence)
                updatePlmSetting()
            }
        }

        // When clicked, raise pose detection score threshold floor
        binding.detectionThresholdPlus.setOnClickListener {
            if (poseLandmarkerHelper.detectionConfidence <= 0.8) {
                poseLandmarkerHelper.detectionConfidence += 0.1f
                viewModel.setDetectionConfidence(poseLandmarkerHelper.detectionConfidence)
                updatePlmSetting()
            }
        }

        // When clicked, lower pose tracking score threshold floor
        binding.trackingThresholdMinus.setOnClickListener {
            if (poseLandmarkerHelper.trackingConfidence >= 0.2) {
                poseLandmarkerHelper.trackingConfidence -= 0.1f
                viewModel.setTrackingConfidence(poseLandmarkerHelper.trackingConfidence)
                updatePlmSetting()
            }
        }

        // When clicked, raise pose tracking score threshold floor
        binding.trackingThresholdPlus.setOnClickListener {
            if (poseLandmarkerHelper.trackingConfidence <= 0.8) {
                poseLandmarkerHelper.trackingConfidence += 0.1f
                viewModel.setTrackingConfidence(poseLandmarkerHelper.trackingConfidence)
                updatePlmSetting()
            }
        }

        // When clicked, lower pose presence score threshold floor
        binding.presenceThresholdMinus.setOnClickListener {
            if (poseLandmarkerHelper.presenceConfidence >= 0.2) {
                poseLandmarkerHelper.presenceConfidence -= 0.1f
                viewModel.setPresenceConfidence(poseLandmarkerHelper.presenceConfidence)
                updatePlmSetting()
            }
        }

        // When clicked, raise pose presence score threshold floor
        binding.presenceThresholdPlus.setOnClickListener {
            if (poseLandmarkerHelper.presenceConfidence <= 0.8) {
                poseLandmarkerHelper.presenceConfidence += 0.1f
                viewModel.setPresenceConfidence(poseLandmarkerHelper.presenceConfidence)
                updatePlmSetting()
            }
        }
        binding.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long
                ) {
                    try {
                        viewModel.setDelegate(p2)
                        updatePlmSetting()
                    } catch(e: UninitializedPropertyAccessException) {
                        Log.e(TAG, "PoseLandmarkerHelper has not been initialized yet.")
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }

        binding.spinnerModel.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p0: AdapterView<*>?,
                    p1: View?,
                    p2: Int,
                    p3: Long
                ) {
                    viewModel.setModel(p2)
                    updatePlmSetting()

                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
    }
    //由CameraModelFragment实现接口，用于更新plm的参数
    private fun updatePlmSetting() {
        if(this::poseLandmarkerHelper.isInitialized) {
            poseLandmarkerSettingChangedListener?.onPoseLandmarkerSettingChanged()
        }
    }
    companion object {
        private const val TAG = "Pose Landmarker"
    }
}