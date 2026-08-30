package com.app.tmarita

import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.app.tmarita.databinding.ActivitySplashBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    private var fotosDecodificadas: List<BitmapDrawable> = emptyList()

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

        // 👇 CLAVE: decode y delay corren EN PARALELO (async + delay), no uno
        // tras otro. El ciclo arranca apenas ambos terminan, no la suma de los dos.
        lifecycleScope.launch {
            val decodeDeferred = async(Dispatchers.Default) { decodificarFotos() }
            delay(750)
            fotosDecodificadas = decodeDeferred.await()
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

    /**
     * Decodifica las 12 fotos EN PARALELO (una corrutina por imagen en
     * Dispatchers.IO), en vez de una por una en secuencia. Con sampling,
     * esto normalmente termina bastante antes que el delay(750) de arriba.
     */
    private suspend fun decodificarFotos(): List<BitmapDrawable> = coroutineScope {
        val targetWidth = resources.displayMetrics.widthPixels
        fotosIds.shuffled().map { resId ->
            async(Dispatchers.IO) {
                val bitmap = decodeSampledBitmap(resources, resId, targetWidth)
                BitmapDrawable(resources, bitmap)
            }
        }.awaitAll()
    }

    private fun decodeSampledBitmap(res: Resources, resId: Int, reqWidth: Int): Bitmap {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(res, resId, options)

        var inSampleSize = 1
        if (options.outWidth > reqWidth) {
            val halfWidth = options.outWidth / 2
            while (halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        val finalOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
        return BitmapFactory.decodeResource(res, resId, finalOptions)
    }

    private fun iniciarCicloDeFotos() {
        if (fotosDecodificadas.isEmpty()) return

        var orden = fotosDecodificadas.toMutableList()
        var indice = 0

        binding.ivPhotoA.setImageDrawable(orden[indice])
        binding.ivPhotoA.animate().alpha(1f).setDuration(600).start()

        lifecycleScope.launch {
            var mostrandoA = true
            while (!isFinishing && !isDestroyed) {
                delay(intervaloCambioMs)

                indice = (indice + 1) % orden.size
                if (indice == 0) orden = orden.shuffled().toMutableList()

                val entrante: ImageView = if (mostrandoA) binding.ivPhotoB else binding.ivPhotoA
                val saliente: ImageView = if (mostrandoA) binding.ivPhotoA else binding.ivPhotoB

                entrante.animate().cancel()
                saliente.animate().cancel()

                entrante.setImageDrawable(orden[indice])
                entrante.alpha = 0f
                entrante.animate().alpha(1f).setDuration(fadeDurationMs).start()
                saliente.animate().alpha(0f).setDuration(fadeDurationMs).start()

                mostrandoA = !mostrandoA
            }
        }
    }
}