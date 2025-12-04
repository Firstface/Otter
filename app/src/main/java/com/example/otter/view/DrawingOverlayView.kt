package com.example.otter.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * 绘制遮罩视图，用于展示用户拖动的绘制区域
 */
class DrawingOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val drawingPath = Path()
    /**
     * 绘制画笔，用于绘制用户拖动的绘制区域
     */
    private val paint = Paint().apply {
        color = Color.RED
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    /**
     * 绘制用户拖动的绘制区域
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(drawingPath, paint)
    }
    /**
     * 处理用户触摸事件，用于绘制用户拖动的绘制区域
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                drawingPath.moveTo(x, y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                drawingPath.lineTo(x, y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                return true
            }
        }
        return false
    }

    /**
     * 获取当前绘制的路径
     */
    fun getDrawingPath(): Path {
        return drawingPath
    }

    /**
     * 重置绘制路径，清除用户拖动的绘制区域
     */
    fun reset() {
        drawingPath.reset()
        invalidate()
    }
}
