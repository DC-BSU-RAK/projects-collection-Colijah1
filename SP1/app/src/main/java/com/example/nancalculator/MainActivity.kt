package com.example.nancalculator

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.nancalculator.databinding.ActivityMainBinding
import com.example.nancalculator.databinding.LayoutInstructionsSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val decimalFormat = DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US))
    private val handler = Handler(Looper.getMainLooper())
    private var progressAnimator: ValueAnimator? = null
    private var pulseAnimator: AnimatorSet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupMotion()
        setupInputs()
        updateRatioLabel(DEFAULT_RATIO)
    }

    private fun setupMotion() {
        binding.content.scheduleLayoutAnimation()
    }

    private fun setupInputs() = with(binding) {
        setupDropdowns()
        ratioSlider.value = DEFAULT_RATIO.toFloat()

        ratioSlider.addOnChangeListener { _, value, _ ->
            updateRatioLabel(value.toDouble())
            hideDiscovery()
        }

        brewButton.setOnClickListener {
            triggerHeavyHaptic()
            brewAndDiscover()
        }

        infoButton.setOnClickListener {
            showInstructionsSheet()
        }
    }

    private fun showInstructionsSheet() {
        val sheetBinding = LayoutInstructionsSheetBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(sheetBinding.root)
        dialog.setOnShowListener {
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.background = ColorDrawable(Color.TRANSPARENT)
        }
        dialog.show()
    }

    private fun setupDropdowns() = with(binding) {
        val baseOptions = resources.getStringArray(R.array.base_liquids)
        val mixerOptions = resources.getStringArray(R.array.mixers)
        val extraOptions = resources.getStringArray(R.array.extras)

        baseLiquidDropdown.setAdapter(createDropdownAdapter(baseOptions))
        mixerDropdown.setAdapter(createDropdownAdapter(mixerOptions))
        extraDropdown.setAdapter(createDropdownAdapter(extraOptions))

        baseLiquidDropdown.setText(baseOptions.first(), false)
        mixerDropdown.setText(mixerOptions.first(), false)
        extraDropdown.setText(extraOptions.first(), false)

        baseLiquidDropdown.setOnItemClickListener { _, _, _, _ ->
            triggerLightTick()
            hideDiscovery()
        }
        mixerDropdown.setOnItemClickListener { _, _, _, _ -> hideDiscovery() }
        extraDropdown.setOnItemClickListener { _, _, _, _ -> hideDiscovery() }
    }

    private fun createDropdownAdapter(options: Array<String>): ArrayAdapter<String> {
        return ArrayAdapter(this, android.R.layout.simple_list_item_1, options)
    }

    private fun brewAndDiscover() {
        hideDiscovery(immediate = true)
        binding.brewButton.isEnabled = false
        binding.brewButton.text = getString(R.string.synthesizing_button)
        binding.synthProgress.visibility = View.VISIBLE
        binding.synthProgress.progress = 0

        handler.removeCallbacksAndMessages(null)
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofInt(0, 100).apply {
            duration = BREW_ANIMATION_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                binding.synthProgress.progress = animator.animatedValue as Int
            }
            doOnEnd {
                binding.brewButton.isEnabled = true
                binding.brewButton.text = getString(R.string.brew_button)
                binding.synthProgress.visibility = View.GONE
                binding.synthProgress.progress = 0
                revealDiscovery(createAlchemyResult())
            }
            start()
        }
    }

    private fun revealDiscovery(result: AlchemyResult) {
        pulseAnimator?.cancel()
        applyAuraGradient(result.auraModifier)
        binding.coffeeName.text = result.name
        binding.flavorProfile.text = result.description
        binding.ingredientSummary.text = getString(R.string.synthesized_label, result.summary)
        binding.discoveryCard.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        binding.discoveryCard.visibility = View.VISIBLE
        binding.discoveryCard.alpha = 0f
        binding.discoveryCard.translationY = DISCOVERY_OFFSET_Y
        binding.discoveryCard.scaleX = 1f
        binding.discoveryCard.scaleY = 1f
        binding.coffeeName.alpha = 0f
        binding.shimmerView.alpha = 0f

        binding.discoveryCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(DISCOVERY_ANIMATION_DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                binding.coffeeName.animate()
                    .alpha(1f)
                    .setDuration(NAME_FADE_DURATION_MS)
                    .start()
                runShimmer()
                startResultPulse()
            }
            .start()
    }

    private fun applyAuraGradient(auraModifier: String) {
        val colors = when (auraModifier) {
            "Lavender" -> intArrayOf(
                Color.parseColor("#4F2D6F"),
                Color.parseColor("#25172F"),
                Color.parseColor("#D4AF37"),
            )

            "Sea Salt" -> intArrayOf(
                Color.parseColor("#063A5F"),
                Color.parseColor("#111C38"),
                Color.parseColor("#D4AF37"),
            )

            "Cocoa" -> intArrayOf(
                Color.parseColor("#3A2118"),
                Color.parseColor("#1A1110"),
                Color.parseColor("#B78945"),
            )

            "Honey" -> intArrayOf(
                Color.parseColor("#704A10"),
                Color.parseColor("#21170D"),
                Color.parseColor("#D4AF37"),
            )

            else -> intArrayOf(
                Color.parseColor("#1E1E1E"),
                Color.parseColor("#262626"),
                Color.parseColor("#D4AF37"),
            )
        }

        binding.discoverySurface.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            colors,
        ).apply {
            cornerRadius = RESULT_CARD_RADIUS_DP * resources.displayMetrics.density
            setStroke(
                (1 * resources.displayMetrics.density).toInt(),
                Color.parseColor("#D4AF37"),
            )
        }
    }

    private fun startResultPulse() {
        val scaleX = ObjectAnimator.ofFloat(binding.discoveryCard, View.SCALE_X, 1f, RESULT_PULSE_SCALE)
        val scaleY = ObjectAnimator.ofFloat(binding.discoveryCard, View.SCALE_Y, 1f, RESULT_PULSE_SCALE)

        pulseAnimator = AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = RESULT_PULSE_DURATION_MS
            interpolator = LinearInterpolator()
        }

        listOf(scaleX, scaleY).forEach { animator ->
            animator.repeatMode = ValueAnimator.REVERSE
            animator.repeatCount = ValueAnimator.INFINITE
        }

        pulseAnimator?.start()
    }

    private fun runShimmer() {
        binding.discoveryCard.post {
            val travel = binding.discoveryCard.width.toFloat() + binding.shimmerView.width
            binding.shimmerView.translationX = -binding.shimmerView.width.toFloat()
            binding.shimmerView.alpha = SHIMMER_ALPHA
            binding.shimmerView.animate()
                .translationX(travel)
                .setDuration(SHIMMER_DURATION_MS)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    binding.shimmerView.alpha = 0f
                }
                .start()
        }
    }

    private fun hideDiscovery(immediate: Boolean = false) {
        handler.removeCallbacksAndMessages(null)
        progressAnimator?.cancel()
        pulseAnimator?.cancel()
        binding.brewButton.isEnabled = true
        binding.brewButton.text = getString(R.string.brew_button)
        if (!immediate) binding.synthProgress.visibility = View.GONE
        binding.synthProgress.progress = 0
        binding.discoveryCard.animate().cancel()
        binding.coffeeName.animate().cancel()
        binding.shimmerView.animate().cancel()
        binding.discoveryCard.visibility = View.GONE
        binding.discoveryCard.scaleX = 1f
        binding.discoveryCard.scaleY = 1f
        binding.coffeeName.alpha = 0f
        binding.shimmerView.alpha = 0f
    }

    private fun createAlchemyResult(): AlchemyResult {
        val base = binding.baseLiquidDropdown.text.toString()
        val mixer = binding.mixerDropdown.text.toString()
        val extra = binding.extraDropdown.text.toString()
        val ratio = binding.ratioSlider.value.toInt()
        val summary = "1 Part $base, $ratio Parts $mixer, 1 Hint of $extra."

        return when {
            base == "Espresso" && mixer == "Tonic Water" && extra == "Sea Salt" ->
                AlchemyResult("The Golden Catalyst", "Sparkling, saline, and electric with a bright espresso core.", summary, extra)

            base == "Matcha" && mixer == "Oat Milk" && extra == "Lavender" ->
                AlchemyResult("Lunar Aura", "Silky, floral, and serene with a soft green-tea glow.", summary, extra)

            base == "Filter Coffee" && mixer == "Steamed Milk" && extra == "Cocoa" ->
                AlchemyResult("Velvet Obsidian", "Dark, plush, and cocoa-laced with a mellow coffee finish.", summary, extra)

            else ->
                AlchemyResult(
                    "The Unknown Extract",
                    "A mysterious compound with unstable aromatics. The engine cannot classify this formula yet.",
                    summary,
                    extra,
                )
        }
    }

    private fun triggerHeavyHaptic() {
        vibrate(PREDEFINED_HEAVY_CLICK, HEAVY_HAPTIC_FALLBACK_MS)
    }

    private fun triggerLightTick() {
        vibrate(PREDEFINED_TICK, LIGHT_HAPTIC_FALLBACK_MS)
    }

    @Suppress("DEPRECATION")
    private fun vibrate(effectId: Int, fallbackDurationMs: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (!vibrator.hasVibrator()) return

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                vibrator.vibrate(VibrationEffect.createPredefined(effectId))
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        fallbackDurationMs,
                        VibrationEffect.DEFAULT_AMPLITUDE,
                    ),
                )
            }

            else -> vibrator.vibrate(fallbackDurationMs)
        }
    }

    private fun updateRatioLabel(ratio: Double) {
        binding.ratioValue.text = getString(R.string.ratio_value, formatAmount(ratio))
    }

    private fun formatAmount(value: Double): String {
        return decimalFormat.format(value)
    }

    private fun ValueAnimator.doOnEnd(action: () -> Unit) {
        addListener(
            object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    action()
                }
            },
        )
    }

    private data class AlchemyResult(
        val name: String,
        val description: String,
        val summary: String,
        val auraModifier: String,
    )

    private companion object {
        const val DEFAULT_RATIO = 16.0
        const val BREW_ANIMATION_DURATION_MS = 1_500L
        const val DISCOVERY_ANIMATION_DURATION_MS = 420L
        const val NAME_FADE_DURATION_MS = 500L
        const val DISCOVERY_OFFSET_Y = 80f
        const val SHIMMER_DURATION_MS = 850L
        const val SHIMMER_ALPHA = 0.28f
        const val RESULT_CARD_RADIUS_DP = 26f
        const val RESULT_PULSE_DURATION_MS = 1_800L
        const val RESULT_PULSE_SCALE = 1.018f
        const val PREDEFINED_TICK = 2
        const val PREDEFINED_HEAVY_CLICK = 5
        const val HEAVY_HAPTIC_FALLBACK_MS = 45L
        const val LIGHT_HAPTIC_FALLBACK_MS = 14L
    }
}