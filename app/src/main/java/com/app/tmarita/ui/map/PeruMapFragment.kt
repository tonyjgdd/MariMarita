package com.app.tmarita.ui.map

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.app.tmarita.R
import com.app.tmarita.databinding.FragmentMapBinding
import com.app.tmarita.viewmodel.PeruMapViewModel
import dagger.hilt.android.AndroidEntryPoint
import android.transition.TransitionManager
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.doOnEnd
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.view.animation.DecelerateInterpolator

@AndroidEntryPoint
class PeruMapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PeruMapViewModel by viewModels()

    private lateinit var frases: Array<String>
    private var lastPhraseIndex = -1

    // Guardamos el margen original del header (16dp del XML) para no perderlo al aplicar el inset
    private var headerBaseTopMargin = 0

    private var pandaAnimator: AnimatorSet? = null





    // Nuevo campo de la clase, junto a los otros:
    private var currentPopupRegionId: String? = null

    private val popupHeightDp = 400
    private val popupMarginBottomDp = 20


    private fun dpToPx(dp: Int): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics)

    private fun renderSelection(state: PeruMapUiState) {
        val region = state.selectedRegion

        if (region == null) {
            if (currentPopupRegionId != null) hideRegionPopup()
            currentPopupRegionId = null
            return
        }

        val isNewSelection = currentPopupRegionId != region.id
        currentPopupRegionId = region.id

        val isLima = state.isLimaGroup(region.id)
        binding.regionNameText.text = if (isLima) "Lima" else region.title
        binding.regionStatusText.text = when {
            isLima -> "Siempre visitado 💛"
            region.id in state.visitedIds -> "Visitado"
            else -> "Aún no visitado"
        }
        binding.regionBackgroundImage.setImageResource(getRegionBackgroundRes(region.id))

        if (isNewSelection) {
            showRegionPopup(region.id)
        }
    }

    private fun showRegionPopup(regionId: String) {
        hideLocateButton()
        binding.bottomNavBar.visibility = View.GONE

        val card = binding.regionInfoCard
        card.animate().cancel()
        card.visibility = View.VISIBLE
        card.alpha = 0f
        card.translationY = dpToPx(popupHeightDp) // arranca "escondido" abajo, como si estuviera tras la nav bar

        card.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(280)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Alto del popup (160dp fijo en XML) + su margen inferior (20dp)
        val reservedBottomPx = dpToPx(popupHeightDp) + dpToPx(popupMarginBottomDp)
        binding.peruMapView.focusRegionAboveBottom(regionId, reservedBottomPx)
    }

    private fun hideRegionPopup() {
        val card = binding.regionInfoCard
        card.animate().cancel()
        card.animate()
            .translationY(dpToPx(popupHeightDp))
            .alpha(0f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                card.visibility = View.GONE
                card.translationY = 0f
                card.alpha = 1f
            }
            .start()

        binding.bottomNavBar.visibility = View.VISIBLE
        showLocateButton()
        binding.peruMapView.clearRegionFocus()
    }










    private fun startPandaAnimation() {
        if (pandaAnimator?.isRunning == true) return

        val panda = binding.ivHeaderPanda

        // Traslación: se mueve al espacio libre de la derecha y vuelve
        val translate = ObjectAnimator.ofFloat(panda, "translationX", 0f, 25f, 0f).apply {
            duration = 3000
        }

        // Leve rotación para dar sensación de "paso"
        val rotate = ObjectAnimator.ofFloat(panda, "rotation", 0f, -6f, 6f, 0f).apply {
            duration = 1000
        }

        // Rebotecito vertical sutil, sincronizado
        val bounce = ObjectAnimator.ofFloat(panda, "translationY", 0f, -4f, 0f).apply {
            duration = 1000
        }

        pandaAnimator = AnimatorSet().apply {
            playTogether(translate, rotate, bounce)
            interpolator = AccelerateDecelerateInterpolator()
            doOnEnd {
                // Se reinicia solo mientras el fragment siga visible
                if (isAdded && view != null) startPandaAnimation()
            }
            start()
        }
    }

    private fun stopPandaAnimation() {
        pandaAnimator?.cancel()
        pandaAnimator = null
        binding.ivHeaderPanda.translationX = 0f
        binding.ivHeaderPanda.translationY = 0f
        binding.ivHeaderPanda.rotation = 0f
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        frases = resources.getStringArray(R.array.welcome_quotes)


        // Guardamos el margen que ya viene del XML (16dp)
        headerBaseTopMargin =
            (binding.headerContainer.layoutParams as ViewGroup.MarginLayoutParams).topMargin

        setupWindowInsets()

        // 👈 Bucle para cambiar el texto de tvProgressLabel cada 5 segundos
        startProgressLabelLoop()

        // 1. Al tocar un departamento: selecciona la región y oculta el Card de Progreso
        binding.peruMapView.onRegionClick = { region ->
            hideCardProgress()
            viewModel.onRegionTapped(region)
        }

        binding.peruMapView.onEmptyAreaClick = {
            viewModel.clearSelection()
        }

        // 2. ESCUCHA LOS CAMBIOS DE ZOOM
        binding.peruMapView.onZoomStateChanged = { isZoomed ->
            if (isZoomed) {
                hideCardProgress()
            } else {
                showCardProgress()
            }
        }

        // 3. CLIC EN LA BRÚJULA: Centra el mapa y muestra el Card de Progreso
        binding.btnLocateMe.setOnClickListener {
            binding.peruMapView.resetZoom()
            showCardProgress()
        }

        binding.btnOpenRegion.setOnClickListener {
            val region = viewModel.uiState.value.selectedRegion ?: return@setOnClickListener
            val state = viewModel.uiState.value
            val displayTitle = if (state.isLimaGroup(region.id)) "Lima" else region.title
            findNavController().navigate(
                PeruMapFragmentDirections.actionMapToRegionDetail(region.id, displayTitle)
            )
        }

        observeUiState()
    }

    /**
     * Bucle que actualiza tvProgressLabel cada 5 segundos usando las frases de R.array.welcome_quotes
     * con una animación suave de opacidad.
     */
    private fun startProgressLabelLoop() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    val phrase = nextPhrase()
                    if (phrase.isNotEmpty()) {
                        binding.tvProgressLabel.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction {
                                binding.tvProgressLabel.text = "$phrase ️🩷"
                                binding.tvProgressLabel.animate()
                                    .alpha(1f)
                                    .setDuration(300)
                                    .start()
                            }
                            .start()
                    }
                    delay(5000L)
                }
            }
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { rootView, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            (binding.headerContainer.layoutParams as ViewGroup.MarginLayoutParams).topMargin =
                systemBars.top + headerBaseTopMargin
            binding.headerContainer.requestLayout()

            rootView.setPadding(
                rootView.paddingLeft,
                rootView.paddingTop,
                rootView.paddingRight,
                systemBars.bottom
            )

            insets
        }
    }

    /** Elige una frase al azar sin repetir la anterior (si hay más de una). */
    private fun nextPhrase(): String {
        if (frases.isEmpty()) return ""
        if (frases.size == 1) return frases[0]
        var index: Int
        do {
            index = frases.indices.random()
        } while (index == lastPhraseIndex)
        lastPhraseIndex = index
        return frases[index]
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: PeruMapUiState) {

        if (state.regions.isNotEmpty()) {
            binding.peruMapView.setRegions(
                state.viewportWidth,
                state.viewportHeight,
                state.regions
            )
        }

        binding.peruMapView.visitedIds = state.visitedIds
        binding.peruMapView.selectedRegionId = state.selectedRegion?.id

        binding.progressBar.max = state.totalCount
        binding.progressBar.progress = state.visitedCount

        binding.progressText.text = "Vamos ${state.visitedCount} de ${state.totalCount} departamentos"
        renderSelection(state)
    }

    private fun getRegionBackgroundRes(regionId: String): Int {
        val resId = resources.getIdentifier(
            "bg_region_${regionId.lowercase()}",
            "drawable",
            requireContext().packageName
        )
        return if (resId != 0) resId else R.drawable.foto5
    }

    private fun hideLocateButton() {
        if (binding.btnLocateMe.visibility == View.VISIBLE) {
            TransitionManager.beginDelayedTransition(binding.btnLocateMe.parent as ViewGroup)
            binding.btnLocateMe.visibility = View.GONE
        }
    }

    private fun showLocateButton() {
        if (binding.btnLocateMe.visibility != View.VISIBLE) {
            TransitionManager.beginDelayedTransition(binding.btnLocateMe.parent as ViewGroup)
            binding.btnLocateMe.visibility = View.VISIBLE
        }
    }

    private fun hideCardProgress() {
        if (binding.cardProgress.visibility == View.VISIBLE) {
            TransitionManager.beginDelayedTransition(binding.cardProgress.parent as ViewGroup)
            binding.cardProgress.visibility = View.GONE
        }
    }

    private fun showCardProgress() {
        if (binding.cardProgress.visibility != View.VISIBLE) {
            TransitionManager.beginDelayedTransition(binding.cardProgress.parent as ViewGroup)
            binding.cardProgress.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        startPandaAnimation()
    }

    override fun onPause() {
        super.onPause()
        stopPandaAnimation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPandaAnimation()
        _binding = null
    }
}