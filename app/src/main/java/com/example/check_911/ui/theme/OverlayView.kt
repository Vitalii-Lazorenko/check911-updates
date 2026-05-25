package com.example.check_911.ui.theme

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import kotlin.math.min

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dimColor = ColorUtils.setAlphaComponent(Color.BLACK, 200) // затемнение
    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    private var frameRect = RectF()
    private var isGreen = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val W = width.toFloat()
        val H = height.toFloat()
        val size = min(W, H) * 0.65f

        val left = (W - size) / 2f
        val top = (H - size) / 2f
        val right = left + size
        val bottom = top + size
        frameRect.set(left, top, right, bottom)

        // === создаём слой для маски ===
        val layer = canvas.saveLayer(0f, 0f, W, H, null)

        // 1️⃣ Нарисуем сплошное затемнение
        val dimPaint = Paint().apply { color = dimColor }
        canvas.drawRect(0f, 0f, W, H, dimPaint)

        // 2️⃣ Создаём прозрачную зону с мягким квадратным градиентом
        val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }

        // Для квадратного градиента создаём временный bitmap
        val gradientBitmap = Bitmap.createBitmap(W.toInt(), H.toInt(), Bitmap.Config.ARGB_8888)
        val gCanvas = Canvas(gradientBitmap)

        val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        blurPaint.color = Color.BLACK
        // Чем больше blur — тем мягче переход
        blurPaint.maskFilter = BlurMaskFilter(80f, BlurMaskFilter.Blur.NORMAL)
        gCanvas.drawRect(frameRect, blurPaint)

        gradientPaint.shader = BitmapShader(gradientBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        // 3️⃣ Вырезаем прозрачную часть
        canvas.drawRect(0f, 0f, W, H, gradientPaint)

        // Завершаем слой
        canvas.restoreToCount(layer)

        // 4️⃣ Рисуем рамку поверх
        framePaint.color = if (isGreen) Color.GREEN else Color.WHITE
        drawCorners(canvas)
    }

    private fun drawCorners(canvas: Canvas) {
        val l = frameRect.left
        val t = frameRect.top
        val r = frameRect.right
        val b = frameRect.bottom
        val len = 50f

        // верхний левый
        canvas.drawLine(l, t, l + len, t, framePaint)
        canvas.drawLine(l, t, l, t + len, framePaint)
        // верхний правый
        canvas.drawLine(r, t, r - len, t, framePaint)
        canvas.drawLine(r, t, r, t + len, framePaint)
        // нижний левый
        canvas.drawLine(l, b, l + len, b, framePaint)
        canvas.drawLine(l, b, l, b - len, framePaint)
        // нижний правый
        canvas.drawLine(r, b, r - len, b, framePaint)
        canvas.drawLine(r, b, r, b - len, framePaint)
    }

    /** Подсветка рамки зелёным при успешном сканировании */
    fun highlightSuccess(durationMs: Long = 1000L) {
        isGreen = true
        invalidate()
        postDelayed({
            isGreen = false
            invalidate()
        }, durationMs)
    }
}
