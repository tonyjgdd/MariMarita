package com.app.tmarita.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.PathParser
import com.app.tmarita.model.PeruRegion

/**
 * Mapa interactivo de Perú por departamentos.
 * No sabe nada de Room, Repository ni JSON: solo recibe regiones + ids visitados
 * y dibuja. Todo lo demás lo maneja el ViewModel/Fragment.
 */
class PeruMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class DrawableRegion(
        val region: PeruRegion,
        val path: Path,
        val bounds: RectF
    )

    private var drawableRegions: List<DrawableRegion> = emptyList()
    private var viewportWidth = 542.767f
    private var viewportHeight = 792f
    private val matrix = Matrix()
    private val inverseMatrix = Matrix()

    private val fillPaintVisited = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#4CAF50")
    }
    private val fillPaintPending = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#CFD8DC")
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#607D8B")
        strokeWidth = 1.2f
    }

    var visitedIds: Set<String> = emptySet()
        set(value) {
            field = value
            invalidate()
        }

    var onRegionClick: ((PeruRegion) -> Unit)? = null

    /** Llamar una vez que el ViewModel entregue la geometría (no cambia después). */
    fun setRegions(viewportW: Float, viewportH: Float, regions: List<PeruRegion>) {
        if (viewportWidth == viewportW && drawableRegions.size == regions.size &&
            drawableRegions.map { it.region.id } == regions.map { it.id }
        ) return // evita reparsear paths en cada emisión del StateFlow

        viewportWidth = viewportW
        viewportHeight = viewportH
        drawableRegions = regions.map { region ->
            val path = PathParser.createPathFromPathData(region.pathData)
            val bounds = RectF().also { path.computeBounds(it, true) }
            DrawableRegion(region, path, bounds)
        }
        updateMatrix(width, height)
        requestLayout()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateMatrix(w, h)
    }

    private fun updateMatrix(w: Int, h: Int) {
        if (viewportWidth <= 0 || viewportHeight <= 0 || w <= 0 || h <= 0) return
        val scale = minOf(w / viewportWidth, h / viewportHeight)
        val dx = (w - viewportWidth * scale) / 2f
        val dy = (h - viewportHeight * scale) / 2f
        matrix.reset()
        matrix.postScale(scale, scale)
        matrix.postTranslate(dx, dy)
        matrix.invert(inverseMatrix)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.concat(matrix)
        drawableRegions.forEach { dr ->
            val fill = if (dr.region.id in visitedIds) fillPaintVisited else fillPaintPending
            canvas.drawPath(dr.path, fill)
            canvas.drawPath(dr.path, strokePaint)
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val pts = floatArrayOf(event.x, event.y)
            inverseMatrix.mapPoints(pts)
            findRegionAt(pts[0], pts[1])?.let { onRegionClick?.invoke(it.region) }
            performClick()
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun findRegionAt(x: Float, y: Float): DrawableRegion? {
        val region = android.graphics.Region()
        return drawableRegions.firstOrNull { dr ->
            if (!dr.bounds.contains(x, y)) return@firstOrNull false
            region.setPath(
                dr.path,
                android.graphics.Region(
                    dr.bounds.left.toInt(), dr.bounds.top.toInt(),
                    dr.bounds.right.toInt() + 1, dr.bounds.bottom.toInt() + 1
                )
            )
            region.contains(x.toInt(), y.toInt())
        }
    }
}
