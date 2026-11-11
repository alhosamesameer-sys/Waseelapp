package com.waseel.assistant.telecom

/**
 * Audio bridge contract. A route must provide BOTH remote RX and assistant TX.
 * Android InCallService alone does not implement this contract for cellular SIM calls.
 */
enum class AudioRouteKind { DIRECT_SIM, SIP_PBX }
data class AudioRouteCapability(val kind: AudioRouteKind, val available: Boolean, val canReceiveRemoteAudio: Boolean, val canSendAssistantAudio: Boolean, val detail: String)

interface CallAudioRoute {
    suspend fun inspect(): AudioRouteCapability
    suspend fun connect(callReference: String): Result<Unit>
    suspend fun sendAssistantPcm16(audio: ByteArray, sampleRate: Int, channels: Int): Result<Unit>
    fun disconnect()
}

class DirectSimAudioRoute : CallAudioRoute {
    override suspend fun inspect() = AudioRouteCapability(
        AudioRouteKind.DIRECT_SIM, false, false, false,
        "غير مدعوم عبر واجهة Android العامة: InCallService لا يتيح مسار صوت ثنائي الاتجاه لتطبيق عادي."
    )
    override suspend fun connect(callReference: String) = Result.failure<Unit>(UnsupportedOperationException("مسار صوت SIM غير متاح عبر الواجهات العامة."))
    override suspend fun sendAssistantPcm16(audio: ByteArray, sampleRate: Int, channels: Int) = Result.failure<Unit>(UnsupportedOperationException("لا يوجد مسار إرسال صوت إلى مكالمة SIM."))
    override fun disconnect() = Unit
}

/** Implement only after a real SIP/PBX media transport is added and tested. */
class SipPbxAudioRoute : CallAudioRoute {
    override suspend fun inspect() = AudioRouteCapability(
        AudioRouteKind.SIP_PBX, false, false, false,
        "واجهة الربط جاهزة تصميمياً، لكن لم تتم إضافة SIP stack أو حساب/خادم PBX بعد."
    )
    override suspend fun connect(callReference: String) = Result.failure<Unit>(UnsupportedOperationException("SIP transport is not configured."))
    override suspend fun sendAssistantPcm16(audio: ByteArray, sampleRate: Int, channels: Int) = Result.failure<Unit>(UnsupportedOperationException("SIP transport is not configured."))
    override fun disconnect() = Unit
}
