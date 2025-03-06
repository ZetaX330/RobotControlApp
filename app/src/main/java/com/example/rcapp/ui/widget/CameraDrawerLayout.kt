package com.example.rcapp.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout

class CameraDrawerLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : DrawerLayout(context, attrs, defStyleAttr) {

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // 只在抽屉打开时拦截事件
        return if (isDrawerOpen(GravityCompat.END)) {
            super.onInterceptTouchEvent(ev)
        } else {
            false
        }
    }
}