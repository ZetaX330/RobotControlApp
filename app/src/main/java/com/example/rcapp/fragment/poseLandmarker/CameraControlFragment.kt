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
        binding.cameraModelSwitchIv.setOnClickListener {
            viewModel.setCameraStatus(!binding.cameraModelSwitchIv.isSelected)
            binding.cameraModelSwitchIv.isSelected=!binding.cameraModelSwitchIv.isSelected
        }
        return binding.root
    }

    companion object {
    }
}