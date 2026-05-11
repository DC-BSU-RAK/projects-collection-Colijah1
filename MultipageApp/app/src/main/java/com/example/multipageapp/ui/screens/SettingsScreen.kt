package com.example.multipageapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.multipageapp.domain.AccentPalette
import com.example.multipageapp.domain.AnimationIntensity
import com.example.multipageapp.domain.AppTheme
import com.example.multipageapp.domain.FocusSound
import com.example.multipageapp.domain.NotificationStyle
import com.example.multipageapp.domain.UserPreferences
import com.example.multipageapp.ui.settings.SettingsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsRoute(
    vm: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("Removes all session logs, streak, and today totals from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.clearHistory()
                        confirmClear = false
                    }
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            }
        )
    }
    SettingsScreen(
        prefs = prefs,
        onName = vm::setDisplayName,
        onDefaultMin = vm::setDefaultMinutes,
        onTheme = vm::setTheme,
        onAccent = vm::setAccent,
        onSound = vm::setSound,
        onNotify = vm::setNotificationStyle,
        onAnim = vm::setAnimationIntensity,
        onAutoNext = vm::setAutoStartNext,
        onClearHistory = { confirmClear = true },
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreen(
    prefs: UserPreferences,
    onName: (String) -> Unit,
    onDefaultMin: (Int) -> Unit,
    onTheme: (AppTheme) -> Unit,
    onAccent: (AccentPalette) -> Unit,
    onSound: (FocusSound) -> Unit,
    onNotify: (NotificationStyle) -> Unit,
    onAnim: (AnimationIntensity) -> Unit,
    onAutoNext: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nameField by rememberSaveable { mutableStateOf(prefs.displayName) }
    var minField by rememberSaveable { mutableStateOf(prefs.defaultSessionMinutes.toString()) }
    LaunchedEffect(prefs.displayName) {
        nameField = prefs.displayName
    }
    LaunchedEffect(prefs.defaultSessionMinutes) {
        minField = prefs.defaultSessionMinutes.toString()
    }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Section("Profile")
        OutlinedTextField(
            value = nameField,
            onValueChange = { nameField = it },
            label = { Text("Display name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        TextButton(
            onClick = { onName(nameField) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save name") }
        Section("Default session length (minutes)")
        OutlinedTextField(
            value = minField,
            onValueChange = { v -> minField = v.filter { c -> c.isDigit() }.take(3) },
            label = { Text("5 – 120") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        TextButton(
            onClick = { onDefaultMin(minField.toIntOrNull() ?: prefs.defaultSessionMinutes) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Apply default length") }
        Section("Theme")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTheme.entries.forEach { t ->
                FilterChip(
                    selected = prefs.theme == t,
                    onClick = { onTheme(t) },
                    label = { Text(t.name) }
                )
            }
        }
        Section("Accent")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccentPalette.entries.forEach { a ->
                FilterChip(
                    selected = prefs.accent == a,
                    onClick = { onAccent(a) },
                    label = { Text(a.name) }
                )
            }
        }
        Section("Ambient sound")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FocusSound.entries.forEach { s ->
                FilterChip(
                    selected = prefs.sound == s,
                    onClick = { onSound(s) },
                    label = { Text(s.name.replace('_', ' ')) }
                )
            }
        }
        Section("Notifications")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NotificationStyle.entries.forEach { n ->
                FilterChip(
                    selected = prefs.notificationStyle == n,
                    onClick = { onNotify(n) },
                    label = { Text(n.name) }
                )
            }
        }
        Section("Animation intensity")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnimationIntensity.entries.forEach { i ->
                FilterChip(
                    selected = prefs.animationIntensity == i,
                    onClick = { onAnim(i) },
                    label = { Text(i.name) }
                )
            }
        }
        Section("Auto-start next session")
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Chain sessions after feedback dismisses",
                modifier = Modifier.weight(1f).padding(end = 12.dp)
            )
            Switch(checked = prefs.autoStartNextSession, onCheckedChange = onAutoNext)
        }
        TextButton(onClick = onClearHistory) { Text("Clear session history") }
        Spacer(Modifier.padding(bottom = 88.dp))
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}
