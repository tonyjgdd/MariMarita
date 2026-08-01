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

        val fade = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val zoom = AnimationUtils.loadAnimation(this, R.anim.zoom)

        binding.ivLogo.startAnimation(zoom)

        binding.tvTitle.alpha = 0f
        binding.tvSubtitle.alpha = 0f

        binding.tvTitle.animate()
            .alpha(1f)
            .setDuration(700)
            .setStartDelay(300)
            .start()

        binding.tvSubtitle.animate()
            .alpha(1f)
            .setDuration(700)
            .setStartDelay(700)
            .start()



        Handler(Looper.getMainLooper()).postDelayed({

            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()

        }, 3500)
    }
}