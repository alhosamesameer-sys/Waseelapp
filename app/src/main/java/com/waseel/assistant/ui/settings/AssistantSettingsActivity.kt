package com.waseel.assistant.ui.settings

import android.os.Bundle
import android.content.Intent
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.setPadding
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.waseel.assistant.ui.settings.VoiceSettingsActivity
import org.json.JSONObject
import com.waseel.assistant.conversation.AssistantConfigRepository
import com.waseel.assistant.conversation.LocalConversationEngine

class AssistantSettingsActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences("waseel_assistant_custom", MODE_PRIVATE) }
    private lateinit var assistantName: EditText
    private lateinit var intro: EditText
    private lateinit var greeting: EditText
    private lateinit var unknownReply: EditText
    private lateinit var instructions: EditText
    private lateinit var faq: EditText
    private lateinit var delayLabel: TextView
    private lateinit var delay: SeekBar
    private lateinit var autoAnswer: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
            setPadding(20)
            setBackgroundColor(android.graphics.Color.rgb(7, 21, 48))
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
        addText(root, "إعدادات وصيل", 26, true)
        val voiceButton = MaterialButton(this).apply { text = "إعدادات الصوت وتجربة النطق" }
        root.addView(voiceButton, match())
        voiceButton.setOnClickListener { startActivity(Intent(this, VoiceSettingsActivity::class.java)) }
        addText(root, "عدّل شخصية المساعد والردود والتعليمات. تُحفظ الإعدادات على هذا الهاتف فقط.", 14, false)
        assistantName = field(root, "اسم المساعد", "وصيل")
        intro = field(root, "تعريف المساعد بنفسه", "أنا وصيل، المساعد الشخصي لصاحب الهاتف.")
        greeting = field(root, "التحية عند الرد", "السلام عليكم، حياك الله. معك وصيل، المساعد الشخصي لصاحب الهاتف. صاحب الهاتف غير متاح حالياً، كيف أقدر أخدمك؟", true)
        unknownReply = field(root, "الرد عند عدم فهم السؤال", "أعتذر منك، ما فهمت طلبك بشكل واضح. ممكن توضحه لي بطريقة ثانية؟", true)
        faq = field(root, "الأسئلة المتكررة والردود (سؤال ثم رد في كل سطر)", "من أنت؟ | أنا وصيل، المساعد الشخصي لصاحب الهاتف.\nوين صاحب الهاتف؟ | صاحب الهاتف غير متاح حالياً، تحب تترك له رسالة؟", true)
        instructions = field(root, "تعليمات إضافية للمساعد (كل تعليمة في سطر)", "تحدث باللهجة السعودية البيضاء.\nلا تخترع معلومات ولا تؤكد موعداً نيابة عن صاحب الهاتف.\nاسأل سؤالاً واحداً في كل مرة.\nلا تطلب كلمات مرور أو رموز تحقق أو بيانات بنكية.", true)
        addText(root, "الرد بعد كم ثانية من بدء الرنين؟", 16, true)
        delayLabel = addText(root, "", 15, false)
        delay = SeekBar(this).apply { max = 30; progress = prefs.getInt("answer_delay", 5) }
        delay.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                delayLabel.text = "التأخير المحدد: $value ثانية"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        root.addView(delay, match())
        delayLabel.text = "التأخير المحدد: ${delay.progress} ثانية"
        autoAnswer = SwitchMaterial(this).apply {
            text = "السماح بالرد التلقائي (يتطلب دعم نظام الاتصال)"
            isChecked = prefs.getBoolean("auto_answer", false)
        }
        root.addView(autoAnswer, match())
        addText(root, "تنبيه: اختيار الرد التلقائي يحفظ تفضيلك فقط. لن يستطيع التطبيق الرد على مكالمات SIM تلقائياً ما لم يسمح Android/الشركة المصنّعة بذلك. كما أن تأخير الرنين لا يضمن توفر صوت الطرفين.", 13, false)
        val test = MaterialButton(this).apply { text = "اختبار ردود وصيل محلياً" }
        root.addView(test, match())
        test.setOnClickListener { testLocalReply() }
        val save = MaterialButton(this).apply { text = "حفظ الإعدادات" }
        root.addView(save, match())
        save.setOnClickListener { saveSettings() }
        val reset = MaterialButton(this).apply { text = "استعادة الإعدادات الافتراضية" }
        root.addView(reset, match())
        reset.setOnClickListener {
            prefs.edit().clear().apply()
            recreate()
        }
        loadValues()
    }

    private fun loadValues() {
        assistantName.setText(prefs.getString("assistant_name", "وصيل"))
        intro.setText(prefs.getString("intro", "أنا وصيل، المساعد الشخصي لصاحب الهاتف."))
        greeting.setText(prefs.getString("greeting", "السلام عليكم، حياك الله. معك وصيل، المساعد الشخصي لصاحب الهاتف. صاحب الهاتف غير متاح حالياً، كيف أقدر أخدمك؟"))
        unknownReply.setText(prefs.getString("unknown_reply", "أعتذر منك، ما فهمت طلبك بشكل واضح. ممكن توضحه لي بطريقة ثانية؟"))
        faq.setText(prefs.getString("faq", "من أنت؟ | أنا وصيل، المساعد الشخصي لصاحب الهاتف.\nوين صاحب الهاتف؟ | صاحب الهاتف غير متاح حالياً، تحب تترك له رسالة؟"))
        instructions.setText(prefs.getString("instructions", "تحدث باللهجة السعودية البيضاء.\nلا تخترع معلومات ولا تؤكد موعداً نيابة عن صاحب الهاتف.\nاسأل سؤالاً واحداً في كل مرة."))
    }

    private fun testLocalReply() {
        val question = EditText(this).apply { hint = "اكتب سؤالاً لاختبار الرد"; setTextColor(android.graphics.Color.WHITE) }
        AlertDialog.Builder(this).setTitle("اختبار المحرك المحلي").setView(question)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("اختبار") { _, _ ->
                saveSettings()
                val engine = LocalConversationEngine(AssistantConfigRepository(this).loadEffectiveConfig())
                val result = engine.reply(question.text.toString())
                AlertDialog.Builder(this).setTitle("رد وصيل التجريبي")
                    .setMessage(result.response + "\n\nدرجة المطابقة: " + result.confidence)
                    .setPositiveButton("حسناً", null).show()
            }.show()
    }

    private fun saveSettings() {
        val name = assistantName.text.toString().trim()
        if (name.isBlank()) { assistantName.error = "اكتب اسم المساعد"; return }
        val data = JSONObject().apply {
            put("assistant_name", name)
            put("intro", intro.text.toString())
            put("greeting", greeting.text.toString())
            put("unknown_reply", unknownReply.text.toString())
            put("faq", faq.text.toString())
            put("instructions", instructions.text.toString())
            put("answer_delay_seconds", delay.progress)
            put("auto_answer_preference", autoAnswer.isChecked)
        }
        prefs.edit()
            .putString("assistant_name", name)
            .putString("intro", intro.text.toString())
            .putString("greeting", greeting.text.toString())
            .putString("unknown_reply", unknownReply.text.toString())
            .putString("faq", faq.text.toString())
            .putString("instructions", instructions.text.toString())
            .putInt("answer_delay", delay.progress)
            .putBoolean("auto_answer", autoAnswer.isChecked)
            .putString("custom_config_json", data.toString(2))
            .apply()
        Toast.makeText(this, "تم حفظ إعدادات وصيل على الهاتف", Toast.LENGTH_LONG).show()
    }

    private fun field(parent: LinearLayout, label: String, default: String, multiline: Boolean = false): EditText {
        addText(parent, label, 15, true)
        val edit = EditText(this).apply {
            setText(default)
            textSize = 15f
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.LTGRAY)
            setPadding(14)
            setBackgroundColor(android.graphics.Color.rgb(20, 43, 78))
            if (multiline) { minLines = 3; gravity = android.view.Gravity.TOP; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE }
        }
        parent.addView(edit, match())
        return edit
    }
    private fun addText(parent: LinearLayout, value: String, size: Int, bold: Boolean): TextView {
        val t = TextView(this).apply {
            text = value; textSize = size.toFloat()
            setTextColor(android.graphics.Color.WHITE)
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 16, 0, 8)
        }
        parent.addView(t, match())
        return t
    }
    private fun match() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
}
