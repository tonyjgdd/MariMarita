package com.app.tmarita

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.app.tmarita.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val frases = resources.getStringArray(R.array.frases_splash)
        binding.tvQuote.text = frases.random()

        binding.ivTitle.alpha = 0f
        binding.tvQuote.alpha = 0f

        animateCollage()

        binding.ivTitle.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(200)
            .start()

        binding.tvQuote.animate()
            .alpha(1f)
            .setDuration(600)
            .setStartDelay(1300)
            .start()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 4400)
    }

    private fun animateCollage() {
        // Slots = las posiciones fijas del layout (tamaño/lugar no cambian)
        val slots = listOf(
            binding.foto1, binding.foto2, binding.foto3, binding.foto4,
            binding.foto5, binding.foto6, binding.foto7, binding.foto8
        )

        // 1. Reasignar QUÉ foto va en cada slot, al azar en cada apertura
        val drawablesRandom = listOf(
            R.drawable.foto1, R.drawable.foto2, R.drawable.foto3, R.drawable.foto4,
            R.drawable.foto5, R.drawable.foto6, R.drawable.foto7, R.drawable.foto8
        ).shuffled()

        slots.forEachIndexed { i, iv -> iv.setImageResource(drawablesRandom[i]) }

        // 2. Animar cada slot entrando desde un borde aleatorio
        val screenW = resources.displayMetrics.widthPixels.toFloat()
        val screenH = resources.displayMetrics.heightPixels.toFloat()

        slots.shuffled().forEachIndexed { index, foto ->
            val direccion = (0..3).random() // 0=izq 1=der 2=arriba 3=abajo
            val offsetX = when (direccion) { 0 -> -screenW * 0.4f; 1 -> screenW * 0.4f; else -> 0f }
            val offsetY = when (direccion) { 2 -> -screenH * 0.3f; 3 -> screenH * 0.3f; else -> 0f }
            val rotacion = listOf(-14f, -9f, 9f, 14f).random()

            foto.alpha = 0f
            foto.translationX = offsetX
            foto.translationY = offsetY
            foto.rotation = rotacion
            foto.scaleX = 0.85f
            foto.scaleY = 0.85f

            foto.animate()
                .alpha(1f)
                .translationX(0f)
                .translationY(0f)
                .rotation(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(550)
                .setStartDelay(index * 110L)
                .setInterpolator(DecelerateInterpolator(1.4f))
                .start()
        }
    }
}