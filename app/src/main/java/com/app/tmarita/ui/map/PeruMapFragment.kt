package com.app.tmarita.ui.map

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
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PeruMapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PeruMapViewModel by viewModels()

    private lateinit var frases: Array<String>
    private var lastPhraseIndex = -1

    // Guardamos el margen original del header (16dp del XML) para no perderlo al aplicar el inset
    private var headerBaseTopMargin = 0

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

    /**
     * Solo la parte de ARRIBA es edge-to-edge: el mapa se extiende detrás de la barra de
     * estado (hora, batería) y el header baja lo justo para no quedar tapado.
     *
     * Abajo NO hacemos edge-to-edge: le damos ese espacio como padding al contenedor raíz,
     * así el mapa y el bottomNavBar quedan automáticamente por ENCIMA de la barra de
     * navegación del sistema (botones o gestos), como en una app normal sin edge-to-edge ahí.
     */
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { rootView, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Header respeta la barra de estado (arriba)
            (binding.headerContainer.layoutParams as ViewGroup.MarginLayoutParams).topMargin =
                systemBars.top + headerBaseTopMargin
            binding.headerContainer.requestLayout()

            // El root reserva el espacio de abajo, empujando todo el contenido (mapa incluido)
            // hacia arriba de la barra de navegación del sistema
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

//        binding.progressText.text = "${state.visitedCount} de ${state.totalCount} · ${state.progressPercent}%"
        binding.progressText.text = "Vamos ${state.visitedCount} de ${state.totalCount} departamentos"
        renderSelection(state)
    }

    private fun renderSelection(state: PeruMapUiState) {
        val region = state.selectedRegion
        if (region == null) {
            binding.regionInfoCard.visibility = View.GONE
            showLocateButton()
            return
        }
        binding.regionInfoCard.visibility = View.VISIBLE
        hideLocateButton() // 👈 mismo lugar que el regionInfoCard: se ocultan mutuamente

        val isLima = state.isLimaGroup(region.id)
        binding.regionNameText.text = if (isLima) "Lima" else region.title
        binding.regionStatusText.text = when {
            isLima -> "Siempre visitado 💛"
            region.id in state.visitedIds -> "Visitado"
            else -> "Aún no visitado"
        }

        binding.regionBackgroundImage.setImageResource(getRegionBackgroundRes(region.id))
    }

    /**
     * Busca un drawable con el patrón "bg_region_<id>" (ej: bg_region_cusco, bg_region_puno).
     * Si no existe una imagen para ese departamento, usa una imagen genérica de respaldo
     * (bg_region_default) para que nunca se vea un espacio vacío o roto.
     *
     * Ventaja de este enfoque: solo necesitas AGREGAR el drawable con el nombre correcto
     * en res/drawable — no hay que tocar código cada vez que agregues una nueva foto.
     */
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



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}