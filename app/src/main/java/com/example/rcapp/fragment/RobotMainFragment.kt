package com.example.rcapp.fragment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.example.rcapp.R
import com.example.rcapp.activity.BluetoothLinkActivity
import com.example.rcapp.activity.PoseLandmarkerActivity
import com.example.rcapp.adapter.RobotMainVp2Adapter
import com.example.rcapp.databinding.FragmentRobotMainBinding

class RobotMainFragment : Fragment() {
    private lateinit var binding: FragmentRobotMainBinding
    private lateinit var viewPager: ViewPager2
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            val currentItem = viewPager.currentItem
            val nextItem = (currentItem + 1) % 3 // 假设有 3 个页面
            viewPager.currentItem = nextItem
            handler.postDelayed(this, 5000) // 每 3 秒轮播一次
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRobotMainBinding.inflate(inflater, container, false)
        viewPager = binding.robotShowVp2
        val images = listOf(
            R.drawable.robot_main_show_item1,
            R.drawable.robot_main_show_item2,
            R.drawable.robot_main_show_item3
        )

        viewPager.adapter = RobotMainVp2Adapter(images)

        // 开始自动轮播
        handler.postDelayed(runnable, 5000)
        return binding.root // 返回根视图
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setRobotMainListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable) // 防止内存泄漏
    }
    private fun setRobotMainListener(){
        binding.robotMainCameraBtn.setOnClickListener {
            val intent = Intent(requireContext(), PoseLandmarkerActivity::class.java)
            startActivity(intent)
        }
        binding.robotMainBluetoothBtn.setOnClickListener{
            val intent = Intent(requireContext(), BluetoothLinkActivity::class.java)
            startActivity(intent)
        }
    }
    companion object {

    }
}