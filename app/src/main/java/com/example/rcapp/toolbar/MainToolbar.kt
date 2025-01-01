package com.example.rcapp.toolbar

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.example.rcapp.viewmodel.MainToolbarViewModel
import com.example.rcapp.R
import com.example.rcapp.databinding.ToolbarLayoutBinding
import com.google.android.material.appbar.AppBarLayout

class MainToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppBarLayout(context, attrs, defStyleAttr) {

    // 绑定布局文件，初始化视图绑定对象
    private var binding: ToolbarLayoutBinding =
        ToolbarLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    val toolbar: Toolbar
        get() = binding.mainTb

    init {
        // 在视图完成布局后设置 ViewModel
        post { setViewModel() }
    }

    //MainToolbarViewModel的设置
    fun setViewModel() {
        val lifecycleOwner = findViewTreeLifecycleOwner()
        if (lifecycleOwner != null) {
            // 绑定 MainToolbarViewModel和MainToolbar生命周期
            binding.viewModel = MainToolbarViewModel
            binding.lifecycleOwner = lifecycleOwner
            // 观察 MainToolbarViewModel 的状态变化
            observeViewModel(lifecycleOwner)
        } else {
            // 如果生命周期所有者为 null，记录错误日志
            Log.e("MainToolbar", "LifecycleOwner is null")
        }
    }

    /**
     * 观察 MainToolbarViewModel 的状态变化并更新 UI
     * observe用lifecycleOwner做参数，lifecycleOwner上一步被绑定到视图binding
     * observe是LiveData的方法，MainToolbarViewModel调用observe，从而实现ViewModel数据变化感知
     *
     */

    private fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        //获取 MainToolbar 的 state
        MainToolbarViewModel.toolbarState.observe(lifecycleOwner) { state ->
            //更新UI
            updateUI(state)
        }
    }

    /**
     * MainToolbarViewModel的LiveData设置ToolbarState为参数
     * View 则可以感知到 ToolbarState的变化
     */
    private fun updateUI(state: MainToolbarViewModel.ToolbarState) {
        binding.bluetoothNameTv.text = state.deviceName
        when (state.bluetoothStatus) {
            MainToolbarViewModel.BluetoothStatus.CLOSED -> showCLOSEDStatus()
            MainToolbarViewModel.BluetoothStatus.DISCONNECTED -> showDisconnectedStatus()
            MainToolbarViewModel.BluetoothStatus.CONNECTED -> showConnectedStatus()
            MainToolbarViewModel.BluetoothStatus.LOADING -> showLoadingAnimation()
        }
        if (!state.isLoading) hideLoadingAnimation()
    }

    //蓝牙图标的长按监听，由其他Activity实现
    fun setBluetoothOnLongClickListener(listener: OnLongClickListener?) {
        binding.bluetoothManageIv.setOnLongClickListener(listener)
    }

    private fun showLoadingAnimation() {
        val fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.load_anim)
        binding.bluetoothManageIv.visibility = INVISIBLE
        binding.bluetoothIcBackgroundIv.visibility = INVISIBLE
        binding.loadingIv.visibility = VISIBLE
        binding.loadingIv.startAnimation(fadeInAnimation)
    }

    /**
     * MainToolBar目前总共四个状态，蓝牙关闭，蓝牙开启未连接，蓝牙开启已连接，蓝牙加载（开启连接中）
     * 前三个状态需要先隐藏加载图标，同时停止加载图标的动画
     * 由于已经在layout中绑定设备名到ViewModel，所以ViewModel中设备名更改后会直接更新UI，无需手动更新
     * 蓝牙图标src为selector，需要手动切换选中状态从而需要手动切换
     */
    // TODO: 蓝牙图标切换方式可能会放在ViewModel里
    //关闭状态设置图标为未选中，图标呈现灰色
    private fun showCLOSEDStatus() {
        hideLoadingAnimation()
        binding.bluetoothManageIv.isSelected = false
    }
    //蓝牙开启未连接设置图标为选中，图标呈现蓝色
    private fun showDisconnectedStatus() {
        hideLoadingAnimation()
        binding.bluetoothManageIv.isSelected = true
    }
    //蓝牙开启已连接设置图标为选中，图标呈现蓝色，设备名为已连接的设备的名
    private fun showConnectedStatus() {
        hideLoadingAnimation()
        binding.bluetoothManageIv.isSelected = true
    }
    //蓝牙加载设置图标为加载图标，通过旋转动画表现加载，设备名为设置为"连接中"

    private fun hideLoadingAnimation() {
        binding.loadingIv.clearAnimation()
        binding.loadingIv.visibility = INVISIBLE
        binding.bluetoothManageIv.visibility = VISIBLE
        binding.bluetoothIcBackgroundIv.visibility = VISIBLE
    }
}