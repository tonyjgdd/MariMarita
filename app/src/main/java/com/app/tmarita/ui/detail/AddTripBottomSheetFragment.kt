package com.app.tmarita.ui.detail

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.setFragmentResult
import com.app.tmarita.R
import com.app.tmarita.databinding.BottomSheetAddTripBinding
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * BottomSheet para registrar un nuevo viaje: foto de portada, lugar, fecha,
 * enlace de Drive con las evidencias y notas opcionales.
 */
class AddTripBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddTripBinding? = null
    private val binding get() = _binding!!

    private val dateFormatter = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "PE"))
    private var selectedDateMillis: Long? = null
    private var savedPhotoPath: String? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { copyImageToInternalStorage(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddTripBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPhotoPicker()
        setupDatePicker()
        setupDriveLinkField()
        setupSaveButton()
    }

    private fun setupPhotoPicker() {
        binding.framePhotoPicker.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        binding.btnChangePhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun copyImageToInternalStorage(sourceUri: Uri) {
        val context = requireContext()
        val fileName = "trip_${System.currentTimeMillis()}.jpg"
        val destFile = File(context.filesDir, fileName)

        // 1. Decodifica el tamaño real de la imagen sin cargarla completa en memoria
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(sourceUri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        // 2. Calcula un factor de reducción (queremos máx ~1080px de ancho)
        val maxDimension = 1080
        var sampleSize = 1
        while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        // 3. Decodifica ya reducida
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = context.contentResolver.openInputStream(sourceUri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return

        // 4. Guarda comprimida como JPEG de buena calidad (85%)
        destFile.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
        }
        bitmap.recycle()

        savedPhotoPath = destFile.absolutePath

        binding.ivTripPhotoPreview.visibility = View.VISIBLE
        binding.emptyPhotoState.visibility = View.GONE
        binding.btnChangePhoto.visibility = View.VISIBLE
        Glide.with(this)
            .load(destFile)
            .centerCrop()
            .into(binding.ivTripPhotoPreview)
    }


    private fun setupDatePicker() {
        binding.etFecha.addTextChangedListener(object : android.text.TextWatcher {
            private var isEditing = false
            private var previousText = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                if (isEditing || s == null) return
                isEditing = true

                val digitsOnly = s.toString().filter { it.isDigit() }.take(8) // ddMMyyyy, máx 8 dígitos
                val formatted = buildString {
                    for (i in digitsOnly.indices) {
                        append(digitsOnly[i])
                        if (i == 1 || i == 3) append('/')
                    }
                }

                if (formatted != previousText) {
                    previousText = formatted
                    binding.etFecha.setText(formatted)
                    binding.etFecha.setSelection(formatted.length)
                }

                // Si ya se completaron los 8 dígitos, intenta parsear la fecha
                selectedDateMillis = if (digitsOnly.length == 8) {
                    parseTypedDate(digitsOnly)
                } else {
                    null
                }

                isEditing = false
            }
        })

        binding.etFecha.setOnFocusChangeListener { _, hasFocus ->
            binding.etFecha.hint = if (hasFocus) "DD/MM/AAAA" else null
        }

    }

    /**
     * Convierte "ddMMyyyy" (8 dígitos) a millis, o null si la fecha no es válida
     * (ej: 31/02/2026 no existe).
     */
    private fun parseTypedDate(digitsOnly: String): Long? {
        val day = digitsOnly.substring(0, 2).toIntOrNull() ?: return null
        val month = digitsOnly.substring(2, 4).toIntOrNull() ?: return null
        val year = digitsOnly.substring(4, 8).toIntOrNull() ?: return null

        val calendar = java.util.Calendar.getInstance().apply {
            isLenient = false // rechaza fechas imposibles en vez de "corregirlas" silenciosamente
            set(java.util.Calendar.YEAR, year)
            set(java.util.Calendar.MONTH, month - 1)
            set(java.util.Calendar.DAY_OF_MONTH, day)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        return try {
            calendar.timeInMillis // fuerza la validación (Calendar lanza excepción si no es lenient y la fecha es inválida)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun setupDriveLinkField() {
        // Limpia el error del campo apenas el usuario vuelve a escribir.
        binding.etDriveLink.addTextChangedListener {
            binding.tilDriveLink.error = null
        }
    }

    private fun setupSaveButton() {
        binding.btnGuardarViaje.setOnClickListener {
            val place = binding.etLugar.text?.toString()?.trim().orEmpty()
            val rawLink = binding.etDriveLink.text?.toString()?.trim().orEmpty()
            val normalizedLink = normalizeDriveLink(rawLink)

            val error = validate(place, rawLink, normalizedLink)
            if (error != null) {
                AlertDialogFragment.show(
                    childFragmentManager,
                    title = error.title,
                    subtitle = error.subtitle,
                    buttonText = "Aceptar",
                    iconRes = error.iconRes
                )
                return@setOnClickListener
            }

            setFragmentResult(
                REQUEST_KEY,
                bundleOf(
                    KEY_PLACE to place,
                    KEY_DATE to selectedDateMillis,
                    KEY_DRIVE to normalizedLink,
                    KEY_NOTES to binding.etNotas.text?.toString(),
                    KEY_PHOTO to savedPhotoPath
                )
            )
            dismiss()
        }
    }

    /**
     * Valida los campos obligatorios en orden y devuelve la información
     * del primer error encontrado, o null si todo es válido.
     */
    private fun validate(
        place: String,
        rawLink: String,
        normalizedLink: String?
    ): ValidationError? = when {
        savedPhotoPath == null -> ValidationError(
            title = "Y la foto linda?",
            subtitle = "Sube la mejor foto para la portada!",
            iconRes = R.drawable.ic_3 // 👈 usa un ícono nuevo, o reutiliza uno existente
        )

        place.isBlank() -> ValidationError(
            title = "Hermosa,te falto el lugar",
            subtitle = "Cuéntame el lugar o provincia que visitamos!",
            iconRes = R.drawable.ic_2
        )
        selectedDateMillis == null -> ValidationError(
            title = "Esta vacia o incompla la fecha, srta Mari",
            subtitle = "Dime la fecha del viaje!",
            iconRes = R.drawable.ic_1
        )
        rawLink.isBlank() -> ValidationError(
            title = "Que decias, ya está? \n y las evidencias linda?",
            subtitle = "Falta el enlace Drive con las fotos!",
            iconRes = R.drawable.ic_3
        )
        normalizedLink == null -> ValidationError(
            title = "Ese enlace esta incorrecto al parecer, verificalo",
            subtitle = "Tiene que ser un link de Drive",
            iconRes = R.drawable.ic_4
        )
        else -> null
    }

    /**
     * Valida que el link sea de Google Drive. Devuelve el link normalizado
     * (con https:// asegurado) si es válido, o null si no lo es.
     * Acepta que el usuario no haya puesto "https://" al inicio.
     */
    private fun normalizeDriveLink(rawLink: String): String? {
        if (rawLink.isBlank()) return null

        val withScheme = if (!rawLink.startsWith("http://") && !rawLink.startsWith("https://")) {
            "https://$rawLink"
        } else {
            rawLink
        }

        val host = Uri.parse(withScheme).host?.lowercase() ?: return null
        val isDriveHost = host == "drive.google.com" || host == "www.drive.google.com"

        return withScheme.takeIf { isDriveHost }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class ValidationError(
        val title: String,
        val subtitle: String,
        @androidx.annotation.DrawableRes val iconRes: Int
    )

    companion object {
        const val REQUEST_KEY = "add_trip_request"
        const val KEY_PLACE = "place"
        const val KEY_DATE = "date"
        const val KEY_DRIVE = "drive"
        const val KEY_NOTES = "notes"
        const val KEY_PHOTO = "photo"
        private const val DATE_PICKER_TAG = "date_picker"

        fun newInstance() = AddTripBottomSheetFragment()
    }
}