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
import com.app.tmarita.R
import com.app.tmarita.databinding.FragmentMapBinding
import com.app.tmarita.viewmodel.PeruMapViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PeruMapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PeruMapViewModel by viewModels()

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

        val frases = resources.getStringArray(R.array.welcome_quotes)
        binding.tvSubtitle.text = frases.random()

        binding.tvSubtitle.alpha = 0f
        binding.tvSubtitle.translationY = 35f

        binding.tvSubtitle.postDelayed({

            binding.tvSubtitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(1400)
                .start()

        }, 900)

        binding.peruMapView.onRegionClick = { region ->
            viewModel.onRegionTapped(region)
        }

        observeUiState()
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

        binding.progressBar.max = state.totalCount
        binding.progressBar.progress = state.visitedCount

        binding.progressText.text =
            "Vamos ${state.visitedCount} de ${state.totalCount} departamentos (${state.progressPercent}%) 🇵🇪"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}