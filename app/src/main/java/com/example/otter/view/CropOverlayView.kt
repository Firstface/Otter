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

    /**
     * A callback to notify the listener that the user has started to draw a crop rectangle.
     */
    var onDragStarted: (() -> Unit)? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        // Make the background mask brighter (50% transparent black)
        backgroundPaint.color = Color.parseColor("#80000000")

        borderPaint.style = Paint.Style.STROKE
        borderPaint.color = Color.WHITE
        borderPaint.strokeWidth = 4f

        clearPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        if (!cropRect.isEmpty) {
            canvas.drawRect(cropRect, clearPaint)
            canvas.drawRect(cropRect, borderPaint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x.coerceIn(0f, width.toFloat())
        val y = event.y.coerceIn(0f, height.toFloat())

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startPoint = Pair(x, y)
                cropRect.set(x, y, x, y)
                isDrawing = true
                // Don't invalidate here, wait for the first move
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDrawing) {
                    // On the very first move, notify the listener to show the confirm button
                    if (cropRect.width() == 0f && cropRect.height() == 0f) {
                        onDragStarted?.invoke()
                    }

                    cropRect.set(
                        startPoint.first.coerceAtMost(x),
                        startPoint.second.coerceAtMost(y),
                        startPoint.first.coerceAtLeast(x),
                        startPoint.second.coerceAtLeast(y)
                    )
                    invalidate() // Update the rectangle as the user drags
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                isDrawing = false
                return true
            }
        }
        return false
    }

    fun getCropRect(): RectF {
        return if (cropRect.isEmpty) RectF() else cropRect
    }

    fun reset() {
        if (!cropRect.isEmpty) {
            cropRect.setEmpty()
            invalidate()
        }
    }
}