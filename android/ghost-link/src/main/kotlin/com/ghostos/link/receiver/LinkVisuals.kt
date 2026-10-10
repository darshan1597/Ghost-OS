package com.ghostos.link.receiver

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan

internal class LinkHeader(context: Context) : FrameLayout(context) {
    init {
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        addView(ImageView(context).apply { setImageResource(R.drawable.link_hero); scaleType = ImageView.ScaleType.FIT_CENTER; importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }, LayoutParams(dp(142), dp(142), Gravity.RIGHT or Gravity.TOP))
        val brand = SpannableString("Ghost Link")
        val colors = intArrayOf(0xFF00BDFF.toInt(), 0xFF3465FF.toInt(), 0xFF7852FF.toInt(), 0xFFB52EFF.toInt())
        for (i in 0..3) brand.setSpan(ForegroundColorSpan(colors[i]), 6 + i, 7 + i, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        addView(TextView(context).apply { text = brand; textSize = 30f; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) }, LayoutParams(-2, -2, Gravity.TOP or Gravity.LEFT).apply { topMargin = dp(29) })
        addView(TextView(context).apply { text = "Your other phone.\nYour control."; textSize = 13f; setTextColor(0xFFB4C8EE.toInt()); setLineSpacing(dp(2).toFloat(), 1f) }, LayoutParams(dp(190), -2, Gravity.TOP or Gravity.LEFT).apply { topMargin = dp(77) })
    }
}
internal class LinkBackdrop : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat(); val h = bounds.height().toFloat()
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(w * .25f, h * .2f, h, intArrayOf(0xFF06142B.toInt(), 0xFF020611.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawRect(bounds, paint)
        fun wave(y: Float, flip: Boolean) {
            val path = Path().apply {
                moveTo(0f, y); cubicTo(w * .25f, y + h * .16f, w * .42f, y + h * .05f, w * .64f, y + h * .025f)
                cubicTo(w * .82f, y - h * .005f, w * .95f, y + h * .015f, w, y + h * .07f)
                lineTo(w, y + h * .13f); cubicTo(w * .7f, y + h * .035f, w * .3f, y + h * .22f, 0f, y + h * .035f); close()
            }
            paint.shader = LinearGradient(0f, y, w, y + h * .12f, intArrayOf(0xC000237D.toInt(), 0xA00B1684.toInt(), 0x2000103B), null, Shader.TileMode.CLAMP)
            canvas.save()
            if (flip) canvas.scale(-1f, 1f, w / 2, h / 2)
            canvas.drawPath(path, paint)
            paint.shader = null; paint.color = 0x661B64E9; paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.5f
            canvas.drawPath(path, paint); paint.style = Paint.Style.FILL; canvas.restore()
        }
        wave(-h * .02f, false); wave(h * .78f, true)
        paint.shader = null
    }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(filter: ColorFilter?) { paint.colorFilter = filter }
    @Suppress("DEPRECATION") override fun getOpacity() = PixelFormat.TRANSLUCENT
}
internal class LinkIcon(context: Context, private val kind: String, private val badge: Boolean = true, private val red: Boolean = false) : View(context) {
    var active = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save(); canvas.scale(width / 64f, height / 64f)
        if (badge) {
            paint.style = Paint.Style.FILL
            paint.shader = RadialGradient(29f, 23f, 36f, intArrayOf(0xFF0A2857.toInt(), 0xFF051026.toInt()), null, Shader.TileMode.CLAMP)
            canvas.drawCircle(32f, 32f, 30f, paint)
            paint.shader = null; paint.style = Paint.Style.STROKE; paint.strokeWidth = 1f; paint.color = if (kind == "mic") 0xFF643CDA.toInt() else 0xFF0F5CAF.toInt()
            canvas.drawCircle(32f, 32f, 30f, paint)
        }
        paint.shader = null; paint.style = Paint.Style.STROKE; paint.strokeWidth = 2.5f; paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND
        paint.color = if (red) 0xFFFF674A.toInt() else if (kind == "bubble") 0xFF77EAFF.toInt() else 0xFFBBCEFF.toInt()
        fun line(x: Float, y: Float, xx: Float, yy: Float) = canvas.drawLine(x, y, xx, yy, paint)
        when(kind) {
            "bluetooth" -> {
                val path = Path().apply { moveTo(31f, 13f); lineTo(44f, 25f); lineTo(21f, 43f); moveTo(31f, 13f); lineTo(31f, 51f); lineTo(44f, 39f); lineTo(21f, 21f) }
                canvas.drawPath(path, paint)
                if (badge) { paint.color = if (active) 0xFF00E5AE.toInt() else 0xFFFFAD22.toInt(); paint.style = Paint.Style.FILL; canvas.drawCircle(53f, 50f, 4f, paint) }
            }
            "phone" -> { canvas.drawRoundRect(23f, 13f, 41f, 51f, 3f, 3f, paint); line(29f, 18f, 35f, 18f); canvas.drawCircle(32f, 46f, .8f, paint) }
            "bubble" -> { for (angle in 0..3) canvas.drawArc(RectF(17f, 17f, 47f, 47f), angle * 90f + 10, 60f, false, paint) }
            "refresh" -> { canvas.drawArc(RectF(18f, 18f, 46f, 46f), 30f, 280f, false, paint); line(44f, 16f, 44f, 26f); line(44f, 26f, 35f, 26f) }
            "mic" -> { canvas.drawRoundRect(27f, 15f, 37f, 35f, 5f, 5f, paint); canvas.drawArc(RectF(22f, 25f, 42f, 42f), 0f, 180f, false, paint); line(32f, 42f, 32f, 50f); line(26f, 50f, 38f, 50f); line(16f, 14f, 49f, 51f) }
            "info" -> { paint.style = Paint.Style.FILL; canvas.drawCircle(32f, 32f, 17f, paint); paint.color = 0xFF06152C.toInt(); paint.strokeWidth = 3f; canvas.drawCircle(32f, 23f, 2f, paint); line(32f, 30f, 32f, 42f) }
            "bulb" -> { canvas.drawArc(RectF(21f, 16f, 43f, 38f), 150f, 240f, false, paint); line(23f, 34f, 27f, 43f); line(41f, 34f, 37f, 43f); line(27f, 43f, 37f, 43f); line(28f, 48f, 36f, 48f); for(i in 0..4) { val a = Math.toRadians((i * 45 + 180).toDouble()); line(32f + kotlin.math.cos(a).toFloat() * 20, 27f + kotlin.math.sin(a).toFloat() * 20, 32f + kotlin.math.cos(a).toFloat() * 25, 27f + kotlin.math.sin(a).toFloat() * 25) } }
            "disconnect" -> { canvas.drawArc(RectF(14f, 21f, 37f, 42f), 90f, 250f, false, paint); canvas.drawArc(RectF(28f, 21f, 51f, 42f), -90f, 250f, false, paint); line(18f, 15f, 47f, 49f) }
            else -> { val path = Path().apply { moveTo(22f, 41f); cubicTo(22f, 21f, 41f, 16f, 48f, 16f); cubicTo(48f, 33f, 36f, 42f, 22f, 41f); close() }; canvas.drawPath(path, paint); canvas.drawCircle(38f, 26f, 4f, paint); line(20f, 43f, 16f, 49f); line(25f, 45f, 22f, 51f) }
        }
        paint.shader = null; canvas.restore()
    }
}
