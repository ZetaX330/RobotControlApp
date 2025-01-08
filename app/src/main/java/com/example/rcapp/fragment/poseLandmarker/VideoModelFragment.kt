package com.example.rcapp.fragment.poseLandmarker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.rcapp.R
import com.example.rcapp.databinding.FragmentVideoModelBinding
import com.example.rcapp.viewmodel.PoseVideoViewModel


class VideoModelFragment : Fragment() {
    private lateinit var binding: FragmentVideoModelBinding
    private lateinit var viewModel: PoseVideoViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding=FragmentVideoModelBinding.inflate(layoutInflater)
        viewModel=ViewModelProvider(requireActivity())[PoseVideoViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.videoUri.observe(viewLifecycleOwner) { uri ->
            with(binding.videoModelShowVv) {
                setVideoURI(uri)
                // mute the audio
                setOnPreparedListener { it.setVolume(0f, 0f) }
//                requestFocus()
                start()
            }
        }
    }

    companion object {

    }
}