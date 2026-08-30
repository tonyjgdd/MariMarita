package com.app.tmarita.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.app.AlertDialog
import com.app.tmarita.R
import com.app.tmarita.databinding.DialogInfoBinding

fun showConfirmDialog(
    context: Context,
    title: String,
    subtitle: String,
    confirmText: String = "Eliminar",
    onConfirm: () -> Unit
) {
    val binding = DialogInfoBinding.inflate(LayoutInflater.from(context))

    val dialog = AlertDialog.Builder(context)
        .setView(binding.root)
        .setCancelable(true)
        .create()

    dialog.window?.apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        requestFeature(Window.FEATURE_NO_TITLE)
    }

    binding.tvTitle.text = title
    binding.tvSubtitle.text = subtitle
    binding.btnAccion.text = confirmText

    binding.btnAccion.setOnClickListener {
        onConfirm()
        dialog.dismiss()
    }

    binding.btnCancelar.setOnClickListener {
        dialog.dismiss()
    }

    dialog.show()

    // Margen lateral, para que no ocupe todo el ancho de la pantalla
    dialog.window?.setLayout(
        (context.resources.displayMetrics.widthPixels * 0.86).toInt(),
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
}