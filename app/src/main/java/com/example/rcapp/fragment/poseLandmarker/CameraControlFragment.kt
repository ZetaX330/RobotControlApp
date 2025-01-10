package com.example.rcapp.fragment.poseLandmarker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.rcapp.R
import com.example.rcapp.databinding.FragmentCameraControlBinding
import com.example.rcapp.viewmodel.PoseCameraViewModel
import com.example.rcapp.viewmodel.PoseVideoViewModel


/**
 * A simple [Fragment] subclass.
 * Use the [CameraControlFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CameraControlFragment : Fragment() {
    private lateinit var binding :FragmentCameraControlBinding
    private lateinit var viewModel: PoseCameraViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentCameraControlBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(requireActivity())[PoseCameraViewModel::class.java]
        //控制相机模式下识别的开关
        binding.cameraModelSwitchIv.setOnClickListener {
            //设置该模式的viewModel中相机状态
            viewModel.setCameraStatus(!binding.cameraModelSwitchIv.isSelected)
            //更新图标ui
            binding.cameraModelSwitchIv.isSelected=!binding.cameraModelSwitchIv.isSelected
        }
        return binding.root
    }

    companion object {
    }
}