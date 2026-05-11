package com.example.multipageapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowFocusFlowWorksSheet(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "How FocusFlow works",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "FocusFlow is a smart focus timer that helps you protect deep work. " +
                    "Pick a duration, name your session, and commit to one block of attention.",
                style = MaterialTheme.typography.bodyLarge
            )
            SectionTitle("Sessions & presets")
            Text(
                "Use quick presets (60 / 25 / 10 minutes) or your default from Settings. " +
                    "Named sessions show up in Insights so you can see what you actually worked on.",
                style = MaterialTheme.typography.bodyMedium
            )
            SectionTitle("Adaptive Focus Feedback")
            Text(
                "When a session ends, FocusFlow scores your focus (0–100%) from plan length, " +
                    "time spent in-flow, pauses, and whether you finished the block. Copy updates " +
                    "based—not generic praise.",
                style = MaterialTheme.typography.bodyMedium
            )
            SectionTitle("Sound & haptics")
            Text(
                "Rain and white noise are synthesized in-app (no media files). Gentle milestones " +
                    "tick as you cross 25%, 50%, and 75%—adjust intensity under Settings.",
                style = MaterialTheme.typography.bodyMedium
            )
            SectionTitle("Insights & streaks")
            Text(
                "Insights look at when you focus, how often you pause, and your average score. " +
                    "Streaks reward consistent days with quality sessions.",
                style = MaterialTheme.typography.bodyMedium
            )
            SectionTitle("Privacy")
            Text(
                "Everything stays on device in DataStore—no account, no cloud sync.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Got it")
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(8.dp))
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}
