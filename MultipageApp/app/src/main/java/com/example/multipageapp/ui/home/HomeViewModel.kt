package com.example.multipageapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.multipageapp.data.FocusRepository
import com.example.multipageapp.domain.SessionLog
import com.example.multipageapp.domain.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val preferences: UserPreferences = UserPreferences(),
    val streak: Int = 0,
    val todayMinutes: Int = 0,
    val weekMinutes: Int = 0,
    val lastSession: SessionLog? = null,
    val hasSessions: Boolean = false
)

class HomeViewModel(
    private val repository: FocusRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.userPreferences,
        repository.streak,
        repository.sessions,
        repository.todayRolledMinutes
    ) { prefs, streak, sessions, todayStored ->
        val week = repository.focusMinutesWeek(sessions)
        val todayFromLogs = repository.focusMinutesTodayFromSessions(sessions)
        val today = maxOf(todayStored, todayFromLogs)
        HomeUiState(
            preferences = prefs,
            streak = streak,
            todayMinutes = today,
            weekMinutes = week,
            lastSession = sessions.firstOrNull(),
            hasSessions = sessions.isNotEmpty()
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUiState()
    )
}
