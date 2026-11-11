package com.waseel.assistant.ui.settings

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.waseel.assistant.speech.OfflineTextToSpeech
import com.waseel.assistant.speech.VoiceSampleStore

class VoiceSettingsActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences("waseel_voice_settings", MODE_PRIVATE) }
    private lateinit var status: TextView
    private lateinit var sampleStatus: TextView
    private lateinit var sampleStore: VoiceSampleStore
    private lateinit var speech: OfflineTextToSpeech
    private lateinit var textInput: EditText
    private lateinit var rateLabel: TextView
    private lateinit var rateSlider: SeekBar
    private var selectedVoiceName: String? = null

    private val samplePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("تعذر قراءة الملف.")
            sampleStore.save(bytes)
            sampleStatus.text = "تم حفظ العينة مشفرة داخل الهاتف. لم يتم إرسالها لأي جهة."
        } catch (error: Exception) {
            sampleStatus.text = error.message ?: "تعذر حفظ العينة الصوتية."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sampleStore = VoiceSampleStore(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
            setPadding(20)
            setBackgroundColor(0xFF061632.toInt())
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)

        addText(root, "إعدادات الصوت", 25, true)
        addText(root, "اختر صوتاً عربياً محلياً من الأصوات المثبتة على جهازك. لا يستخدم وصيل أصواتاً شبكية.", 14, false)
        status = panel(root, "جارٍ فحص الأصوات العربية المحلية...")
        addText(root, "نوع الصوت", 17, true)
        val voiceChoices = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val maleButton = MaterialButton(this).apply { text = "طبقة أقرب للذكر" }
        val femaleButton = MaterialButton(this).apply { text = "طبقة أقرب للأنثى" }
        voiceChoices.addView(maleButton, LinearLayout.LayoutParams(0, dp(52), 1f))
        voiceChoices.addView(femaleButton, LinearLayout.LayoutParams(0, dp(52), 1f))
        root.addView(voiceChoices)
        maleButton.setOnClickListener { chooseGenderPreset("ذكر", 0.90f) }
        femaleButton.setOnClickListener { chooseGenderPreset("أنثى", 1.10f) }

        addText(root, "سرعة الكلام", 17, true)
        rateLabel = addText(root, "", 14, false)
        rateSlider = SeekBar(this).apply { max = 10; progress = prefs.getInt("speech_rate_step", 5) }
        root.addView(rateSlider, match())
        updateRateLabel()
        rateSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = updateRateLabel()
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                prefs.edit().putInt("speech_rate_step", rateSlider.progress).apply()
            }
        })

        addText(root, "جرّب صوت المساعد", 17, true)
        textInput = EditText(this).apply {
            setText("السلام عليكم، حياك الله. معك وصيل، كيف أقدر أخدمك؟")
            minLines = 3
            gravity = Gravity.TOP
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFFB5C6DF.toInt())
            setPadding(14)
            setBackgroundColor(0xFF10264B.toInt())
        }
        root.addView(textInput, match())
        val listVoices = MaterialButton(this).apply { text = "عرض الأصوات العربية المثبتة" }
        root.addView(listVoices, match())
        listVoices.setOnClickListener { showAvailableVoices() }
        val preview = MaterialButton(this).apply { text = "استمع إلى الصوت" }
        root.addView(preview, match())
        preview.setOnClickListener {
            speech.setPitch(prefs.getFloat("voice_pitch", 1.0f))
            val ok = speech.speak(textInput.text.toString(), speechRate())
            if (!ok) status.text = "الصوت المحلي غير جاهز أو لا توجد عينة نطق متاحة."
        }

        addText(root, "إضافة عينة صوتية", 17, true)
        addText(root, "يمكنك حفظ ملف صوتي مرجعي مشفر محلياً. حفظ العينة لا يعني أن استنساخ الصوت أصبح جاهزاً؛ محرك XTTS v2 لم يُدمج بعد داخل Android.", 14, false)
        sampleStatus = panel(root, if (sampleStore.hasSample()) "توجد عينة صوتية مشفرة محفوظة على هذا الهاتف." else "لم تتم إضافة عينة صوتية بعد.")
        val addSample = MaterialButton(this).apply { text = "اختيار عينة صوتية من الجهاز" }
        root.addView(addSample, match())
        addSample.setOnClickListener { samplePicker.launch(arrayOf("audio/*")) }
        val deleteSample = MaterialButton(this).apply { text = "حذف العينة المحفوظة" }
        root.addView(deleteSample, match())
        deleteSample.setOnClickListener {
            sampleStore.delete()
            sampleStatus.text = "تم حذف العينة الصوتية المحلية."
        }
        addText(root, "حالة استنساخ الصوت", 17, true)
        panel(root, "XTTS v2 يدعم العربية واستنساخ الصوت، لكن تشغيله محلياً على هاتف Android يحتاج تهيئة نموذج ووقت تشغيل متوافقين. لم يتم تفعيل الاستنساخ حتى ينجح اختبار حقيقي دون إنترنت.")
        speech = OfflineTextToSpeech(this) { ready, message ->
            runOnUiThread {
                status.text = message
                if (ready) {
                    val saved = prefs.getString("selected_voice_name", null)
                    if (saved != null && speech.selectVoice(saved)) selectedVoiceName = saved
                    else if (speech.availableLocalArabicVoices().isNotEmpty()) {
                        selectedVoiceName = speech.availableLocalArabicVoices().first().name
                        speech.selectVoice(selectedVoiceName!!)
                    }
                    status.text = "$message\nيمكن ضبط طبقة الصوت إلى منخفضة أو مرتفعة. Android لا يعرّف جنس كل صوت بشكل موثوق."
                }
            }
        }
    }

    private fun chooseGenderPreset(label: String, pitch: Float) {
        prefs.edit().putString("voice_presentation", label).putFloat("voice_pitch", pitch).apply()
        speech.setPitch(pitch)
        status.text = "تم اختيار طبقة صوت $label. هذا تعديل تقريبي لطبقة الصوت وليس ضماناً لصوت ذكر أو أنثى حقيقي؛ ذلك يعتمد على الأصوات المثبتة."
    }

    private fun showAvailableVoices() {
        val voices = speech.availableLocalArabicVoices()
        if (voices.isEmpty()) {
            status.text = "لا توجد أصوات عربية محلية متاحة."
            return
        }
        val names = voices.map { it.name }.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle("الأصوات العربية المحلية")
            .setItems(names) { _, which ->
                val voice = voices[which]
                if (speech.selectVoice(voice.name)) {
                    selectedVoiceName = voice.name
                    prefs.edit().putString("selected_voice_name", voice.name).apply()
                    status.text = "تم اختيار الصوت المحلي: ${voice.name}"
                }
            }.show()
    }

    private fun speechRate(): Float = (0.75f + rateSlider.progress * 0.05f).coerceIn(0.75f, 1.25f)
    private fun updateRateLabel() { rateLabel.text = "سرعة النطق: ${String.format("%.2f", speechRate())}x" }
    private fun addText(parent: LinearLayout, value: String, size: Int, bold: Boolean): TextView {
        val view = TextView(this).apply {
            text = value; textSize = size.toFloat(); setTextColor(0xFFF4F8FF.toInt())
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 16, 0, 8)
        }
        parent.addView(view, match())
        return view
    }
    private fun panel(parent: LinearLayout, value: String): TextView {
        val view = TextView(this).apply {
            text = value; textSize = 14f; setTextColor(0xFFCFDDF3.toInt())
            setPadding(16); setBackgroundColor(0xFF10264B.toInt())
        }
        parent.addView(view, match())
        return view
    }
    private fun match() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onDestroy() { if (::speech.isInitialized) speech.shutdown(); super.onDestroy() }
}
