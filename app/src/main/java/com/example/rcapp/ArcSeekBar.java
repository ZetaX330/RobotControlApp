package com.example.rcapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

public class ArcSeekBar extends View {
    private Paint backgroundPaint;
    private Paint progressPaint;
    private Paint thumbPaint;
    private RectF arcRectF;
    private float centerX;
    private float centerY;
    private float radius;
    private float progress = 0;
    private final float maxProgress = 100;//进度条进度
    private final float startAngle = 150;//进度条初始绘制角度
    private final float sweepAngle = 240;//进度条绘制角度范围
    private float currentAngle = 0;//实际物理角度

    private float thumbX;
    private float thumbY;
    private boolean isDragging = false;

    private OnAngleChangeListener onAngleChangeListener;

    public ArcSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        backgroundPaint = new Paint();
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(20);
        backgroundPaint.setColor(Color.GRAY);

        progressPaint = new Paint();
        progressPaint.setAntiAlias(true);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(20);
        progressPaint.setColor(Color.BLUE);

        thumbPaint = new Paint();
        thumbPaint.setAntiAlias(true);
        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setColor(Color.GREEN);

        arcRectF = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        radius = Math.min(w, h) / 2f - 30;
        arcRectF.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        updateThumbPosition();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawArc(arcRectF, startAngle, sweepAngle, false, backgroundPaint);

        float progressSweep = (progress / maxProgress) * sweepAngle;
        canvas.drawArc(arcRectF, startAngle, progressSweep, false, progressPaint);

        canvas.drawCircle(thumbX, thumbY, 30, thumbPaint);

        progressPaint.setColor(Color.BLACK);
        canvas.drawLine(centerX, centerY, thumbX, thumbY, progressPaint);
    }

    private void updateThumbPosition() {
        float angle = startAngle + (progress / maxProgress) * sweepAngle;
        double radians = Math.toRadians(angle);
        thumbX = (float) (centerX + radius * Math.cos(radians));
        thumbY = (float) (centerY + radius * Math.sin(radians));
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDragging = true;
                performClick();
//                updateProgressFromTouch(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    //移动进度条滑块后调用
                    updateProgressFromTouch(event.getX(), event.getY());
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                break;
        }
        return true;
    }

    private void updateProgressFromTouch(float x, float y) {
        //先获取触摸位置与中心点的横纵距离
        float dx = x - centerX;
        float dy = y - centerY;
        //计算弧度，返回值在-pai到pai之间
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        angle = (angle < 0) ? angle + 360 : angle;

        float calculatedAngle = angle - startAngle;
        if (calculatedAngle < 0) {
            calculatedAngle += 360;
        }

        if (calculatedAngle >= 0 && calculatedAngle <= sweepAngle) {
            currentAngle = calculatedAngle;
            progress = (currentAngle / sweepAngle) * maxProgress;
            updateThumbPosition();
            invalidate();

            if (onAngleChangeListener != null) {
                onAngleChangeListener.onAngleChanged(currentAngle);
            }
        }
    }

    public void setOnAngleChangeListener(OnAngleChangeListener listener) {
        this.onAngleChangeListener = listener;
    }

    public void setAngle(float angle) {
        this.progress = Math.min(Math.max(angle, 0), maxProgress);
        updateThumbPosition();
        invalidate();
    }

    public interface OnAngleChangeListener {
        void onAngleChanged(float angle);
    }
}