package com.waseel.assistant.speech

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

/** Arabic TTS restricted to voices that do not require a network connection. */
class OfflineTextToSpeech(context: Context, private val onReady: (Boolean, String) -> Unit) {
    private var engine: TextToSpeech? = null
    private var localArabicVoices: List<Voice> = emptyList()

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            val tts = engine
            if (status != TextToSpeech.SUCCESS || tts == null) {
                onReady(false, "تعذر تشغيل محرك النطق المثبت.")
                return@TextToSpeech
            }
            val locale = Locale("ar", "SA")
            val languageStatus = tts.isLanguageAvailable(locale)
            if (languageStatus < TextToSpeech.LANG_AVAILABLE) {
                onReady(false, "لا توجد بيانات عربية مناسبة مثبتة.")
                return@TextToSpeech
            }
            tts.setLanguage(locale)
            localArabicVoices = if (Build.VERSION.SDK_INT >= 21) {
                tts.voices.orEmpty().filter { it.locale.language == "ar" && !it.isNetworkConnectionRequired }
            } else emptyList()
            if (Build.VERSION.SDK_INT >= 21 && localArabicVoices.isEmpty()) {
                onReady(false, "لم يتم العثور على صوت عربي محلي مؤكد. لن نستخدم صوتاً شبكياً.")
                return@TextToSpeech
            }
            localArabicVoices.firstOrNull()?.let { tts.voice = it }
            onReady(true, "تم العثور على ${localArabicVoices.size} صوت عربي لا يتطلب الإنترنت. اختر الصوت وجربه.")
        }
    }

    fun availableLocalArabicVoices(): List<Voice> = localArabicVoices

    fun selectVoice(name: String): Boolean {
        val voice = localArabicVoices.firstOrNull { it.name == name } ?: return false
        return engine?.setVoice(voice) == TextToSpeech.SUCCESS
    }

    fun setPitch(pitch: Float) { engine?.setPitch(pitch.coerceIn(0.8f, 1.2f)) }

    fun speak(text: String, rate: Float = 1.0f): Boolean {
        val tts = engine ?: return false
        if (text.isBlank() || localArabicVoices.isEmpty()) return false
        tts.setSpeechRate(rate.coerceIn(0.75f, 1.25f))
        return tts.speak(text.trim(), TextToSpeech.QUEUE_FLUSH, null, "waseel_voice_preview") == TextToSpeech.SUCCESS
    }

    fun stop() { engine?.stop() }
    fun shutdown() { engine?.stop(); engine?.shutdown(); engine = null }
}
