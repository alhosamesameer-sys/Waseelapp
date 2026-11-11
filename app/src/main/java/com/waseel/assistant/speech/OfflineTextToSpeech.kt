package com.waseel.assistant.speech

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import java.util.Locale

class OfflineTextToSpeech(context: Context, private val onReady: (Boolean, String) -> Unit) {
    private var engine: TextToSpeech? = null
    init {
        engine = TextToSpeech(context) { status ->
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
            val localVoice = if (Build.VERSION.SDK_INT >= 21) {
                tts.voices?.firstOrNull { voice ->
                    voice.locale.language == "ar" && !voice.isNetworkConnectionRequired
                }
            } else null
            if (Build.VERSION.SDK_INT >= 21 && localVoice == null) {
                onReady(false, "يوجد دعم لغة محتمل، لكن لم يتم العثور على صوت عربي محلي مؤكد. لن نستخدم صوتاً شبكياً.")
                return@TextToSpeech
            }
            if (localVoice != null) tts.voice = localVoice
            val result = tts.setLanguage(locale)
            val supported = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            onReady(supported, if (supported) "تم اختيار صوت عربي غير معتمد على اتصال الشبكة؛ اختبره في وضع الطيران."
                else "الصوت العربي المحلي غير متاح.")
        }
    }
    fun shutdown() { engine?.shutdown(); engine = null }
}
