import android.content.Context
import android.graphics.Canvas
import android.os.Build
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import androidx.core.graphics.withTranslation
import SpecifiedWidthSpan
import kotlin.math.min

/**
 * @author mipa
 * @date 2025/8/25
 * @introduction
 * 用来做自定义宽度截断的拓展texiview，支持在最后一行预留出指定宽度的截断,向下兼容至api21
 * 实现为根据layout的布局结果自行计算最后一行的宽度并截断留出reverseWidth，考虑到最后一行
 * 截断后该行变短可能会回缩至倒数第二行
 * （例如宽度为10个字符a时a aaaaaaaaaaaa会被视为两行，这个时候对最后一行截断为 a aa...就会变为一行，与原布局不符），
 * 于是使用占位span的方式，将整个最后一行设置为自定义的占位spanstr，宽度为单行宽度，并自行drawtext，占位空间内左对齐绘制文字
 */

class WidthAdaptiveTruncationTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextView(context, attrs, defStyleAttr) {
    var reverseWidth: Int = 0
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }//预留宽度

    // 截断文本所使用的 Layout
    private var mTruncatedLayout: StaticLayout? = null
    private var isTruncated = false //是否被自定义截断

    private var ellipsizeStr = "..."


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (reverseWidth == 0 || maxLines == 0 || lineCount == 0 || lineCount < maxLines) {
            isTruncated = false
        } else {
            val contentWidth = measuredWidth - paddingLeft - paddingRight
            val ellipsizeWidth = paint.measureText(ellipsizeStr)
            val lastLineWidth = layout.getLineWidth(maxLines - 1)
            val lastLineAvailableWidth = contentWidth - ellipsizeWidth - reverseWidth
            if (lastLineAvailableWidth >= 0
                && ((lastLineWidth > lastLineAvailableWidth && lineCount == maxLines)
                        || lineCount > maxLines)
            ) {
                truncate(layout, lastLineAvailableWidth, layout.getLineStart(maxLines - 1))
                isTruncated = true
            } else {
                isTruncated = false
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (isTruncated) {
            canvas.withTranslation(paddingLeft.toFloat(), paddingTop.toFloat()) {
                mTruncatedLayout?.apply {
                    paint.color = currentTextColor
                    draw(canvas)
                }
            }
        } else {
            super.onDraw(canvas)
        }
    }

    private fun truncate(
        originLayout: Layout,
        lastLineAvailableWidth: Float,
        lineStart: Int
    ) {
        val lastLineIdx = maxLines - 1
        // 最接近临界位置的字符
        val nearest = originLayout.getOffsetForHorizontal(lastLineIdx, lastLineAvailableWidth)
        var offset = min(nearest + 1,  originLayout.getLineEnd(lastLineIdx) - 1)
        //因为api返回的是字符中心点，我们需要的是右边界，因此+1并且进行下面的循环查找

        // 获取该字符右侧的位置，向前查找，不能超过可用空间
        var realStartX = originLayout.getPrimaryHorizontal(offset).toInt()
        while (offset > lineStart && realStartX > lastLineAvailableWidth) {
            offset--
            realStartX = originLayout.getPrimaryHorizontal(offset).toInt()
        }

        val contentWidth = measuredWidth - paddingLeft - paddingRight

        val normalShowPart = text.subSequence(0, lineStart)//正常显示的行
        val truncatedText = text.subSequence(lineStart, offset)//需要截断的行
        val truncatedLineText = TextUtils.concat(truncatedText, ellipsizeStr)//截断后该行的显示，就是加了个截断符

        val truncatedLineSpan = SpannableString(truncatedLineText)
        truncatedLineSpan.setSpan(
            SpecifiedWidthSpan(contentWidth),
            0,
            truncatedLineText.length,  // 对应要替换的字符范围
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )//截断后由于该行变短，很可能会布局到上一行去，这里使用一个自定义的单行长度的span阻止这种情形
        val completeText = TextUtils.concat(normalShowPart, truncatedLineSpan)



        mTruncatedLayout = createStaticLayout(
            completeText,
            originLayout.paint,
            contentWidth,
            Int.MAX_VALUE
        )
    }

    /**
     * 创建StaticLayout，兼容API 21
     */
    private fun createStaticLayout(
        text: CharSequence,
        textPaint: TextPaint,
        width: Int,
        maxLines: Int
    ): StaticLayout {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // API 23及以上使用Builder模式
            StaticLayout.Builder.obtain(text, 0, text.length, textPaint, width)
                .setMaxLines(maxLines)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setIncludePad(includeFontPadding)
                .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
                .build()
        } else {
            // API 21-22使用构造函数
            // 注意：旧版本的StaticLayout构造函数不支持直接设置maxLines，
            // 我们需要在绘制时自行处理行数限制
            StaticLayout(
                text,
                textPaint,
                width,
                Layout.Alignment.ALIGN_NORMAL,
                lineSpacingMultiplier,
                lineSpacingExtra,
                includeFontPadding
            )
        }
    }

}