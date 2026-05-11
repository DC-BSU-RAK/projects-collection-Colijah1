package com.example.multipageapp.domain

import kotlin.math.roundToInt

/**
 * Rule-based **focus score** (0–100 %) from plan and actual focus time.
 */
object FocusScoring {
    fun scorePercent(plannedMinutes: Int, focusedSeconds: Int, completedFully: Boolean): Int {
        val target = plannedMinutes.coerceAtLeast(1) * 60
        val ratio = (focusedSeconds.toFloat() / target).coerceIn(0f, 1.15f)
        val base = if (completedFully) {
            82f + ratio * 18f
        } else {
            35f + ratio * 48f
        }
        return base.roundToInt().coerceIn(22, 100)
    }
}
