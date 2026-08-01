package com.app.tmarita

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.app.tmarita.databinding.ActivitySplashBinding
import android.view.animation.AnimationUtils


class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val frases = resources.getStringArray(R.array.welcome_quotes)
        binding.tvFrase.text = frases.random()

        // Cargar animaciones
        val fade = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val zoom = AnimationUtils.loadAnimation(this, R.anim.zoom)

        // Aplicarlas
        binding.ivLogo.startAnimation(zoom)
        binding.tvFrase.startAnimation(fade)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 3500)
    }
}