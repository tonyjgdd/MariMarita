package com.app.tmarita

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat // <-- Asegúrate de importar esto
import androidx.lifecycle.lifecycleScope
import com.app.tmarita.databinding.ActivitySplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    private val fotos = listOf(
        R.drawable.foto1, R.drawable.foto2, R.drawable.foto3,
        R.drawable.foto4, R.drawable.foto5, R.drawable.foto6,
        R.drawable.foto7, R.drawable.foto8, R.drawable.foto9
    )

    private val intervaloCambioMs = 1100L
    private val duracionSplashMs = 4200L
    private val fadeDurationMs = 500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // IMPORTANTE: Hace que la app ocupe toda la pantalla incluyendo la barra superior
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvSubtitle.text = resources.getStringArray(R.array.frases_splash).random()

        iniciarCicloDeFotos()

        lifecycleScope.launch {
            delay(duracionSplashMs)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    private fun iniciarCicloDeFotos() {
        val orden = fotos.shuffled().toMutableList()
        var indice = 0

        binding.ivPhotoA.setImageResource(orden[indice])

        lifecycleScope.launch {
            var mostrandoA = true
            while (!isFinishing && !isDestroyed) {
                delay(intervaloCambioMs)

                indice = (indice + 1) % orden.size
                if (indice == 0) orden.shuffle()

                val entrante: ImageView = if (mostrandoA) binding.ivPhotoB else binding.ivPhotoA
                val saliente: ImageView = if (mostrandoA) binding.ivPhotoA else binding.ivPhotoB

                entrante.setImageResource(orden[indice])
                entrante.alpha = 0f
                entrante.animate().alpha(1f).setDuration(fadeDurationMs).start()
                saliente.animate().alpha(0f).setDuration(fadeDurationMs).start()

                mostrandoA = !mostrandoA
            }
        }
    }
}