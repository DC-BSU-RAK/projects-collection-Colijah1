package com.example.multipageapp.ui.focus

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.multipageapp.audio.AmbientSoundPlayer
import com.example.multipageapp.data.FocusRepository
import com.example.multipageapp.domain.FocusSound
import com.example.multipageapp.domain.UserPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FocusSessionUiState(
    val preferences: UserPreferences = UserPreferences(),
    val plannedMinutes: Int = 25,
    val sessionTitle: String = "",
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val pauseCount: Int = 0,
    val startedAtMs: Long = 0L,
    val elapsedFocusedSeconds: Int = 0,
    /** Haptic / UI pulse when crossing 25%, 50%, 75% */
    val progressTick: Int = 0,
    val autoStartNext: Boolean = false,
    val scheduleAutoRestart: Boolean = false
)

class FocusViewModel(
    private val repository: FocusRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val navMinutes: Int =
        savedStateHandle.get<Int>(MINUTES_ARG)?.takeIf { it in 5..120 } ?: DEFAULT_MINUTES

    private val ambient = AmbientSoundPlayer()
    private val _ui = MutableStateFlow(
        FocusSessionUiState(
            plannedMinutes = navMinutes,
            remainingSeconds = navMinutes * 60,
            sessionTitle = "${navMinutes} min focus"
        )
    )
    val uiState: StateFlow<FocusSessionUiState> = _ui.asStateFlow()

    private var tickJob: Job? = null
    private var lastThresholdCrossed = -1

    init {
        repository.userPreferences
            .distinctUntilChanged { old, new ->
                old.sound == new.sound &&
                    old.animationIntensity == new.animationIntensity &&
                    old.notificationStyle == new.notificationStyle &&
                    old.autoStartNextSession == new.autoStartNextSession
            }
            .onEach { prefs ->
                val planned = navMinutes.coerceIn(5, 120)
                _ui.update { s ->
                    s.copy(
                        preferences = prefs,
                        plannedMinutes = planned,
                        remainingSeconds = if (s.isRunning || s.isFinished) s.remainingSeconds else planned * 60,
                        sessionTitle = if (s.sessionTitle.isNotBlank()) s.sessionTitle else "${planned} min focus",
                        autoStartNext = prefs.autoStartNextSession
                    )
                }
                syncAmbient(prefs.sound)
            }
            .launchIn(viewModelScope)
    }

    private fun syncAmbient(sound: FocusSound) {
        if (_ui.value.isRunning && !_ui.value.isFinished) ambient.start(sound)
        else ambient.stop()
    }

    fun updateSessionTitle(title: String) {
        _ui.update { it.copy(sessionTitle = title) }
    }

    fun startSession() {
        if (_ui.value.isFinished) return
        if (_ui.value.remainingSeconds <= 0) {
            _ui.update { it.copy(remainingSeconds = it.plannedMinutes * 60) }
        }
        _ui.update {
            it.copy(
                isRunning = true,
                startedAtMs = if (it.startedAtMs == 0L) System.currentTimeMillis() else it.startedAtMs
            )
        }
        syncAmbient(_ui.value.preferences.sound)
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (_ui.value.isRunning && !_ui.value.isFinished) {
                delay(1_000)
                val cur = _ui.value
                if (cur.remainingSeconds <= 0) {
                    completeSession(full = true)
                    break
                }
                val newRem = (cur.remainingSeconds - 1).coerceAtLeast(0)
                val newElapsed = cur.elapsedFocusedSeconds + 1
                val planned = cur.plannedMinutes * 60
                val progressed = planned - newRem
                val pct = if (planned > 0) (progressed * 100) / planned else 0
                val threshold = when {
                    pct >= 75 -> 3
                    pct >= 50 -> 2
                    pct >= 25 -> 1
                    else -> 0
                }
                val tickBump = if (threshold > lastThresholdCrossed) {
                    lastThresholdCrossed = threshold
                    _ui.value.progressTick + 1
                } else {
                    _ui.value.progressTick
                }
                _ui.update {
                    it.copy(
                        remainingSeconds = newRem,
                        elapsedFocusedSeconds = newElapsed,
                        progressTick = tickBump
                    )
                }
                if (newRem == 0) {
                    completeSession(full = true)
                    break
                }
            }
        }
    }

    fun pauseSession() {
        if (!_ui.value.isRunning) return
        tickJob?.cancel()
        _ui.update { it.copy(isRunning = false, pauseCount = it.pauseCount + 1) }
        ambient.stop()
    }

    fun completeSession(full: Boolean) {
        tickJob?.cancel()
        val s = _ui.value
        if (s.isFinished) return
        ambient.stop()
        _ui.update {
            it.copy(
                isRunning = false,
                isFinished = true,
                remainingSeconds = 0,
                scheduleAutoRestart = it.preferences.autoStartNextSession
            )
        }
        viewModelScope.launch {
            repository.recordSession(
                title = s.sessionTitle,
                plannedMinutes = s.plannedMinutes,
                focusedSeconds = s.elapsedFocusedSeconds.coerceAtLeast(0),
                completedFully = full,
                startedAtMs = if (s.startedAtMs == 0L) System.currentTimeMillis() else s.startedAtMs,
                pauseCount = s.pauseCount
            )
        }
    }

    fun resetForNewSession(minutes: Int) {
        tickJob?.cancel()
        ambient.stop()
        lastThresholdCrossed = -1
        val m = minutes.coerceIn(5, 120)
        _ui.update {
            FocusSessionUiState(
                preferences = it.preferences,
                plannedMinutes = m,
                sessionTitle = "${m} min focus",
                remainingSeconds = m * 60,
                autoStartNext = it.preferences.autoStartNextSession
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        ambient.stop()
        tickJob?.cancel()
    }

    companion object {
        const val MINUTES_ARG = "minutes"
        const val DEFAULT_MINUTES = 25
    }
}
