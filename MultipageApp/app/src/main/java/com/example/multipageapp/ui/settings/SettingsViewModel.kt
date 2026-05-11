package com.example.multipageapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.multipageapp.data.FocusRepository
import com.example.multipageapp.domain.AccentPalette
import com.example.multipageapp.domain.AnimationIntensity
import com.example.multipageapp.domain.AppTheme
import com.example.multipageapp.domain.FocusSound
import com.example.multipageapp.domain.NotificationStyle
import com.example.multipageapp.domain.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: FocusRepository
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = repository.userPreferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserPreferences()
    )

    fun setDisplayName(name: String) {
        viewModelScope.launch {
            repository.updatePreferences { it.copy(displayName = name.trim().ifBlank { "Friend" }) }
        }
    }

    fun setDefaultMinutes(min: Int) {
        viewModelScope.launch {
            repository.updatePreferences { it.copy(defaultSessionMinutes = min.coerceIn(5, 120)) }
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { repository.updatePreferences { it.copy(theme = theme) } }
    }

    fun setAccent(accent: AccentPalette) {
        viewModelScope.launch { repository.updatePreferences { it.copy(accent = accent) } }
    }

    fun setSound(sound: FocusSound) {
        viewModelScope.launch { repository.updatePreferences { it.copy(sound = sound) } }
    }

    fun setNotificationStyle(style: NotificationStyle) {
        viewModelScope.launch { repository.updatePreferences { it.copy(notificationStyle = style) } }
    }

    fun setAnimationIntensity(intensity: AnimationIntensity) {
        viewModelScope.launch { repository.updatePreferences { it.copy(animationIntensity = intensity) } }
    }

    fun setAutoStartNext(enabled: Boolean) {
        viewModelScope.launch { repository.updatePreferences { it.copy(autoStartNextSession = enabled) } }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearSessions() }
    }
}
