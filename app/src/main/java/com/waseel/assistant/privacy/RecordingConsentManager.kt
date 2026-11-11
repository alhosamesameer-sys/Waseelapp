package com.waseel.assistant.privacy

import android.content.Context
import android.content.SharedPreferences

class RecordingConsentManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("waseel_consent", Context.MODE_PRIVATE)

    fun grant(callId: String, callerAccepted: Boolean, ownerEnabledRecording: Boolean) {
        require(callId.isNotBlank())
        prefs.edit().putBoolean("caller_$callId", callerAccepted)
            .putBoolean("owner_$callId", ownerEnabledRecording).apply()
    }

    fun mayRecord(callId: String): Boolean =
        prefs.getBoolean("caller_$callId", false) && prefs.getBoolean("owner_$callId", false)

    fun clear(callId: String) { prefs.edit().remove("caller_$callId").remove("owner_$callId").apply() }
}
