package com.example.rcapp.fragment.poseLandmarker

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.example.rcapp.R
import com.example.rcapp.databinding.FragmentVideoControlBinding
import com.example.rcapp.viewmodel.PoseVideoViewModel


class VideoControlFragment : Fragment() {
    private lateinit var binding:FragmentVideoControlBinding
    private lateinit var viewModel: PoseVideoViewModel
    private val openVideoLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            // 处理返回的视频URI
            sendVideoUri(it)
        } ?: run {
            // 用户未选择文件
            Toast.makeText(context, "No video selected", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentVideoControlBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(requireActivity())[PoseVideoViewModel::class.java]

        binding.videoModelSelectIv.setOnClickListener {
            openVideoLauncher.launch(arrayOf("video/*"))
        }
        return binding.root
    }
    private fun sendVideoUri(uri: Uri) {
        viewModel.setVideoUri(uri)
    }
    companion object {

    }
}