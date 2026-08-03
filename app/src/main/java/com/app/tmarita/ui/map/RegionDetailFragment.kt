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
import com.app.tmarita.databinding.FragmentRegionDetailBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class RegionDetailFragment : Fragment() {

    private var _binding: FragmentRegionDetailBinding? = null
    private val binding get() = _binding!!

    private val args: RegionDetailFragmentArgs by navArgs()
    private val viewModel: RegionDetailViewModel by viewModels()

    private val dateFormatter = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "PE"))
    private var selectedDateMillis: Long? = null
    private var initialized = false

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
        binding.etFecha.setOnClickListener { showDatePicker() }

        binding.btnAbrirDrive.setOnClickListener {
            val link = binding.etDriveLink.text?.toString().orEmpty()
            if (link.isNotBlank()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            } else {
                Snackbar.make(binding.root, "Aún no hay un enlace guardado", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.btnGuardar.setOnClickListener {
            viewModel.save(
                visited = binding.switchVisitado.isChecked,
                place = binding.etLugar.text?.toString(),
                driveLink = binding.etDriveLink.text?.toString(),
                notes = binding.etNotas.text?.toString(),
                visitDateMillis = selectedDateMillis
            )
            Snackbar.make(binding.root, "Guardado 💛", Snackbar.LENGTH_SHORT).show()
        }

        observeState()
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Fecha del viaje")
            .setSelection(selectedDateMillis ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            selectedDateMillis = millis
            binding.etFecha.setText(dateFormatter.format(millis))
        }
        picker.show(parentFragmentManager, "date_picker")
    }

    /** Solo precarga los campos UNA vez al entrar; después el usuario manda hasta tocar Guardar. */
    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (!initialized) {
                        binding.switchVisitado.isChecked = state.visited
                        binding.etLugar.setText(state.place)
                        binding.etDriveLink.setText(state.driveLink)
                        binding.etNotas.setText(state.notes)
                        selectedDateMillis = state.visitDateMillis
                        binding.etFecha.setText(
                            state.visitDateMillis?.let { dateFormatter.format(it) } ?: ""
                        )
                        initialized = true
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