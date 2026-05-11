package com.example.multipageapp.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.multipageapp.domain.AccentPalette
import com.example.multipageapp.domain.AnimationIntensity
import com.example.multipageapp.domain.AppTheme
import com.example.multipageapp.domain.FocusScoring
import com.example.multipageapp.domain.FocusSound
import com.example.multipageapp.domain.InsightEngine
import com.example.multipageapp.domain.NotificationStyle
import com.example.multipageapp.domain.SessionFeedback
import com.example.multipageapp.domain.SessionLog
import com.example.multipageapp.domain.UserPreferences
import java.util.Calendar
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "focusflow_compose")

/**
 * **DataStore** preferences + session JSON. See [updatePreferences] / [recordSession] for writes.
 */
class FocusRepository(private val context: Context) {

    private val _sessionFeedback = MutableStateFlow<SessionFeedback?>(null)
    val sessionFeedback: StateFlow<SessionFeedback?> = _sessionFeedback.asStateFlow()

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { p -> p.toUserPreferences() }

    val sessions: Flow<List<SessionLog>> = context.dataStore.data.map { p ->
        parseSessions(p[Keys.SESSIONS_JSON] ?: "[]")
    }

    val streak: Flow<Int> = context.dataStore.data.map { it[Keys.STREAK] ?: 0 }

