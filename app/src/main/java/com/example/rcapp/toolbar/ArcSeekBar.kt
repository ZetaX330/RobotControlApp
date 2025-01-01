package com.example.rcapp.toolbar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * 个人测试单独舵机使用的弧形进度条，无须在意
 */
class ArcSeekBar(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {
    private var backgroundPaint: Paint? = null
    private var progressPaint: Paint? = null
    private var thumbPaint: Paint? = null
    private var arcRectF: RectF? = null
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f
    private var progress = 0f
    private val maxProgress = 100f //进度条进度
    private val startAngle = 150f //进度条初始绘制角度
    private val sweepAngle = 240f //进度条绘制角度范围
    private var currentAngle = 0f //实际物理角度

    private var thumbX = 0f
    private var thumbY = 0f
    private var isDragging = false

    private var onAngleChangeListener: OnAngleChangeListener? = null

    fun setOnAngleChangeListener(listener: OnAngleChangeListener) {
        this.onAngleChangeListener = listener
    }

    init {
        init()
    }

    private fun init() {
        backgroundPaint = Paint()
        backgroundPaint!!.isAntiAlias = true
        backgroundPaint!!.style = Paint.Style.STROKE
        backgroundPaint!!.strokeWidth = 20f
        backgroundPaint!!.color = Color.GRAY

        progressPaint = Paint()
        progressPaint!!.isAntiAlias = true
        progressPaint!!.style = Paint.Style.STROKE
        progressPaint!!.strokeWidth = 20f
        progressPaint!!.color = Color.BLUE

        thumbPaint = Paint()
        thumbPaint!!.isAntiAlias = true
        thumbPaint!!.style = Paint.Style.FILL
        thumbPaint!!.color = Color.GREEN

        arcRectF = RectF()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        radius = (min(w.toDouble(), h.toDouble()) / 2f - 30).toFloat()
        arcRectF!![centerX - radius, centerY - radius, centerX + radius] = centerY + radius
        updateThumbPosition()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawArc(arcRectF!!, startAngle, sweepAngle, false, backgroundPaint!!)

        val progressSweep = (progress / maxProgress) * sweepAngle
        canvas.drawArc(arcRectF!!, startAngle, progressSweep, false, progressPaint!!)

        canvas.drawCircle(thumbX, thumbY, 30f, thumbPaint!!)

        progressPaint!!.color = Color.BLACK
        canvas.drawLine(centerX, centerY, thumbX, thumbY, progressPaint!!)
    }

    private fun updateThumbPosition() {
        val angle = startAngle + (progress / maxProgress) * sweepAngle
        val radians = Math.toRadians(angle.toDouble())
        thumbX = (centerX + radius * cos(radians)).toFloat()
        thumbY = (centerY + radius * sin(radians)).toFloat()
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                performClick()
            }

            MotionEvent.ACTION_MOVE -> if (isDragging) {
                //移动进度条滑块后调用
                updateProgressFromTouch(event.x, event.y)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isDragging = false
        }
        return true
    }

    private fun updateProgressFromTouch(x: Float, y: Float) {
        //先获取触摸位置与中心点的横纵距离
        val dx = x - centerX
        val dy = y - centerY
        //计算弧度，返回值在-pai到pai之间
        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        angle = if ((angle < 0)) angle + 360 else angle

        var calculatedAngle = angle - startAngle
        if (calculatedAngle < 0) {
            calculatedAngle += 360f
        }

        if (calculatedAngle in 0.0..sweepAngle.toDouble()) {
            currentAngle = calculatedAngle
            progress = (currentAngle / sweepAngle) * maxProgress
            updateThumbPosition()
            invalidate()

            if (onAngleChangeListener != null) {
                onAngleChangeListener?.onAngleChange(currentAngle)
            }
        }
    }

    fun setAngle(angle: Float) {
        this.progress =
            min(max(angle.toDouble(), 0.0), maxProgress.toDouble()).toFloat()
        updateThumbPosition()
        invalidate()
    }

    fun interface OnAngleChangeListener {
        fun onAngleChange(angle: Float): Boolean
    }
}