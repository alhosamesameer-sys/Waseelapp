package com.waseel.assistant.telecom

import com.waseel.assistant.privacy.EncryptedAudioStore
import com.waseel.assistant.privacy.RecordingConsentManager

/**
 * Accepts frames only from a verified call-audio route. No mic/speaker loopback is used as a fake call recording.
 */
class CallAudioSession(
    private val consent: RecordingConsentManager,
    private val store: EncryptedAudioStore,
    private val route: CallAudioRoute
) {
    suspend fun capability() = route.inspect()

    suspend fun persistVerifiedFrames(callId: Long, callerAccepted: Boolean, ownerEnabled: Boolean, pcm16Frames: ByteArray): Result<String> = runCatching {
        consent.grant(callId.toString(), callerAccepted, ownerEnabled)
        check(consent.mayRecord(callId.toString())) { "لم تكتمل موافقة الطرفين؛ التسجيل ممنوع." }
        val capability = route.inspect()
        check(capability.available && capability.canReceiveRemoteAudio && capability.canSendAssistantAudio) {
            "مسار الصوت الثنائي غير متاح؛ لم يتم تسجيل صوت المكالمة."
        }
        store.save(callId, pcm16Frames).absolutePath
    }
}
