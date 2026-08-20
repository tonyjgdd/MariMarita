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
    // Departamentos Visitados (Verde bonito con volumen)
    private val colorVisitedTop = Color.parseColor("#A3D9A5")   // Verde claro
    private val colorVisitedBottom = Color.parseColor("#6EB872") // Verde vivo

    // Departamentos Pendientes / No visitados (Blanco puro)
    private val colorPendingTop = Color.parseColor("#FAFAFA")    // Blanco
    private val colorPendingBottom = Color.parseColor("#FAFAFA") // Blanco

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Borde de los departamentos (Plomo / Gris)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#B0B0B0") // Plomo suave
        strokeWidth = 1.2f
    }






    // Halo de selección: color de acento distinto al borde normal, para que se note
    // claramente sin importar el zoom o el estado (visitado/pendiente) del departamento.
    private val selectedAccentColor = Color.parseColor("#2D6CDF") // azul, estándar de "seleccionado" en apps de mapas
    private val selectedFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = selectedAccentColor
        alpha = 45 // relleno muy sutil, solo para "tintar" el departamento seleccionado
    }
    private val selectedGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = selectedAccentColor
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // ---- Estilo de etiquetas: pequeñas, negritas, sutiles ----
    private val labelFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#2B2420")
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        setShadowLayer(3f, 0f, 1f, Color.parseColor("#66FFFFFF"))
    }
    private val labelTextSizePx = 20f
    private val labelPaddingPx = 20f

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
    var onZoomStateChanged: ((isZoomed: Boolean) -> Unit)? = null


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
        onZoomStateChanged?.invoke(false) // Notifica que volvió al zoom mínimo
    }

    // ---- Foco de departamento seleccionado (independiente del pan/zoom del usuario) ----
    private var focusTranslateY = 0f
    private var focusAnimator: ValueAnimator? = null

    /**
     * Si el departamento seleccionado queda tapado por el popup, sube el mapa
     * lo MÍNIMO necesario para destaparlo (no lo centra ni lo mueve de más).
     * Si el departamento ya es visible por completo, no mueve nada.
     *
     * @param regionId departamento a revisar/destapar
     * @param reservedBottomPx alto en píxeles reales de pantalla que ocupa el popup (más su margen)
     * @param extraPadding aire extra entre el departamento y el borde del popup
     */
    fun focusRegionAboveBottom(regionId: String, reservedBottomPx: Float, extraPadding: Float = 24f) {
        val dr = drawableRegions.firstOrNull { it.region.id == regionId } ?: return

        // Punto más bajo del departamento, en su posición base (sin offset de foco)...
        val bottomPoint = floatArrayOf(dr.bounds.centerX(), dr.bounds.bottom)
        totalMatrix.mapPoints(bottomPoint)
        // ...más el offset YA vigente, para saber dónde está REALMENTE en pantalla ahora mismo.
        val regionBottomOnScreen = bottomPoint[1] + focusTranslateY

        val visibleLimit = height - reservedBottomPx - extraPadding
        val overlap = regionBottomOnScreen - visibleLimit

        // Si con el offset actual el departamento YA se ve bien, no tocar nada.
        if (overlap <= 0f) return

        // Si hace falta, sube SOLO lo adicional necesario desde donde ya está.
        val target = focusTranslateY - overlap
        animateFocusTo(target)
    }

    /** Revierte el offset de foco (el mapa vuelve a su posición normal). */
    fun clearRegionFocus() {
        animateFocusTo(0f)
    }

    private fun animateFocusTo(target: Float) {
        focusAnimator?.cancel()
        focusAnimator = ValueAnimator.ofFloat(focusTranslateY, target).apply {
            duration = 750 // más lento, para que el movimiento se sienta suave
            interpolator = android.view.animation.AccelerateDecelerateInterpolator() // acelera y frena de a poco, sin golpe seco
            addUpdateListener {
                focusTranslateY = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateBaseMatrix(w, h)
    }

    private val initialScaleFactor = 0.85f
    private val initialOffsetY = 120f

    private fun updateBaseMatrix(w: Int, h: Int) {
        if (viewportWidth <= 0 || viewportHeight <= 0 || w <= 0 || h <= 0) return
        val scale = minOf(w / viewportWidth, h / viewportHeight) * initialScaleFactor
        val dx = (w - viewportWidth * scale) / 2f
        val dy = (h - viewportHeight * scale) / 2f + initialOffsetY
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

    // 👈 REEMPLAZA TU scaleDetector CON ESTE
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val proposedZoom = (zoom * detector.scaleFactor).coerceIn(minZoom, maxZoom)
            val actualFactor = proposedZoom / zoom
            if (actualFactor != 1f) {
                userMatrix.postScale(actualFactor, actualFactor, detector.focusX, detector.focusY)
                zoom = proposedZoom
                recomputeTotalMatrix()
                invalidate()

                // Notifica si la pantalla tiene zoom (true) o si está en el zoom inicial (false)
                onZoomStateChanged?.invoke(zoom > minZoom)
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
            val pts = floatArrayOf(e.x, e.y - focusTranslateY) // resta el offset de foco antes de mapear
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
        canvas.translate(0f, focusTranslateY) // desplaza todo el dibujo por el foco animado
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
        }

        // 👇 El borde de selección se dibuja AL FINAL, encima de TODOS los departamentos.
        // Si se dibujara dentro del forEach de arriba, los vecinos dibujados después
        // taparían parte del borde azul en los lados que comparten contigo (por eso
        // antes se veían "lados sin azul": los internos, colindantes con otro depto).
        drawableRegions.firstOrNull { it.region.id == selectedRegionId }?.let { selectedDr ->
            drawSelectionGlow(canvas, selectedDr.path, currentScale())
        }

        canvas.restore()

        drawLabels(canvas)
    }

    /**
     * Marca visualmente el departamento seleccionado con:
     * 1. Un relleno semitransparente de color de acento (se nota sin importar si el
     *    departamento está "visitado" en verde o "pendiente" en blanco).
     * 2. Un borde sólido y limpio del mismo color, de grosor CONSTANTE en pantalla
     *    (se divide entre `scale` porque este dibujo ocurre dentro del canvas ya
     *    transformado por zoom/pan — sin esto, el borde se ve grueso al hacer zoom).
     */
    private fun drawSelectionGlow(canvas: Canvas, path: Path, scale: Float) {
        canvas.drawPath(path, selectedFillPaint)

        selectedGlowPaint.strokeWidth = 3.5f / scale
        canvas.drawPath(path, selectedGlowPaint)
    }

    /**
     * Dibuja las etiquetas SOLO cuando el usuario ha hecho zoom (zoom > minZoom) y,
     * además, el departamento ya es lo bastante grande en pantalla para que el texto
     * calce sin desbordarse. Al iniciar (zoom mínimo, mapa completo) no se muestra
     * ningún nombre — así el mapa se ve limpio y los nombres aparecen progresivamente
     * a medida que el usuario explora.
     */
    private fun drawLabels(canvas: Canvas) {
        if (zoom <= minZoom) return // 👈 nada de texto hasta que el usuario haga zoom

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
                center[1] += focusTranslateY // aplica el mismo offset de foco a las etiquetas
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