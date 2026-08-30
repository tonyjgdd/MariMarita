package com.app.tmarita.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.app.tmarita.R
import com.app.tmarita.databinding.DialogAlertBinding

/**
 * Diálogo genérico para mostrar mensajes de validación o información
 * al usuario, con ícono, título, subtítulo y botón de acción configurables.
 */
class AlertDialogFragment : DialogFragment() {

    private var _binding: DialogAlertBinding? = null
    private val binding get() = _binding!!

    private var title: String = ""
    private var subtitle: String = ""
    private var buttonText: String = ""
    @DrawableRes private var iconRes: Int = R.drawable.logo_sf

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.TransparentDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAlertBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        isCancelable = true
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setDimAmount(0.4f)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.85).toInt(), // 85% del ancho de pantalla
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivIcon.setImageResource(iconRes)
        binding.tvTitle.text = title
        binding.tvSubtitle.text = subtitle
        binding.btnAccion.text = buttonText
        binding.btnAccion.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "alert_dialog"

        fun show(
            fragmentManager: FragmentManager,
            title: String,
            subtitle: String = "",
            buttonText: String,
            @DrawableRes iconRes: Int = R.drawable.logo_sf
        ) {
            AlertDialogFragment().apply {
                this.title = title
                this.subtitle = subtitle
                this.buttonText = buttonText
                this.iconRes = iconRes
            }.show(fragmentManager, TAG)
        }
    }
}