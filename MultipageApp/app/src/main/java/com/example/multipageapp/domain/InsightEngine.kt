package com.example.multipageapp.domain

import java.util.Calendar

/**
 * **Adaptive Focus Feedback** — simple heuristics over session history (no ML).
 */
object InsightEngine {

    fun aggregateInsight(sessions: List<SessionLog>): String {
        if (sessions.isEmpty()) return "Complete a session to unlock rhythm insights."
        val night = sessions.count { hourOf(it.startedAtEpochMs) >= 18 }
        val day = sessions.count { hourOf(it.startedAtEpochMs) in 8..11 }
        if (night >= day + 2 && sessions.size >= 3) {
            return "You focus more at night—consider protecting evening deep-work windows."
        }
        if (day >= night + 2 && sessions.size >= 3) {
            return "You lean into daylight focus—keep mornings clear for hard tasks."
        }
        val avgPause = sessions.map { it.pauseCount }.average()
        if (avgPause >= 2.5) {
            return "You tend to pause frequently—try 15-minute micro-sprints before longer blocks."
        }
        val shortIncomplete = sessions.count { it.plannedMinutes <= 25 && !it.completedFully }
        if (shortIncomplete >= 3) {
            return "Shorter sessions sometimes end early—start with a clear exit criterion."
        }
        val avgScore = sessions.map { it.focusScorePercent }.average()
        return if (avgScore >= 82) {
            "Strong average focus score (${avgScore.toInt()}%). Sustain the cadence."
        } else {
            "Average focus score ${avgScore.toInt()}%. One extra session this week will lift momentum."
        }
    }

    fun sessionHeadline(session: SessionLog): String =
        if (session.completedFully) "Session complete — stellar block."
        else "Session ended early — progress still counts."

    fun sessionDetail(session: SessionLog, historyIncludingSession: List<SessionLog>): String {
        val parts = mutableListOf<String>()
        if (session.pauseCount >= 3) {
            parts += "Frequent pauses detected—next time try a gentler preset."
        }
        if (session.plannedMinutes >= 45 && session.focusScorePercent < 70) {
            parts += "Longer blocks look demanding—alternate with recovery breaks."
        }
        if (hourOf(session.startedAtEpochMs) >= 18) {
            parts += "Evening energy carried this session."
        }
        if (parts.isEmpty()) {
            parts += aggregateInsight(historyIncludingSession).take(160)
        }
        return parts.joinToString(" ")
    }

    private fun hourOf(epochMs: Long): Int =
        Calendar.getInstance().apply { timeInMillis = epochMs }.get(Calendar.HOUR_OF_DAY)
}
