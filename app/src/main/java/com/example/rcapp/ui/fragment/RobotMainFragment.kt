package com.example.rcapp.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.rcapp.R
import com.example.rcapp.databinding.FragmentRobotMainBinding
import com.example.rcapp.ui.activity.BluetoothLinkActivity
import com.example.rcapp.ui.activity.CameraActivity
import com.example.rcapp.ui.activity.UserInstructionActivity
import com.example.rcapp.ui.adapter.RobotMainVp2Adapter

class RobotMainFragment : Fragment() {
    //<editor-fold desc="1. 常量和延迟初始化">
    companion object {
        private const val CAROUSEL_DELAY_MS = 5000L  // 轮播间隔时间
        private const val INITIAL_DELAY_MS = 5000L   // 初始延迟
        private const val PAGE_COUNT = 3              // 页面总数
    }

    private var _binding: FragmentRobotMainBinding? = null
    private val binding get() = _binding!!            // 安全非空断言（仅在生命周期内使用）

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRobotMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupViewPager()
        startCarousel()
        setupClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)  // 移除所有回调
        _binding = null                           // 避免内存泄漏
    }
    //</editor-fold>

    //<editor-fold desc="3. UI初始化逻辑">
    private fun setupToolbar() {
        (activity as? AppCompatActivity)?.apply {
            setSupportActionBar(binding.myToolbar.toolbar)
            binding.myToolbar.setViewModel()  // 确保 setViewModel 实现为空安全
        }
    }

    private fun setupViewPager() {
        viewPager = binding.robotShowVp2
        val images = List(PAGE_COUNT) { R.drawable.robot_main_show_item1 }
        viewPager.adapter = RobotMainVp2Adapter(images)
        binding.indicator.setViewPager(viewPager)
    }

    // 自动轮播逻辑
    private val carouselRunnable = object : Runnable {
        override fun run() {
            viewPager.currentItem = (viewPager.currentItem + 1) % PAGE_COUNT
            handler.postDelayed(this, CAROUSEL_DELAY_MS)
        }
    }

    private fun startCarousel() {
        handler.postDelayed(carouselRunnable, INITIAL_DELAY_MS)
    }

    // 点击事件处理
    private fun setupClickListeners() {
        binding.run {
            robotMainCameraBtn.setOnClickListener {
                startActivity(Intent(context, CameraActivity::class.java))
            }
            robotMainBluetoothBtn.setOnClickListener {
                startActivity(Intent(context, BluetoothLinkActivity::class.java))
            }
            robotMainUserInstructionBtn.setOnClickListener {
                startActivity(Intent(context, UserInstructionActivity::class.java))
            }
        }
    }
}