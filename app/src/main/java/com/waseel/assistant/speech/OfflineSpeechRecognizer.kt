package com.waseel.assistant.speech

import android.content.Context
import android.speech.SpeechRecognizer

data class SpeechCapability(val available: Boolean, val onDeviceApiAvailable: Boolean, val message: String)

class OfflineSpeechRecognizer(private val context: Context) {
    fun capability(): SpeechCapability {
        if (!SpeechRecognizer.isRecognitionAvailable(context))
            return SpeechCapability(false, false, "لا يوجد محرك تعرف على الكلام مثبت.")
        val onDevice = android.os.Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        return SpeechCapability(onDevice, onDevice,
            if (onDevice) "واجهة التعرف المحلي متاحة؛ يلزم اختبار حزمة العربية على الجهاز."
            else "لا يتوفر محرك تعرف محلي مؤكد. لن يتم التحويل تلقائياً إلى خدمة سحابية.")
    }
}
