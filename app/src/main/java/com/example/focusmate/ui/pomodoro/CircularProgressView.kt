package com.example.focusmate.ui.pomodoro

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress: Float = 0f
    private var maxProgress: Float = 100f

    private val progressPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#40ffffff")
        strokeWidth = 2f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val oval = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val size = min(w, h)
        val strokeWidth = progressPaint.strokeWidth
        val padding = strokeWidth / 2

        oval.set(
            padding,
            padding,
            size - padding,
            size - padding
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background circle
        canvas.drawOval(oval, backgroundPaint)

        // Draw progress arc
        if (progress > 0) {
            val sweepAngle = (progress / maxProgress) * 360f
            canvas.drawArc(oval, -90f, sweepAngle, false, progressPaint)
        }
    }

    fun setProgress(progress: Float) {
        this.progress = progress
        invalidate()
    }

    fun getProgress(): Float = progress

    fun setMaxProgress(maxProgress: Float) {
        this.maxProgress = maxProgress
        invalidate()
    }

    fun getMaxProgress(): Float = maxProgress
}