    val todayRolledMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.TODAY_MIN] ?: 0 }

    suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        context.dataStore.edit { prefs ->
            val cur = prefs.toUserPreferences()
            val next = transform(cur)
            prefs[Keys.NAME] = next.displayName
            prefs[Keys.DEFAULT_MIN] = next.defaultSessionMinutes.coerceIn(5, 120)
            prefs[Keys.THEME] = next.theme.name
            prefs[Keys.ACCENT] = next.accent.name
            prefs[Keys.SOUND] = next.sound.name
            prefs[Keys.NOTIFY] = next.notificationStyle.name
            prefs[Keys.ANIM] = next.animationIntensity.name
            prefs[Keys.AUTO_NEXT] = next.autoStartNextSession
        }
    }

    suspend fun clearSessions() {
        context.dataStore.edit {
            it[Keys.SESSIONS_JSON] = "[]"
            it[Keys.STREAK] = 0
            it[Keys.LAST_STREAK_DAY] = ""
            it[Keys.TODAY_MIN] = 0
            it[Keys.TODAY_KEY] = ""
        }
    }

    suspend fun recordSession(
        title: String,
        plannedMinutes: Int,
        focusedSeconds: Int,
        completedFully: Boolean,
        startedAtMs: Long,
        pauseCount: Int
    ) {
        val score = FocusScoring.scorePercent(plannedMinutes, focusedSeconds, completedFully)
        val session = SessionLog(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "Focus" },
            plannedMinutes = plannedMinutes,
            focusedSeconds = focusedSeconds,
            completedFully = completedFully,
            startedAtEpochMs = startedAtMs,
            pauseCount = pauseCount,
            focusScorePercent = score
        )
        var historyForFeedback: List<SessionLog> = emptyList()
        context.dataStore.edit { prefs ->
            val existing = parseSessions(prefs[Keys.SESSIONS_JSON] ?: "[]")
            val next = (listOf(session) + existing).take(120)
            historyForFeedback = next
            prefs[Keys.SESSIONS_JSON] = serializeSessions(next)
            rollTodayMinutes(prefs, focusedSeconds / 60)
            if (completedFully && score >= 58) updateStreak(prefs)
        }
        _sessionFeedback.value = SessionFeedback(
            scorePercent = score,
            headline = InsightEngine.sessionHeadline(session),
            detail = InsightEngine.sessionDetail(session, historyForFeedback)
        )
    }

    fun consumeSessionFeedback() {
        _sessionFeedback.value = null
    }

    fun focusMinutesTodayFromSessions(sessions: List<SessionLog>): Int {
        val cal = Calendar.getInstance()
        val y = cal.get(Calendar.YEAR)
        val d = cal.get(Calendar.DAY_OF_YEAR)
        return sessions
            .filter {
                val c = Calendar.getInstance().apply { timeInMillis = it.startedAtEpochMs }
                c.get(Calendar.YEAR) == y && c.get(Calendar.DAY_OF_YEAR) == d
            }
            .sumOf { it.focusedSeconds } / 60
    }

    fun focusMinutesWeek(sessions: List<SessionLog>): Int {
        val now = System.currentTimeMillis()
        val weekMs = 7L * 24 * 60 * 60 * 1000
        return sessions
            .filter { now - it.startedAtEpochMs <= weekMs }
            .sumOf { it.focusedSeconds } / 60
    }

    private fun Preferences.toUserPreferences(): UserPreferences = UserPreferences(
        displayName = this[Keys.NAME] ?: "Friend",
        defaultSessionMinutes = this[Keys.DEFAULT_MIN] ?: 25,
        theme = this[Keys.THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.DARK,
        accent = this[Keys.ACCENT]?.let { runCatching { AccentPalette.valueOf(it) }.getOrNull() }
            ?: AccentPalette.AURORA,
        sound = this[Keys.SOUND]?.let { runCatching { FocusSound.valueOf(it) }.getOrNull() }
            ?: FocusSound.NONE,
        notificationStyle = this[Keys.NOTIFY]?.let { runCatching { NotificationStyle.valueOf(it) }.getOrNull() }
            ?: NotificationStyle.GENTLE,
        animationIntensity = this[Keys.ANIM]?.let { runCatching { AnimationIntensity.valueOf(it) }.getOrNull() }
            ?: AnimationIntensity.MEDIUM,
        autoStartNextSession = this[Keys.AUTO_NEXT] ?: false
    )

    private fun rollTodayMinutes(prefs: MutablePreferences, added: Int) {
        val key = todayKey()
        val stored = prefs[Keys.TODAY_KEY] ?: ""
        val base = if (stored == key) prefs[Keys.TODAY_MIN] ?: 0 else 0
        prefs[Keys.TODAY_KEY] = key
        prefs[Keys.TODAY_MIN] = base + added.coerceAtLeast(0)
    }

    private fun updateStreak(prefs: MutablePreferences) {
        val today = todayKey()
        val last = prefs[Keys.LAST_STREAK_DAY] ?: ""
        val cur = prefs[Keys.STREAK] ?: 0
        val next = when {
            last == today -> cur
            last == yesterdayKey() -> cur + 1
            else -> 1
        }
        prefs[Keys.STREAK] = next
        prefs[Keys.LAST_STREAK_DAY] = today
    }

    private fun todayKey(): String {
        val c = Calendar.getInstance()
        return "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
    }

    private fun yesterdayKey(): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -1)
        return "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
    }

    private fun parseSessions(json: String): List<SessionLog> = try {
        val arr = JSONArray(json)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    SessionLog(
                        id = o.getString("id"),
                        title = o.getString("title"),
                        plannedMinutes = o.getInt("plannedMinutes"),
                        focusedSeconds = o.getInt("focusedSeconds"),
                        completedFully = o.getBoolean("completedFully"),
                        startedAtEpochMs = o.getLong("startedAt"),
                        pauseCount = o.getInt("pauseCount"),
                        focusScorePercent = o.getInt("score")
                    )
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun serializeSessions(list: List<SessionLog>): String {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("title", s.title)
                    put("plannedMinutes", s.plannedMinutes)
                    put("focusedSeconds", s.focusedSeconds)
                    put("completedFully", s.completedFully)
                    put("startedAt", s.startedAtEpochMs)
                    put("pauseCount", s.pauseCount)
                    put("score", s.focusScorePercent)
                }
            )
        }
        return arr.toString()
    }

    private object Keys {
        val NAME = stringPreferencesKey("display_name")
        val DEFAULT_MIN = intPreferencesKey("default_minutes")
        val THEME = stringPreferencesKey("theme")
        val ACCENT = stringPreferencesKey("accent")
        val SOUND = stringPreferencesKey("sound")
        val NOTIFY = stringPreferencesKey("notify")
        val ANIM = stringPreferencesKey("anim")
        val AUTO_NEXT = booleanPreferencesKey("auto_next")
        val SESSIONS_JSON = stringPreferencesKey("sessions_json")
        val STREAK = intPreferencesKey("streak")
        val LAST_STREAK_DAY = stringPreferencesKey("last_streak_day")
        val TODAY_MIN = intPreferencesKey("today_focus_min")
        val TODAY_KEY = stringPreferencesKey("today_key")
    }
}
