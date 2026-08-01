package com.app.tmarita.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.graphics.PathParser
import com.app.tmarita.model.PeruRegion

class PeruMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    private data class DrawableRegion(
        val region: PeruRegion,
        val path: Path,
        val bounds: RectF
    )

    private var drawableRegions: List<DrawableRegion> = emptyList()
    private var viewportWidth = 542.767f
    private var viewportHeight = 792f

    // baseMatrix: ajusta el viewport al tamaño de la view (fit-center).
    // userMatrix: zoom/pan del usuario, en espacio de pantalla, ENCIMA del base.
    private val baseMatrix = Matrix()
    private val userMatrix = Matrix()
    private val totalMatrix = Matrix()
    private val inverseTotalMatrix = Matrix()
    private val matrixValues = FloatArray(9)

    private var zoom = 1f
    private val minZoom = 1f
    private val maxZoom = 5f

    private val fillPaintVisited = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#8FBC8F") // verde salvia suave — ya visitado
    }
    private val fillPaintPending = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#D98880") // terracota suave — aún no visitado
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#2B3A3A") // ink
        strokeWidth = 1f
    }
    private val selectedStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#B08D57") // gold
        strokeWidth = 3f
    }

    // Etiqueta: texto con sombra suave (en vez de borde duro), se lee bien sobre cualquier color.
    private val labelFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#2B2420") // ink
        textAlign = Paint.Align.CENTER
        // Cambia la tipografía aquí. Opciones rápidas:
        //   "sans-serif-medium"  -> limpia, moderna (la que dejé activa)
        //   "sans-serif-black"   -> más gruesa/bold
        //   Typeface.SERIF       -> elegante, como el título de la app
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        setShadowLayer(4f, 0f, 1f, Color.parseColor("#80FFFFFF"))
    }
    private val labelTextSizePx = 34f // tamaño fijo en pantalla, no escala con el zoom
    private val labelPaddingPx = 16f  // margen mínimo dentro del departamento para mostrar el nombre

    var visitedIds: Set<String> = emptySet()
        set(value) {
            field = value
            invalidate()
        }

    var selectedRegionId: String? = null
        set(value) {
            field = value
            invalidate()
        }

    var onRegionClick: ((PeruRegion) -> Unit)? = null
    var onEmptyAreaClick: (() -> Unit)? = null

    fun setRegions(viewportW: Float, viewportH: Float, regions: List<PeruRegion>) {
        if (drawableRegions.size == regions.size &&
            drawableRegions.map { it.region.id } == regions.map { it.id }
        ) return

        viewportWidth = viewportW
        viewportHeight = viewportH
        drawableRegions = regions.map { region ->
            val path = PathParser.createPathFromPathData(region.pathData)
            val bounds = RectF().also { path.computeBounds(it, true) }
            DrawableRegion(region, path, bounds)
        }
        updateBaseMatrix(width, height)
        requestLayout()
        invalidate()
    }

    /** Vuelve a mostrar el mapa completo, sin zoom. */
    fun resetZoom() {
        userMatrix.reset()
        zoom = 1f
        recomputeTotalMatrix()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateBaseMatrix(w, h)
    }

    private fun updateBaseMatrix(w: Int, h: Int) {
        if (viewportWidth <= 0 || viewportHeight <= 0 || w <= 0 || h <= 0) return
        val scale = minOf(w / viewportWidth, h / viewportHeight)
        val dx = (w - viewportWidth * scale) / 2f
        val dy = (h - viewportHeight * scale) / 2f
        baseMatrix.reset()
        baseMatrix.postScale(scale, scale)
        baseMatrix.postTranslate(dx, dy)
        recomputeTotalMatrix()
    }

    private fun recomputeTotalMatrix() {
        totalMatrix.set(userMatrix)
        totalMatrix.preConcat(baseMatrix)
        totalMatrix.invert(inverseTotalMatrix)
    }

    /** Escala uniforme actual (viewport -> pantalla), para saber cuántos px mide cada departamento. */
    private fun currentScale(): Float {
        totalMatrix.getValues(matrixValues)
        return matrixValues[Matrix.MSCALE_X]
    }

    // ---- Gestos: pellizco para zoom, arrastre para desplazar ----

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val proposedZoom = (zoom * detector.scaleFactor).coerceIn(minZoom, maxZoom)
            val actualFactor = proposedZoom / zoom
            if (actualFactor != 1f) {
                userMatrix.postScale(actualFactor, actualFactor, detector.focusX, detector.focusY)
                zoom = proposedZoom
                recomputeTotalMatrix()
                invalidate()
            }
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            if (zoom > minZoom) {
                userMatrix.postTranslate(-dx, -dy)
                recomputeTotalMatrix()
                invalidate()
            }
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            resetZoom()
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val pts = floatArrayOf(e.x, e.y)
            inverseTotalMatrix.mapPoints(pts)
            val hit = findRegionAt(pts[0], pts[1])
            if (hit != null) {
                onRegionClick?.invoke(hit.region)
            } else {
                onEmptyAreaClick?.invoke()
            }
            performClick()
            return true
        }
    })

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.concat(totalMatrix)
        drawableRegions.forEach { dr ->
            val fill = if (dr.region.id in visitedIds) fillPaintVisited else fillPaintPending
            canvas.drawPath(dr.path, fill)
            canvas.drawPath(dr.path, strokePaint)
            if (dr.region.id == selectedRegionId) {
                canvas.drawPath(dr.path, selectedStrokePaint)
            }
        }
        canvas.restore()

        drawLabels(canvas)
    }

    /**
     * Dibuja el nombre de cada departamento en espacio de pantalla (fuera de la matriz de zoom,
     * para que el texto no se deforme ni se agrande con el pellizco) y SOLO si, al tamaño actual
     * de zoom, el departamento en pantalla es más grande que el propio texto + margen.
     */
    private fun drawLabels(canvas: Canvas) {
        val scale = currentScale()
        labelFillPaint.textSize = labelTextSizePx
        val textHeight = labelFillPaint.fontMetrics.let { it.descent - it.ascent }

        drawableRegions.forEach { dr ->
            val renderedWidth = dr.bounds.width() * scale
            val renderedHeight = dr.bounds.height() * scale
            val text = dr.region.title
            val textWidth = labelFillPaint.measureText(text)

            if (renderedWidth > textWidth + labelPaddingPx && renderedHeight > textHeight + labelPaddingPx) {
                val center = floatArrayOf(dr.bounds.centerX(), dr.bounds.centerY())
                totalMatrix.mapPoints(center)
                val baselineY = center[1] - (labelFillPaint.descent() + labelFillPaint.ascent()) / 2
                canvas.drawText(text, center[0], baselineY, labelFillPaint)
            }
        }
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