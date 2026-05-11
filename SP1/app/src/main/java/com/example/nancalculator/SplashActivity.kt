package com.example.nancalculator

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import com.example.nancalculator.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.splashContent.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(LOGO_ANIMATION_DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()

        Handler(Looper.getMainLooper()).postDelayed({
            val options = ActivityOptionsCompat.makeCustomAnimation(
                this,
                android.R.anim.fade_in,
                android.R.anim.fade_out,
            )
            startActivity(Intent(this, MainActivity::class.java), options.toBundle())
            finish()
        }, SPLASH_DURATION_MS)
    }

    private companion object {
        const val LOGO_ANIMATION_DURATION_MS = 2_000L
        const val SPLASH_DURATION_MS = 2_000L
    }
}
