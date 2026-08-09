package com.app.tmarita.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PeruMapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PeruMapViewModel by viewModels()

    private lateinit var frases: Array<String>
    private var lastPhraseIndex = -1

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

        // 1. Al tocar un departamento: selecciona la región y oculta el Card de Progreso
        binding.peruMapView.onRegionClick = { region ->
            hideCardProgress()
            viewModel.onRegionTapped(region)
        }

        binding.peruMapView.onEmptyAreaClick = {
            viewModel.clearSelection()
        }

        // 2. 👈 ESCUCHA LOS CAMBIOS DE ZOOM
        binding.peruMapView.onZoomStateChanged = { isZoomed ->
            if (isZoomed) {
                hideCardProgress()
            } else {
                showCardProgress()
            }
        }

        // 3. 👈 CLIC EN LA BRÚJULA: Centra el mapa y muestra el Card de Progreso
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

//        binding.progressText.text = "${state.visitedCount} de ${state.totalCount} · ${state.progressPercent}%"
        binding.progressText.text = "Vamos ${state.visitedCount} de ${state.totalCount} departamentos"
        renderSelection(state)
    }

    private fun renderSelection(state: PeruMapUiState) {
        val region = state.selectedRegion
        if (region == null) {
            binding.regionInfoCard.visibility = View.GONE
            return
        }
        binding.regionInfoCard.visibility = View.VISIBLE

        val isLima = state.isLimaGroup(region.id)
        binding.regionNameText.text = if (isLima) "Lima" else region.title
        binding.regionStatusText.text = when {
            isLima -> "Siempre visitado 💛"
            region.id in state.visitedIds -> "Visitado"
            else -> "Aún no visitado"
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



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}