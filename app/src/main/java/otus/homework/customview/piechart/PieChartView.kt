package otus.homework.customview.piechart

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Parcelable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.ColorUtils.HSLToColor
import kotlinx.parcelize.Parcelize
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var categories = emptyList<PieChartCategory>()
    private var sectors = emptyList<PieChartSector>()
    private var paths = emptyList<Path>()

    private var lineWidth = 80f
    private var innerRect = RectF()
    private var outerRect = RectF()

    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var onCategoryClick: ((PieChartCategory) -> Unit)? = null

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            findCategoryAtPoint(e.x, e.y)?.let {
                onCategoryClick?.invoke(it)
            }

            return true
        }
    })

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        val wSize = MeasureSpec.getSize(widthMeasureSpec)
        val hSize = MeasureSpec.getSize(heightMeasureSpec)

        val w = when (wMode) {
            MeasureSpec.EXACTLY -> wSize
            MeasureSpec.AT_MOST -> wSize
            else -> wSize
        }

        val h = when (hMode) {
            MeasureSpec.EXACTLY -> hSize
            MeasureSpec.AT_MOST -> hSize
            else -> hSize
        }

        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        calculateRects(w, h)
        calculatePathsAndSectors()
    }

    override fun onDraw(canvas: Canvas) {
        paths.forEachIndexed { index, path ->
            pathPaint.color = categories.getOrNull(index)?.color ?: Color.GRAY

            canvas.drawPath(path, pathPaint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean = gestureDetector.onTouchEvent(event)

    override fun onSaveInstanceState(): Parcelable? =
        PieChartSavedState(
            superSavedState = super.onSaveInstanceState(),
            categories = categories,
            lineWidth = lineWidth
        )

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)

        if (state is PieChartSavedState) {
            categories = state.categories
            lineWidth = state.lineWidth
        }
    }

    fun setOnCategoryClick(onCategoryClick: ((PieChartCategory) -> Unit)?) {
        this.onCategoryClick = onCategoryClick
    }

    fun setLineWidth(lineWidth: Int) {
        this.lineWidth = lineWidth.toFloat()
        requestLayout()
    }

    fun setData(data: List<PieChartData>) {
        categories = data
            .groupBy { it.category }
            .toList()
            .let { groups ->
                val groupsSize = groups.size

                groups.mapIndexed { index, group ->
                    PieChartCategory(
                        name = group.first,
                        amount = group.second.sumOf { it.amount },
                        color = getCategoryColor(index, groupsSize)
                    )
                }
            }
            .sortedByDescending { it.amount }

        requestLayout()
    }

    private fun findCategoryAtPoint(x: Float, y: Float): PieChartCategory? {
        val centerX = width / 2f
        val centerY = height / 2f
        val r = min(width, height) / 2f
        val innerR = r - lineWidth
        val outerR = r

        val d = sqrt((centerX - x).pow(2) + (centerY - y).pow(2))

        if (d !in innerR..outerR) {
            return null
        }

        val theta = atan2(y - centerY, x - centerX).toDouble() + PI / 2
        var a = Math.toDegrees(theta)

        if (a < 0) {
            a += 360;
        }

        val indexOfCategory = sectors.indexOfFirst { a in it.offset..(it.offset + it.sweep) }

        return categories.getOrNull(indexOfCategory)
    }

    private fun calculateRects(w: Int, h: Int) {
        val centerX = w / 2f
        val centerY = h / 2f
        val r = min(w, h) / 2f
        val innerR = r - lineWidth
        val outerR = r

        innerRect = RectF(centerX - innerR, centerY - innerR, centerX + innerR, centerY + innerR)
        outerRect = RectF(centerX - outerR, centerY - outerR, centerX + outerR, centerY + outerR)
    }

    private fun calculatePathsAndSectors() {
        val sum = categories.sumOf { it.amount }.toFloat()
        val sweeps = categories.map { it.amount / sum }.ifEmpty { listOf(360f) }
        var offsetInDegrees = -90f
        val newPaths = mutableListOf<Path>()
        val newSectors = mutableListOf<PieChartSector>()

        sweeps.forEach { sweep ->
            val sweepInDegrees = sweep * 360f
            val path = getArcPath(offsetInDegrees, sweepInDegrees)
            val sector = PieChartSector(offsetInDegrees + 90f, sweepInDegrees)

            offsetInDegrees += sweepInDegrees

            newPaths.add(path)
            newSectors.add(sector)
        }

        paths = newPaths
        sectors = newSectors
    }

    private fun getArcPath(arcOffset: Float, arcSweep: Float): Path =
        Path().apply {
            arcTo(outerRect, arcOffset, arcSweep)
            arcTo(innerRect, arcOffset + arcSweep, -arcSweep)
            close()
        }

    private fun getCategoryColor(index: Int, size: Int): Int =
        HSLToColor(floatArrayOf(360f * index / size, 0.9f, 0.5f))

    private data class PieChartSector(
        val offset: Float,
        val sweep: Float
    )

    @Parcelize
    private class PieChartSavedState(
        val superSavedState: Parcelable?,
        val categories: List<PieChartCategory>,
        val lineWidth: Float
    ): BaseSavedState(superSavedState), Parcelable
}