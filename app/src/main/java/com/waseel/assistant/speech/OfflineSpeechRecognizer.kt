package com.waseel.assistant.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

data class SpeechCapability(val available: Boolean, val onDeviceApiAvailable: Boolean, val message: String)

/** Explicitly on-device recognition only. No network recognizer fallback is attempted. */
class OfflineSpeechRecognizer(private val context: Context) {
    fun capability(): SpeechCapability {
        if (!SpeechRecognizer.isRecognitionAvailable(context))
            return SpeechCapability(false, false, "لا يوجد محرك تعرف على الكلام مثبت.")
        val onDevice = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        return SpeechCapability(onDevice, onDevice, if (onDevice)
            "واجهة التعرف المحلي متاحة؛ يجب اختبار حزمة العربية دون إنترنت على هذا الهاتف."
        else "لا يتوفر محرك محلي مؤكد. لن يتم إرسال الصوت إلى الإنترنت.")
    }

    fun createRecognizer(): SpeechRecognizer? {
        if (Build.VERSION.SDK_INT < 31 || !SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) return null
        return SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
    }

    fun recognitionIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-SA")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
    }

    fun start(listener: RecognitionListener): Result<SpeechRecognizer> = runCatching {
        val recognizer = createRecognizer()
            ?: throw IllegalStateException("محرك التعرف المحلي غير متاح على هذا الجهاز.")
        recognizer.setRecognitionListener(listener)
        recognizer.startListening(recognitionIntent())
        recognizer
    }
}
