package com.example.otter.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.otter.R

/**
 * 裁剪区域遮罩视图，用于展示用户拖动的裁剪区域
 */
class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint()
    private val borderPaint = Paint()
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var startPoint = Pair(0f, 0f)
    private var cropRect = RectF()

    private var isDrawing = false
    private var isDrawingFinished = false

    var onDragStarted: (() -> Unit)? = null

    /**
     * 当视图被创建时，初始化画笔属性
     */
    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        backgroundPaint.color = ContextCompat.getColor(context, R.color.crop_mask_color)

        borderPaint.style = Paint.Style.STROKE
        borderPaint.color = Color.WHITE
        borderPaint.strokeWidth = 4f

        clearPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    /**
     * 当视图被绘制时，绘制裁剪区域遮罩
     */
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        if (!cropRect.isEmpty) {
            canvas.drawRect(cropRect, clearPaint)
            canvas.drawRect(cropRect, borderPaint)
        }
    }
    /**
     * 当视图被触摸时，处理用户拖动裁剪区域的操作
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
       if (isDrawingFinished) {
            return false
        }

        val x = event.x.coerceIn(0f, width.toFloat())
        val y = event.y.coerceIn(0f, height.toFloat())

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startPoint = Pair(x, y)
                cropRect.set(x, y, x, y)
                isDrawing = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDrawing) {
                    if (cropRect.width() == 0f && cropRect.height() == 0f) {
                        onDragStarted?.invoke()
                    }

                    cropRect.set(
                        startPoint.first.coerceAtMost(x),
                        startPoint.second.coerceAtMost(y),
                        startPoint.first.coerceAtLeast(x),
                        startPoint.second.coerceAtLeast(y)
                    )
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                isDrawing = false
                isDrawingFinished = true // Lock the crop rect
                return true
            }
        }
        return false
    }


        /**
         * 计算裁剪区域与图片显示区域的有效交集
         *
         * @param imageBounds 图片显示区域的 RectF
         * @return 有效交集的 RectF，若无交集则返回 null
         */
    fun getValidCropRect(imageBounds: RectF): RectF? {
        val intersection = RectF(cropRect)
        if (intersection.intersect(imageBounds)) {
            return intersection
        }
        return null
    }

        /**
         * 重置裁剪区域遮罩，允许用户绘制新的矩形
         */
    fun reset() {
        if (!cropRect.isEmpty) {
            cropRect.setEmpty()
            invalidate()
        }
        isDrawingFinished = false // Allow drawing again
    }
}