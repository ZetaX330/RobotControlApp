package com.example.rcapp.activity

import android.Manifest.permission

import android.os.Bundle
import androidx.viewpager2.widget.ViewPager2

import com.example.rcapp.R
import com.example.rcapp.adapter.RunningModelVp2Adapter
import com.example.rcapp.databinding.ActivityPoseLandmarkerBinding
import com.example.rcapp.fragment.poseLandmarker.CameraModelFragment
import com.example.rcapp.fragment.poseLandmarker.ImageModelFragment
import com.example.rcapp.fragment.poseLandmarker.VideoModelFragment
import com.google.android.material.tabs.TabLayoutMediator

class PoseLandmarkerActivity  : BleServiceBaseActivity() {
    private lateinit var binding: ActivityPoseLandmarkerBinding
    override fun onBluetoothServiceConnected() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPoseLandmarkerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val adapter = RunningModelVp2Adapter(this)
        binding.runningModelVp2.adapter = adapter

        TabLayoutMediator(binding.runningModelTl, binding.runningModelVp2) { tab, position ->
            tab.text = when (position) {
                0 -> "摄像"
                1 -> "视频"
                2 -> "图片"
                else -> null
            }
        }.attach()
        binding.runningModelVp2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // 根据 position 更新上方的 Fragment
                val newFragment=when (position) {
                    0 -> CameraModelFragment()
                    1 -> VideoModelFragment()
                    2 -> ImageModelFragment()
                    else -> throw IllegalStateException("Unexpected position $position")
                }
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.running_model_fragment,newFragment)
                    .  commit()
            }
        })
    }
    override fun onResume() {
        super.onResume()
    }
    override fun onPause() {
        super.onPause()
    }
    override fun onDestroy() {
        super.onDestroy()
        // Shut down our background executor

    }


    companion object {
        private const val TAG = "CameraActivity"
        //请求编号
        const val REQUEST_CODE_PERMISSIONS = 10
        //要请求的相机权限
        val REQUIRED_PERMISSIONS = arrayOf(permission.CAMERA)
    }

}