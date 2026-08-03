package com.app.tmarita.view

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.DecelerateInterpolator
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

    private val baseMatrix = Matrix()
    private val userMatrix = Matrix()
    private val totalMatrix = Matrix()
    private val inverseTotalMatrix = Matrix()
    private val matrixValues = FloatArray(9)

    private var zoom = 1f
    private val minZoom = 1f
    private val maxZoom = 5f

    // ---- Colores base ----
    private val colorVisitedTop = Color.parseColor("#A9D3A5")   // verde salvia claro
    private val colorVisitedBottom = Color.parseColor("#7FAF7A") // verde salvia oscuro
    private val colorPendingTop = Color.parseColor("#E3A9A0")    // terracota claro
    private val colorPendingBottom = Color.parseColor("#C77E72") // terracota oscuro

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#2B3A3A")
        strokeWidth = 1f
    }

    // Halo de selección: varias pasadas con distinto grosor/alpha = efecto glow
    private val selectedGlowColor = Color.parseColor("#B08D57") // gold
    private val selectedGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val selectedCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = selectedGlowColor
        strokeWidth = 3f
    }

    private val labelFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#2B2420")
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        setShadowLayer(4f, 0f, 1f, Color.parseColor("#80FFFFFF"))
    }
    private val labelTextSizePx = 34f
    private val labelPaddingPx = 16f

    // ---- Progreso de animación por región: 0f = pendiente, 1f = visitado ----
    // Se anima suavemente cada vez que un id entra/sale de visitedIds, en vez de saltar de color.
    private val visitProgress = HashMap<String, Float>()
    private val activeAnimators = HashMap<String, ValueAnimator>()
    private val argbEvaluator = ArgbEvaluator()

    var visitedIds: Set<String> = emptySet()
        set(value) {
            val old = field
            field = value
            if (drawableRegions.isNotEmpty()) {
                val changed = (old - value) + (value - old) // ids cuyo estado cambió
                changed.forEach { id -> animateVisitChange(id, id in value) }
            }
            invalidate()
        }

    var selectedRegionId: String? = null
        set(value) {
            field = value
            invalidate()
        }

    var onRegionClick: ((PeruRegion) -> Unit)? = null
    var onEmptyAreaClick: (() -> Unit)? = null

    private fun animateVisitChange(regionId: String, toVisited: Boolean) {
        activeAnimators[regionId]?.cancel()
        val start = visitProgress[regionId] ?: (if (toVisited) 0f else 1f)
        val end = if (toVisited) 1f else 0f
        val animator = ValueAnimator.ofFloat(start, end).apply {
            duration = 420
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                visitProgress[regionId] = it.animatedValue as Float
                invalidate()
            }
            start()
        }
        activeAnimators[regionId] = animator
    }

    fun setRegions(viewportW: Float, viewportH: Float, regions: List<PeruRegion>) {
        if (drawableRegions.size == regions.size &&
            drawableRegions.map { it.region.id } == regions.map { it.id }
        ) return

        viewportWidth = viewportW
        viewportHeight = viewportH
        drawableRegions = regions.map { region ->
            val path = PathParser.createPathFromPathData(region.pathData)
            val bounds = RectF().also { path.computeBounds(it, true) }
            // Estado inicial de progreso: sin animación, ya sea 0 o 1
            visitProgress.putIfAbsent(region.id, if (region.id in visitedIds) 1f else 0f)
            DrawableRegion(region, path, bounds)
        }
        updateBaseMatrix(width, height)
        requestLayout()
        invalidate()
    }

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

    private fun currentScale(): Float {
        totalMatrix.getValues(matrixValues)
        return matrixValues[Matrix.MSCALE_X]
    }

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
            val progress = visitProgress[dr.region.id] ?: (if (dr.region.id in visitedIds) 1f else 0f)

            // Interpola color top/bottom según progreso (pendiente <-> visitado)
            val top = argbEvaluator.evaluate(progress, colorPendingTop, colorVisitedTop) as Int
            val bottom = argbEvaluator.evaluate(progress, colorPendingBottom, colorVisitedBottom) as Int

            // Gradiente vertical propio de cada región (da volumen, no se ve "plano")
            fillPaint.shader = LinearGradient(
                dr.bounds.left, dr.bounds.top, dr.bounds.left, dr.bounds.bottom,
                top, bottom, Shader.TileMode.CLAMP
            )

            // Sombra suave bajo las regiones ya visitadas: sensación de "relieve"
            fillPaint.setShadowLayer(4f * progress, 0f, 2f * progress, Color.parseColor("#33000000"))

            canvas.drawPath(dr.path, fillPaint)
            canvas.drawPath(dr.path, strokePaint)

            if (dr.region.id == selectedRegionId) {
                drawSelectionGlow(canvas, dr.path)
            }
        }

        canvas.restore()

        drawLabels(canvas)
    }

    /** Halo de selección: 3 pasadas de stroke, de más ancha/transparente a más fina/opaca. */
    private fun drawSelectionGlow(canvas: Canvas, path: Path) {
        val steps = listOf(
            10f to 40,   // ancho, muy transparente (el "resplandor")
            6f to 90,
            3f to 255    // núcleo, opaco
        )
        steps.forEach { (width, alpha) ->
            selectedGlowPaint.strokeWidth = width
            selectedGlowPaint.color = selectedGlowColor
            selectedGlowPaint.alpha = alpha
            canvas.drawPath(path, selectedGlowPaint)
        }
    }

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