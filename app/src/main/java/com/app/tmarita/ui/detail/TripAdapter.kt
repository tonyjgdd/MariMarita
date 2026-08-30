package com.app.tmarita.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.tmarita.R
import com.app.tmarita.databinding.ItemTripBinding
import com.app.tmarita.model.Trip
import com.app.tmarita.util.showConfirmDialog
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class TripAdapter(
    private val onOpenDrive: (Trip) -> Unit,
    private val onDelete: (Trip) -> Unit
) : ListAdapter<Trip, TripAdapter.TripViewHolder>(DIFF) {

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale("es", "PE"))

    inner class TripViewHolder(val binding: ItemTripBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemTripBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TripViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = getItem(position)
        with(holder.binding) {
            tvTripPlace.text = trip.place?.takeIf { it.isNotBlank() } ?: "Sin lugar especificado"
            tvTripDate.text = trip.visitDateMillis?.let { dateFormatter.format(it) } ?: "Sin fecha"
            tvTripNotes.text = trip.notes
            tvTripNotes.visibility = if (trip.notes.isNullOrBlank()) View.GONE else View.VISIBLE

            // Foto de portada por viaje
            if (!trip.photoPath.isNullOrBlank()) {
                Glide.with(ivTripPhoto)
                    .load(File(trip.photoPath))
                    .override(500, 340) // 👈 tamaño aproximado de la card en px, ajusta si quieres
                    .placeholder(R.drawable.ic_no_disponible)
                    .centerCrop()
                    .into(ivTripPhoto)
            } else {
                Glide.with(ivTripPhoto).clear(ivTripPhoto)
                ivTripPhoto.setImageResource(R.drawable.foto5)
            }

            val hasDrive = !trip.driveLink.isNullOrBlank()
            btnTripDrive.visibility = if (hasDrive) View.VISIBLE else View.GONE
            btnTripDrive.setOnClickListener { onOpenDrive(trip) }

            btnTripDelete.setOnClickListener {
                showConfirmDialog(
                    context = root.context,
                    title = "¿Eliminar viaje?",
                    subtitle = "Esta acción no se puede deshacer.",
                    confirmText = "Eliminar",
                    onConfirm = { onDelete(trip) }
                )
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Trip>() {
            override fun areItemsTheSame(old: Trip, new: Trip) = old.id == new.id
            override fun areContentsTheSame(old: Trip, new: Trip) = old == new
        }
    }
}