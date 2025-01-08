package com.example.rcapp.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.rcapp.fragment.poseLandmarker.CameraControlFragment
import com.example.rcapp.fragment.poseLandmarker.CameraModelFragment
import com.example.rcapp.fragment.poseLandmarker.ImageControlFragment
import com.example.rcapp.fragment.poseLandmarker.ImageModelFragment
import com.example.rcapp.fragment.poseLandmarker.VideoControlFragment

class RunningModelVp2Adapter (activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CameraControlFragment()
            1 -> VideoControlFragment()
            2 -> ImageControlFragment()
            else -> throw IllegalStateException("Unexpected position $position")
        }
    }
}