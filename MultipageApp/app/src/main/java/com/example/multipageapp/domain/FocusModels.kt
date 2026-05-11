package com.example.multipageapp.domain

/**
 * Persisted user preferences ([androidx.datastore.preferences.DataStore] in [FocusRepository]).
 */
data class UserPreferences(
    val displayName: String = "Friend",
    val defaultSessionMinutes: Int = 25,
    val theme: AppTheme = AppTheme.DARK,
    val accent: AccentPalette = AccentPalette.AURORA,
    val sound: FocusSound = FocusSound.NONE,
    val notificationStyle: NotificationStyle = NotificationStyle.GENTLE,
    val animationIntensity: AnimationIntensity = AnimationIntensity.MEDIUM,
    val autoStartNextSession: Boolean = false
)

enum class AppTheme { LIGHT, DARK, AMOLED }

enum class AccentPalette { SLATE, OCEAN, LAVENDER, AURORA }

enum class FocusSound { NONE, RAIN, WHITE_NOISE }

enum class NotificationStyle { GENTLE, STRICT }

enum class AnimationIntensity { LOW, MEDIUM, HIGH }

/**
 * One completed focus session (serialized into JSON inside DataStore).
 */
data class SessionLog(
    val id: String,
    val title: String,
    val plannedMinutes: Int,
    val focusedSeconds: Int,
    val completedFully: Boolean,
    val startedAtEpochMs: Long,
    val pauseCount: Int,
    val focusScorePercent: Int
)

/**
 * Shown after a session ends (USP: Adaptive Focus Feedback).
 */
data class SessionFeedback(
    val scorePercent: Int,
    val headline: String,
    val detail: String
)
