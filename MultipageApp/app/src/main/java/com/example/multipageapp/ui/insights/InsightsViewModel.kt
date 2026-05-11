package com.example.multipageapp.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.multipageapp.data.FocusRepository
import com.example.multipageapp.domain.InsightEngine
import com.example.multipageapp.domain.SessionLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class InsightsUiState(
    val insight: String = "",
    val sessions: List<SessionLog> = emptyList(),
    val totalMinutesAllTime: Int = 0
)

class InsightsViewModel(
    private val repository: FocusRepository
) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> = repository.sessions.map { sessions ->
        val totalSec = sessions.sumOf { it.focusedSeconds }
        InsightsUiState(
            insight = InsightEngine.aggregateInsight(sessions),
            sessions = sessions,
            totalMinutesAllTime = totalSec / 60
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())
}
