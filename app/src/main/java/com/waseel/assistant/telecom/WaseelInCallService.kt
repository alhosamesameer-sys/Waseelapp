package com.waseel.assistant.telecom

import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import org.json.JSONArray
import java.util.Calendar

/**
 * Handles Telecom call state only when WASEEL is selected as the default dialer.
 * Answering a call does not grant access to cellular uplink/downlink audio.
 */
class WaseelInCallService : InCallService() {
    private val handler = Handler(Looper.getMainLooper())
    private val pending = mutableMapOf<Call, Runnable>()

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                if (state != Call.STATE_RINGING) cancelPending(call)
                else scheduleAnswerIfConfigured(call)
            }

            override fun onDetailsChanged(call: Call, details: Call.Details) {
                if (call.state == Call.STATE_RINGING) scheduleAnswerIfConfigured(call)
            }
        })
        if (call.state == Call.STATE_RINGING) scheduleAnswerIfConfigured(call)
    }

    private fun scheduleAnswerIfConfigured(call: Call) {
        if (pending.containsKey(call)) return
        val assistantPrefs = getSharedPreferences("waseel_assistant_custom", MODE_PRIVATE)
        val featurePrefs = getSharedPreferences("waseel_feature_data", MODE_PRIVATE)
        if (!assistantPrefs.getBoolean("auto_answer", false)) return
        if (!withinWorkingHours(featurePrefs)) return
        val number = call.details?.handle?.schemeSpecificPart.orEmpty()
        if (number.isNotBlank()) {
            val excluded = featurePrefs.getString("excluded_numbers", "").orEmpty()
                .split(Regex("[,\\n;]")).map { it.trim() }.filter { it.isNotEmpty() }
            if (excluded.any { number.endsWith(it) || it.endsWith(number) }) return
            val allowed = featurePrefs.getString("allowed_numbers", "").orEmpty()
                .split(Regex("[,\\n;]")).map { it.trim() }.filter { it.isNotEmpty() }
            if (allowed.isNotEmpty() && allowed.none { number.endsWith(it) || it.endsWith(number) }) return
        }
        val delaySeconds = assistantPrefs.getInt("answer_delay", 5).coerceIn(0, 30)
        val task = Runnable {
            pending.remove(call)
            if (call.state == Call.STATE_RINGING && assistantPrefs.getBoolean("auto_answer", false)) {
                try { call.answer(VideoProfile.STATE_AUDIO_ONLY) } catch (_: SecurityException) { }
            }
        }
        pending[call] = task
        handler.postDelayed(task, delaySeconds * 1000L)
    }

    private fun withinWorkingHours(prefs: android.content.SharedPreferences): Boolean {
        if (!prefs.getBoolean("hours_enabled", false)) return true
        val start = prefs.getString("hours_start", "09:00").orEmpty()
        val end = prefs.getString("hours_end", "17:00").orEmpty()
        val startMinutes = parseMinutes(start) ?: return false
        val endMinutes = parseMinutes(end) ?: return false
        val now = Calendar.getInstance()
        val current = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return if (startMinutes <= endMinutes) current in startMinutes..endMinutes
        else current >= startMinutes || current <= endMinutes
    }

    private fun parseMinutes(value: String): Int? {
        val parts = value.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    private fun cancelPending(call: Call) {
        pending.remove(call)?.let(handler::removeCallbacks)
    }

    override fun onCallRemoved(call: Call) {
        cancelPending(call)
        super.onCallRemoved(call)
    }

    override fun onDestroy() {
        pending.values.forEach(handler::removeCallbacks)
        pending.clear()
        super.onDestroy()
    }
}
