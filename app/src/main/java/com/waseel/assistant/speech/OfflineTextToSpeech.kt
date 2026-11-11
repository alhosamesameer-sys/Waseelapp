package com.waseel.assistant.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class OfflineTextToSpeech(context: Context, private val onReady: (Boolean, String) -> Unit) {
    private var engine: TextToSpeech? = null
    init {
        engine = TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) { onReady(false, "تعذر تشغيل محرك النطق."); return@TextToSpeech }
            val locale = Locale("ar", "SA")
            val result = engine?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
            val supported = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            onReady(supported, if (supported) "الصوت العربي متاح؛ تحقق من تثبيت بياناته محلياً." else "بيانات الصوت العربي غير مثبتة.")
        }
    }
    fun shutdown() { engine?.shutdown(); engine = null }
}
