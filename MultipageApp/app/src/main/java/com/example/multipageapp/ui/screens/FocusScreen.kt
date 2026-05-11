package com.example.multipageapp.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.multipageapp.data.FocusRepository
import com.example.multipageapp.domain.AnimationIntensity
import com.example.multipageapp.domain.NotificationStyle
import com.example.multipageapp.ui.components.FocusSessionRing
import com.example.multipageapp.ui.focus.FocusViewModel
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    vm: FocusViewModel,
    repository: FocusRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val feedback by repository.sessionFeedback.collectAsStateWithLifecycle()
    val view = LocalView.current
    LaunchedEffect(ui.progressTick) {
        if (ui.progressTick > 0) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }
    LaunchedEffect(ui.isFinished, ui.scheduleAutoRestart, feedback) {
        if (ui.isFinished && ui.scheduleAutoRestart && feedback == null) {
            delay(500)
            vm.resetForNewSession(ui.plannedMinutes)
            vm.startSession()
        }
    }
    val totalSec = (ui.plannedMinutes * 60).coerceAtLeast(1)
    val progressRaw = 1f - ui.remainingSeconds.toFloat() / totalSec.toFloat()
    val progress by animateFloatAsState(
        targetValue = progressRaw.coerceIn(0f, 1f),
        animationSpec = tween(480),
        label = "focusProgress"
    )
    Scaffold(
        modifier,
        topBar = {
            TopAppBar(
                title = { Text("Focus session") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (ui.isRunning) vm.pauseSession()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = ui.sessionTitle,
                onValueChange = vm::updateSessionTitle,
                label = { Text("Session name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !ui.isRunning && !ui.isFinished
            )
            BoxWithRing(progress, ui.preferences.animationIntensity)
            val clock = formatClock(ui.remainingSeconds)
            Text(
                clock,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (ui.isFinished) "Session saved · check the feedback card"
                else if (ui.isRunning) "Stay with the block…"
                else "Ready when you are",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(8.dp))
            if (!ui.isFinished) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!ui.isRunning) {
                        Button(
                            onClick = { vm.startSession() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Text("Start", Modifier.padding(start = 8.dp))
                            }
                        }
                    } else {
                        TextButton(onClick = { vm.pauseSession() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Pause, contentDescription = null)
                            Text("Pause", Modifier.padding(start = 8.dp))
                        }
                    }
                    val strict = ui.preferences.notificationStyle == NotificationStyle.STRICT
                    TextButton(
                        onClick = { vm.completeSession(full = false) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                        Text(
                            if (strict) "End now (strict)" else "End session gently",
                            Modifier.padding(start = 8.dp)
                        )
                    }
                }
            } else {
                Text(
                    "Nice work. Open the sheet to read your adaptive feedback.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun BoxWithRing(progress: Float, intensity: AnimationIntensity) {
    Box(
        Modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        FocusSessionRing(
            progress = progress,
            animationIntensity = intensity,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun formatClock(seconds: Int): String {
    val m = TimeUnit.SECONDS.toMinutes(seconds.toLong().coerceAtLeast(0))
    val s = seconds.coerceAtLeast(0) - m * 60
    return "%02d:%02d".format(m, s)
}
