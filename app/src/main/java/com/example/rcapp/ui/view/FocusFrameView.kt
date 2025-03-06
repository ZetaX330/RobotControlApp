package com.example.rcapp.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.example.rcapp.R
import com.example.rcapp.ui.viewmodel.PoseCameraViewModel
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.lang.Math.abs
import kotlin.math.max


class FocusFrameView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private lateinit var paint:Paint
    private var result:PoseLandmarkerResult?=null
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
    private var isAutoFocus:Boolean?=null
    private var scaleFactor: Float = 1f
    private var centerX: Float? = null
    private var centerY: Float? = null
    private var minX = Float.MAX_VALUE
    private var minY = Float.MAX_VALUE
    private var maxX = Float.MIN_VALUE
    private var maxY = Float.MIN_VALUE
    private val cornerLength = 100f
    private val lineLength   = 40f
    private val cornerRadius = 20f
    private var exposure  = 0


    private fun initCustomPaint(){
        paint = Paint().apply {
            color = Color.LTGRAY // 设置绘制颜色
            strokeWidth = 8f // 设置线条宽度
            style = Paint.Style.STROKE // 设置为描边模式
        }
    }
    private fun initLockPaint() {
        paint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.orange)
            strokeWidth = 8f // 设置线条宽度
            style = Paint.Style.STROKE // 设置为描边模式
        }
    }
    fun clear() {
        result = null
        centerX = null
        centerY = null
        invalidate()
    }
    // 设置中心点
    fun drawFocusFrame(x: Float, y: Float) {
        centerX = x
        centerY = y
        isAutoFocus=false
        exposure=0
        initCustomPaint()
        invalidate() // 请求重绘
    }
    fun drawLockFocusFrame(x: Float, y: Float) {
        centerX = x
        centerY = y
        isAutoFocus=false
        exposure=0
        initLockPaint()
        invalidate() // 请求重绘
    }
    fun drawAutoFocusFrame(poseLandmarkerResults: PoseLandmarkerResult, imageHeight: Int, imageWidth: Int) {
        scaleFactor=max(width * 1f / imageWidth, height * 1f / imageHeight)
        result=poseLandmarkerResults
        this.imageWidth=imageWidth
        this.imageHeight=imageHeight
        isAutoFocus=true
        initCustomPaint()
        invalidate() // 请求重绘

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if(isAutoFocus == true) {
            result?.let { poseLandmarkerResult ->
                // 每个 landmark 代表一帧的识别结果
                for (landmark in poseLandmarkerResult.landmarks()) {
                    // 重置追焦框的定位点
                    minX = Float.MAX_VALUE
                    minY = Float.MAX_VALUE
                    maxX = Float.MIN_VALUE
                    maxY = Float.MIN_VALUE
                    // 遍历 landmark 中的关节点
                    for (normalizedLandmark in landmark) {
                        Log.d("landmark", landmark.size.toString())

                        val x = normalizedLandmark.x() * imageWidth * scaleFactor
                        val y = normalizedLandmark.y() * imageHeight * scaleFactor
                        // 更新最小和最大值
                        if (x < minX) minX = x
                        if (y < minY) minY = y
                        if (x > maxX) maxX = x
                        if (y > maxY) maxY = y
                    }
                    // 左上角
                    canvas.drawLine(
                        minX,
                        minY - cornerRadius,
                        minX + lineLength,
                        minY - cornerRadius,
                        paint)
                    canvas.drawLine(
                        minX -  cornerRadius,
                        minY,
                        minX -  cornerRadius,
                        minY + lineLength,
                        paint)
                    canvas.drawArc(
                        minX - cornerRadius,
                        minY - cornerRadius,
                        minX + cornerRadius,
                        minY + cornerRadius,
                        180f,
                        90f,
                        false, paint)

                    // 右上角
                    canvas.drawLine(
                        maxX,
                        minY - cornerRadius,
                        maxX - lineLength,
                        minY - cornerRadius,
                        paint)
                    canvas.drawLine(
                        maxX +  cornerRadius,
                        minY,
                        maxX +  cornerRadius,
                        minY + lineLength,
                        paint)
                    canvas.drawArc(
                        maxX - cornerRadius,
                        minY - cornerRadius,
                        maxX + cornerRadius,
                        minY + cornerRadius,
                        270f,
                        90f,
                        false, paint)

                    // 左下角
                    canvas.drawLine(
                        minX,
                        maxY + cornerRadius,
                        minX + lineLength,
                        maxY + cornerRadius,
                        paint)
                    canvas.drawLine(
                        minX -  cornerRadius,
                        maxY,
                        minX -  cornerRadius,
                        maxY - lineLength,
                        paint)
                    canvas.drawArc(
                        minX - cornerRadius,
                        maxY - cornerRadius,
                        minX + cornerRadius,
                        maxY + cornerRadius,
                        90f,
                        90f,
                        false, paint)

                    // 右下角
                    canvas.drawLine(
                        maxX,
                        maxY + cornerRadius,
                        maxX - lineLength,
                        maxY + cornerRadius,
                        paint)
                    canvas.drawLine(
                        maxX + cornerRadius,
                        maxY,
                        maxX + cornerRadius,
                        maxY - lineLength,
                        paint)
                    canvas.drawArc(
                        maxX - cornerRadius,
                        maxY - cornerRadius,
                        maxX + cornerRadius,
                        maxY + cornerRadius,
                        0f,
                        90f,
                        false, paint)
                }


            }

        }
        else if(isAutoFocus==false){
            // 左上角
            centerX?.let { x ->
                centerY?.let { y->
                    canvas.drawLine(
                        x - cornerLength,
                        y - cornerLength - cornerRadius,
                        x - cornerLength + lineLength,
                        y - cornerLength - cornerRadius,
                        paint)
                    canvas.drawLine(
                        x - cornerLength - cornerRadius,
                        y - cornerLength,
                        x - cornerLength - cornerRadius,
                        y - cornerLength + lineLength,
                        paint)
                    canvas.drawArc(
                        x - cornerLength-cornerRadius,
                        y - cornerLength-cornerRadius,
                        x - cornerLength+cornerRadius,
                        y - cornerLength+cornerRadius,
                        180f,
                        90f,
                        false, paint)

                    //右上角
                    canvas.drawLine(
                        x + cornerLength,
                        y - cornerLength - cornerRadius,
                        x + cornerLength - lineLength,
                        y - cornerLength - cornerRadius,
                        paint)
                    canvas.drawLine(
                        x + cornerLength + cornerRadius,
                        y - cornerLength,
                        x + cornerLength + cornerRadius,
                        y - cornerLength + lineLength,
                        paint)
                    canvas.drawArc(
                        x + cornerLength-cornerRadius,
                        y - cornerLength-cornerRadius,
                        x + cornerLength+cornerRadius,
                        y - cornerLength+cornerRadius,
                        270f,
                        90f,
                        false, paint)

                    //左下角
                    canvas.drawLine(
                        x - cornerLength,
                        y + cornerLength + cornerRadius,
                        x - cornerLength + lineLength,
                        y + cornerLength + cornerRadius,
                        paint)
                    canvas.drawLine(
                        x - cornerLength - cornerRadius,
                        y + cornerLength,
                        x - cornerLength - cornerRadius,
                        y + cornerLength - lineLength,
                        paint)
                    canvas.drawArc(
                        x - cornerLength-cornerRadius,
                        y + cornerLength-cornerRadius,
                        x - cornerLength+cornerRadius,
                        y + cornerLength+cornerRadius,
                        90f,
                        90f,
                        false, paint)

                    //右下角
                    canvas.drawLine(
                        x + cornerLength,
                        y + cornerLength + cornerRadius,
                        x + cornerLength - lineLength,
                        y + cornerLength + cornerRadius,
                        paint)
                    canvas.drawLine(
                        x + cornerLength + cornerRadius,
                        y + cornerLength,
                        x + cornerLength + cornerRadius,
                        y + cornerLength - lineLength,
                        paint)
                    canvas.drawArc(
                        x + cornerLength-cornerRadius,
                        y + cornerLength-cornerRadius,
                        x + cornerLength+cornerRadius,
                        y + cornerLength+cornerRadius,
                        0f,
                        90f,
                        false, paint)
                }
            }

        }
    }
}