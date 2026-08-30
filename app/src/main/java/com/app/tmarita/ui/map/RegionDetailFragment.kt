package com.app.tmarita.ui.detail

import android.content.Intent
import android.net.Uri
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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.tmarita.databinding.FragmentRegionDetailBinding
import com.app.tmarita.ui.detail.TripAdapter
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegionDetailFragment : Fragment() {

    private var _binding: FragmentRegionDetailBinding? = null
    private val binding get() = _binding!!

    private val args: RegionDetailFragmentArgs by navArgs()
    private val viewModel: RegionDetailViewModel by viewModels()

    private lateinit var adapter: TripAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvRegionTitle.text = args.regionTitle
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        adapter = TripAdapter(
            onOpenDrive = { trip ->
                val link = trip.driveLink
                if (!link.isNullOrBlank()) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                }
            },
            onDelete = { trip ->
                viewModel.deleteTrip(trip.id)
                SuccessDialogFragment.show(parentFragmentManager, "Viaje eliminado 😞")
            }
        )
        binding.rvTrips.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTrips.adapter = adapter

        binding.fabAddTrip.setOnClickListener {
            AddTripBottomSheetFragment.newInstance()
                .show(childFragmentManager, "add_trip")
        }

        childFragmentManager.setFragmentResultListener(
            AddTripBottomSheetFragment.REQUEST_KEY, viewLifecycleOwner
        ) { _, bundle ->
            viewModel.addTrip(
                place = bundle.getString(AddTripBottomSheetFragment.KEY_PLACE),
                visitDateMillis = bundle.getLong(AddTripBottomSheetFragment.KEY_DATE, -1L)
                    .takeIf { it != -1L },
                driveLink = bundle.getString(AddTripBottomSheetFragment.KEY_DRIVE),
                notes = bundle.getString(AddTripBottomSheetFragment.KEY_NOTES),
                photoPath = bundle.getString(AddTripBottomSheetFragment.KEY_PHOTO)   // 👈 nuevo
            )
            SuccessDialogFragment.show(parentFragmentManager, "¡Viaje guardado! 🩷")
        }

        observeTrips()
    }

    private fun observeTrips() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.trips.collect { trips ->
                    adapter.submitList(trips)
                    binding.emptyState.visibility = if (trips.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvTrips.visibility = if (trips.isEmpty()) View.GONE else View.VISIBLE

                    // 👇 nuevo: actualiza el contador dinámicamente
                    binding.tvTripCount.text = when (trips.size) {
                        0 -> "Sin viajes registrados"
                        1 -> "1 viaje registrado"
                        else -> "${trips.size} viajes registrados"
                    }
                }
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}