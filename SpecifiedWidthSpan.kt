import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan

/**
 * @author mipa
 * @date 2025/8/28
 * @introduction
 * 指定宽度的span，文字在其内左对齐
 */
class SpecifiedWidthSpan(val width: Int) : ReplacementSpan() {

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        return width
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        canvas.drawText(text, start, end, x, y.toFloat(), paint)
    }
}