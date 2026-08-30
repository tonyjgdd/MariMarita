package com.app.tmarita

import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.app.tmarita.databinding.ActivitySplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    private val fotosIds = listOf(
        R.drawable.foto1, R.drawable.foto2, R.drawable.foto3,
        R.drawable.foto4, R.drawable.foto5, R.drawable.foto6,
        R.drawable.foto7, R.drawable.foto8, R.drawable.foto9,
        R.drawable.foto10, R.drawable.foto11, R.drawable.foto12
    )

    private val intervaloCambioMs = 900L
    private val duracionSplashMs = 8000L
    private val fadeDurationMs = 450L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvSubtitle.text = resources.getStringArray(R.array.welcome).random()

        ejecutarAnimacionEntrada()

        lifecycleScope.launch {
            delay(duracionSplashMs)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    private fun ejecutarAnimacionEntrada() {
        binding.ivPanda.alpha = 0f
        binding.ivPanda.scaleX = 0.7f
        binding.ivPanda.scaleY = 0.7f
        binding.tvTitle.alpha = 0f
        binding.progressBar.translationY = 50f
        binding.tvLoading.translationY = 50f

        binding.ivPanda.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(700)
            .setInterpolator(DecelerateInterpolator())
            .start()

        binding.tvTitle.animate().alpha(1f).setStartDelay(300).setDuration(600).start()
        binding.vScrim.animate().alpha(1f).setStartDelay(450).setDuration(500).start()

        binding.ivQuote.animate().alpha(1f).setStartDelay(700).setDuration(600).start()
        binding.tvSubtitles.animate().alpha(1f).setStartDelay(700).setDuration(600).start()

        // Iniciamos el ciclo directamente al cumplir los 750 ms
        lifecycleScope.launch {
            delay(750)
            iniciarCicloDeFotos()
        }

        binding.progressBar.animate()
            .alpha(1f).translationY(0f)
            .setStartDelay(900).setDuration(500)
            .start()

        binding.tvLoading.animate()
            .alpha(1f).translationY(0f)
            .setStartDelay(900).setDuration(500)
            .start()
    }

    private fun iniciarCicloDeFotos() {
        var orden = fotosIds.shuffled().toMutableList()
        var indice = 0

        // Primera foto cargada directamente con Glide
        cargarFotoConGlide(orden[indice], binding.ivPhotoA)
        binding.ivPhotoA.animate().alpha(1f).setDuration(600).start()

        lifecycleScope.launch {
            var mostrandoA = true
            while (!isFinishing && !isDestroyed) {
                delay(intervaloCambioMs)

                indice = (indice + 1) % orden.size
                if (indice == 0) orden = fotosIds.shuffled().toMutableList()

                val entrante: ImageView = if (mostrandoA) binding.ivPhotoB else binding.ivPhotoA
                val saliente: ImageView = if (mostrandoA) binding.ivPhotoA else binding.ivPhotoB

                entrante.animate().cancel()
                saliente.animate().cancel()

                // Carga eficiente de la foto entrante con Glide
                cargarFotoConGlide(orden[indice], entrante)

                entrante.alpha = 0f
                entrante.animate().alpha(1f).setDuration(fadeDurationMs).start()
                saliente.animate().alpha(0f).setDuration(fadeDurationMs).start()

                mostrandoA = !mostrandoA
            }
        }
    }

    private fun cargarFotoConGlide(resId: Int, imageView: ImageView) {
        Glide.with(this)
            .load(resId)
            .centerCrop()
            .into(imageView)
    }
}