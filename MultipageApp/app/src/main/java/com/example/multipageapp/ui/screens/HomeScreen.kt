package com.example.multipageapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.multipageapp.ui.components.HowFocusFlowWorksSheet
import com.example.multipageapp.ui.home.HomeUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onStartFocus: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var customMinutes by rememberSaveable { mutableStateOf("") }
    var showGuide by rememberSaveable { mutableStateOf(false) }
    HowFocusFlowWorksSheet(visible = showGuide, onDismiss = { showGuide = false })
    val p = state.preferences
    val gradient = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            MaterialTheme.colorScheme.surface
        )
    )
    Column(
        modifier
            .fillMaxSize()
            .background(gradient)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RowTopBar(onInfo = { showGuide = true })
        Text(
            "Hello, ${p.displayName}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Today · ${state.todayMinutes} min focus · ${state.weekMinutes} min this week",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Streak", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${state.streak} day streak — keep the rhythm.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Text("Quick start", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PresetChip("60 min") { onStartFocus(60) }
            PresetChip("25 min") { onStartFocus(25) }
            PresetChip("10 min") { onStartFocus(10) }
            PresetChip("Default (${p.defaultSessionMinutes})") { onStartFocus(p.defaultSessionMinutes) }
        }
        OutlinedTextField(
            value = customMinutes,
            onValueChange = { v -> customMinutes = v.filter { ch -> ch.isDigit() }.take(3) },
            label = { Text("Custom minutes (5–120)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            onClick = {
                val m = customMinutes.toIntOrNull()?.coerceIn(5, 120) ?: p.defaultSessionMinutes
                onStartFocus(m)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                Text("Start focus", Modifier.padding(start = 8.dp))
            }
        }
        if (state.lastSession != null) {
            val s = state.lastSession
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Last session", fontWeight = FontWeight.SemiBold)
                    Text(
                        "${s.title} · ${s.plannedMinutes} min planned · score ${s.focusScorePercent}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No sessions yet", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Start your first block—Insights and feedback unlock after you finish one session.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RowTopBar(onInfo: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Text(
            "FocusFlow",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        IconButton(onClick = onInfo, modifier = Modifier.align(Alignment.CenterEnd)) {
            Icon(Icons.Outlined.Info, contentDescription = "How it works")
        }
    }
}

@Composable
private fun PresetChip(label: String, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, shape = RoundedCornerShape(16.dp)) {
        Text(label)
    }
}
