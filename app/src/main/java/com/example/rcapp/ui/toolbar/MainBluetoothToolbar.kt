package com.example.rcapp.ui.toolbar

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.example.rcapp.ui.viewmodel.MainToolbarViewModel
import com.example.rcapp.R
import com.example.rcapp.databinding.MainBluetoothToolbarLayoutBinding
import com.google.android.material.appbar.AppBarLayout

class MainBluetoothToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppBarLayout(context, attrs, defStyleAttr) {
    private var isBluetoothOpen:Boolean? = null

    // 绑定布局文件，初始化视图绑定对象
    private var binding: MainBluetoothToolbarLayoutBinding =
        MainBluetoothToolbarLayoutBinding.inflate(LayoutInflater.from(context), this, true)

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
        dropDeviceName()
        setBluetoothOnClickListener()
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
        binding.bluetoothNameTv.text=state.deviceName
        if(!state.isDrop){
            when (state.bluetoothStatus) {
                MainToolbarViewModel.BluetoothStatus.CLOSED -> showCLOSEDStatus()
                MainToolbarViewModel.BluetoothStatus.DISCONNECTED -> showDisconnectedStatus()
                MainToolbarViewModel.BluetoothStatus.CONNECTED -> showConnectedStatus()
                MainToolbarViewModel.BluetoothStatus.LOADING -> showLoadingAnimation()
            }
        }

        if (!state.isLoading) hideLoadingAnimation()
    }

    /**
     * 蓝牙图标的长按监听，由子类Activity实现
     */
    fun setBluetoothOnLongClickListener(listener: (View) -> Boolean) {
        binding.bluetoothManageIv.setOnLongClickListener(listener)
    }
    /**
     * 蓝牙图标的点按监听，由子类Activity实现
     */
    private fun setBluetoothOnClickListener(){
        binding.bluetoothManageIv.setOnClickListener {
            if(!isBluetoothOpen!!)
                return@setOnClickListener
            if(binding.bluetoothNameTv.visibility==View.GONE){
                expandDeviceName()
            }
            else if(binding.bluetoothNameTv.visibility==View.VISIBLE){
                dropDeviceName()
            }
        }

    }

    /**
     * MainToolBar目前总共四个状态，蓝牙关闭，蓝牙开启未连接，蓝牙开启已连接，蓝牙加载（开启连接中）
     * 前三个状态需要先隐藏加载图标，同时停止加载图标的动画
     * 由于已经在layout中绑定设备名到ViewModel，所以ViewModel中设备名更改后会直接更新UI，无需手动更新
     * 蓝牙图标src为selector，需要手动切换选中状态从而需要手动切换
     */
    // TODO: 蓝牙图标切换方式可能会放在ViewModel里
    private fun showLoadingAnimation() {
        binding.bluetoothManageIv.visibility = INVISIBLE
        binding.bluetoothIcBackgroundIv.visibility = INVISIBLE
        binding.loadingIv.visibility = VISIBLE
        val fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.load_anim)
        binding.loadingIv.startAnimation(fadeInAnimation)
    }

    //关闭状态设置图标为未选中，图标呈现灰色
    private fun showCLOSEDStatus() {
        isBluetoothOpen=false
        hideLoadingAnimation()
        binding.bluetoothManageIv.isSelected = false
        dropDeviceName()
    }
    //蓝牙开启未连接设置图标为选中，图标呈现蓝色
    private fun showDisconnectedStatus() {
        isBluetoothOpen=true
        hideLoadingAnimation()
        binding.bluetoothManageIv.isSelected = true
    }
    //蓝牙开启已连接设置图标为选中，图标呈现蓝色，设备名为已连接的设备的名
    private fun showConnectedStatus() {
        hideLoadingAnimation()
        binding.bluetoothManageIv.isSelected = true
    }
    //蓝牙加载设置图标为加载图标，通过旋转动画表现加载

    private fun  hideLoadingAnimation() {
        binding.loadingIv.clearAnimation()
        binding.loadingIv.visibility = INVISIBLE
        binding.bluetoothManageIv.visibility = VISIBLE
        binding.bluetoothIcBackgroundIv.visibility = VISIBLE

    }
    private fun dropDeviceName(){
        binding.bluetoothNameTv.animate()
            .alpha(0.25f)
            .setDuration(150)
            .withEndAction {
                binding.bluetoothNameTv.visibility = View.GONE
            }
        val color = ContextCompat.getColor(context, R.color.light_blue_gray)
        binding.cardview.setCardBackgroundColor(color)
    }
    private fun expandDeviceName(){
        binding.bluetoothNameTv.visibility = View.VISIBLE
        binding.bluetoothNameTv.alpha = 0.25f
        binding.bluetoothNameTv.animate()
            .alpha(1.0f)
            .setDuration(150)

        val color = ContextCompat.getColor(context, R.color.white)
        binding.cardview.setCardBackgroundColor(color)
    }
}