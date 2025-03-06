package com.example.rcapp.ui.viewmodel
import androidx.lifecycle.LiveData

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.rcapp.util.PoseLandmarkerHelper
import java.util.Locale

/**
 * 对应plmHelper中五个参数
 */
class PoseSettingViewModel(private val poseLandmarkerHelper: PoseLandmarkerHelper) : ViewModel() {
    private val _model = MutableLiveData(PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL)
    private val _delegate = MutableLiveData(PoseLandmarkerHelper.DELEGATE_CPU)
    private val _detectionConfidence = MutableLiveData(PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE)
    private val _trackingConfidence = MutableLiveData(PoseLandmarkerHelper.DEFAULT_POSE_TRACKING_CONFIDENCE)
    private val _presenceConfidence = MutableLiveData(PoseLandmarkerHelper.DEFAULT_POSE_PRESENCE_CONFIDENCE)
    val currentDelegate: LiveData<Int> get() = _delegate
    val currentModel: LiveData<Int> get() = _model
    val currentDetectionConfidence: LiveData<Float> get() = _detectionConfidence
    val currentTrackingConfidence: LiveData<Float> get() = _trackingConfidence
    val currentPresenceConfidence: LiveData<Float> get() = _presenceConfidence

    fun setModel(model: Int) {
        _model.value = model
        poseLandmarkerHelper.currentModel=model
    }
    fun setDelegate(delegate: Int) {
        _delegate.value = delegate
        poseLandmarkerHelper.currentDelegate=delegate
    }

    fun setDetectionConfidence(confidence: Float) {
        val roundedValue = String.format(Locale.US, "%.2f", confidence).toFloat()
        _detectionConfidence.value = roundedValue
        poseLandmarkerHelper.detectionConfidence = confidence
    }

    fun setTrackingConfidence(confidence: Float) {
        val roundedValue = String.format(Locale.US, "%.2f", confidence).toFloat()
        _trackingConfidence.value = roundedValue
        poseLandmarkerHelper.trackingConfidence = confidence

    }

    fun setPresenceConfidence(confidence: Float) {
        val roundedValue = String.format(Locale.US, "%.2f", confidence).toFloat()
        _presenceConfidence.value = roundedValue
        poseLandmarkerHelper.presenceConfidence = confidence
    }


}