package com.waseel.assistant.ui

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CallLog
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.waseel.assistant.database.WaseelDatabase
import com.waseel.assistant.ui.settings.AssistantSettingsActivity
import com.waseel.assistant.ui.settings.VoiceSettingsActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WaseelFeatureActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences("waseel_feature_data", MODE_PRIVATE) }
    private lateinit var root: LinearLayout
    private val db by lazy { WaseelDatabase.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra(EXTRA_SCREEN) ?: "menu"
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18))
            setBackgroundColor(0xFF061632.toInt())
        }
        setContentView(ScrollView(this).apply { addView(root) })
        when (screen) {
            "menu" -> showMenu()
            "calls" -> showCalls()
            "reports" -> showReports()
            "quick_replies" -> showQuickReplies()
            "privacy" -> showPrivacy()
            "hours" -> showWorkingHours()
            "numbers" -> showNumberLists()
            "backup" -> showBackup()
            "diagnostics" -> showDiagnostics()
            "call_detail" -> showCallDetail()
            else -> showMenu()
        }
    }

    private fun showMenu() {
        header("مركز وصيل", "إدارة المكالمات والردود والخصوصية والتقارير")
        menuButton("سجل المكالمات", "calls")
        menuButton("الردود الجاهزة", "quick_replies")
        menuButton("التقارير والإحصائيات", "reports")
        menuButton("إعدادات الصوت وتجربة النطق") { startActivity(Intent(this, VoiceSettingsActivity::class.java)) }
        menuButton("شخصية المساعد والتعليمات") { startActivity(Intent(this, AssistantSettingsActivity::class.java)) }
        menuButton("ساعات العمل", "hours")
        menuButton("الأرقام المسموحة والمستثناة", "numbers")
        menuButton("الخصوصية والتسجيل", "privacy")
        menuButton("النسخ الاحتياطي", "backup")
        menuButton("فحص النظام والصوت", "diagnostics")
    }

    private fun showCalls() {
        header("سجل المكالمات", "يعرض سجل الهاتف عند منح صلاحية سجل المكالمات.")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            panel("صلاحية سجل المكالمات غير ممنوحة. افتح إعدادات الهاتف وامنح وصيل الصلاحية لعرض السجل.")
            return
        }
        try {
            val cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.DURATION, CallLog.Calls.TYPE),
                null, null, "${CallLog.Calls.DATE} DESC"
            )
            cursor?.use {
                if (it.count == 0) panel("لا توجد مكالمات في سجل الهاتف حالياً.")
                var shown = 0
                while (it.moveToNext() && shown < 100) {
                    val name = it.getString(0) ?: it.getString(1) ?: "رقم غير معروف"
                    val number = it.getString(1) ?: "—"
                    val date = it.getLong(2)
                    val duration = it.getLong(3)
                    val type = when (it.getInt(4)) {
                        CallLog.Calls.INCOMING_TYPE -> "واردة"
                        CallLog.Calls.OUTGOING_TYPE -> "صادرة"
                        CallLog.Calls.MISSED_TYPE -> "فائتة"
                        CallLog.Calls.REJECTED_TYPE -> "مرفوضة"
                        else -> "مكالمة"
                    }
                    panel("$name\n$number\n$type • ${formatDate(date)} • ${duration} ثانية")
                    shown++
                }
            }
        } catch (e: SecurityException) {
            panel("رفض النظام الوصول إلى سجل المكالمات.")
        }
    }

    private fun showReports() {
        header("التقارير والإحصائيات", "الإحصائيات أدناه من سجلات وصيل المحفوظة في قاعدة البيانات، ولا تمثل سجل الهاتف إلا إذا تمت مزامنته.")
        val today = dayStart(System.currentTimeMillis())
        val week = today - 6L * 24 * 60 * 60 * 1000
        val month = monthStart(System.currentTimeMillis())
        val cards = listOf(
            "مكالمات اليوم" to db.callDao().countBetween(today, System.currentTimeMillis()),
            "مكالمات هذا الأسبوع" to db.callDao().countBetween(week, System.currentTimeMillis()),
            "مكالمات هذا الشهر" to db.callDao().countBetween(month, System.currentTimeMillis()),
            "إجمالي المكالمات المسجلة في وصيل" to db.callDao().totalCalls(),
            "مكالمات تعامل معها المساعد" to db.callDao().assistantHandledCalls()
        )
        cards.forEach { (label, flow) ->
            val value = panel("$label\n—")
            lifecycleScope.launch { flow.collectLatest { count -> value.text = "$label\n$count" } }
        }
    }

    private fun showQuickReplies() {
        header("الردود الجاهزة", "أضف ردوداً يمكنك تعديلها وحذفها. تحفظ على الهاتف.")
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list, match())
        val add = actionButton("إضافة رد جديد")
        root.addView(add, match())
        add.setOnClickListener { editReply(-1) }
        renderReplies(list)
    }

    private fun renderReplies(list: LinearLayout) {
        list.removeAllViews()
        val replies = readReplies()
        if (replies.length() == 0) panel("لا توجد ردود مخصصة بعد. أضف أول رد.")
        for (i in 0 until replies.length()) {
            val item = replies.optJSONObject(i) ?: continue
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12)); setBackgroundColor(0xFF10264B.toInt()) }
            val title = TextView(this).apply { text = item.optString("title"); textSize = 17f; setTextColor(0xFFF4F8FF.toInt()) }
            val body = TextView(this).apply { text = item.optString("response"); textSize = 14f; setTextColor(0xFFCFDDF3.toInt()) }
            row.addView(title); row.addView(body)
            val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val edit = actionButton("تعديل")
            val delete = actionButton("حذف")
            buttons.addView(edit, LinearLayout.LayoutParams(0, dp(48), 1f))
            buttons.addView(delete, LinearLayout.LayoutParams(0, dp(48), 1f))
            row.addView(buttons)
            edit.setOnClickListener { editReply(i) }
            delete.setOnClickListener {
                replies.remove(i); saveReplies(replies); renderReplies(list)
            }
            list.addView(row, match().apply { bottomMargin = dp(10) })
        }
    }

    private fun editReply(index: Int) {
        val data = readReplies()
        val old = if (index >= 0) data.optJSONObject(index) ?: JSONObject() else JSONObject()
        val title = EditText(this).apply { hint = "عنوان الرد"; setText(old.optString("title")); setTextColor(0xFFFFFFFF.toInt()) }
        val response = EditText(this).apply { hint = "نص الرد"; setText(old.optString("response")); minLines = 3; setTextColor(0xFFFFFFFF.toInt()) }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12)); addView(title); addView(response) }
        AlertDialog.Builder(this).setTitle(if (index < 0) "إضافة رد" else "تعديل الرد")
            .setView(box).setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ") { _, _ ->
                if (title.text.isNotBlank() && response.text.isNotBlank()) {
                    val item = JSONObject().put("title", title.text.toString().trim()).put("response", response.text.toString().trim())
                    if (index < 0) data.put(item) else data.put(index, item)
                    saveReplies(data)
                    recreate()
                }
            }.show()
    }

    private fun showPrivacy() {
        header("الخصوصية والتسجيل", "بياناتك محلية. لا يبدأ التسجيل الصوتي ما لم يتوفر مسار صوت موثوق وموافقة لازمة.")
        val transcript = switch("حفظ نص المحادثة بعد الموافقة", prefs.getBoolean("transcript", true))
        val audio = switch("طلب حفظ صوت المكالمة (غير متاح حالياً)", prefs.getBoolean("audio", false))
        val consent = switch("اشتراط موافقة المتصل وصاحب الهاتف", true).apply { isEnabled = false }
        val save = actionButton("حفظ إعدادات الخصوصية")
        root.addView(save, match())
        save.setOnClickListener {
            prefs.edit().putBoolean("transcript", transcript.isChecked).putBoolean("audio", audio.isChecked).apply()
            Toast.makeText(this, "حُفظت التفضيلات. لن يتم إنشاء تسجيل صوتي دون مسار مدعوم وموافقة.", Toast.LENGTH_LONG).show()
        }
        panel("التسجيل الصوتي لمكالمات SIM غير مفعل؛ صلاحية الميكروفون لا تمنح التطبيق صوت الطرفين. لا يستخدم وصيل الميكروفون العادي كتسجيل للمكالمة.")
    }

    private fun showWorkingHours() {
        header("ساعات العمل", "حدد متى تفضل تشغيل تفضيل الرد التلقائي. التطبيق لا يرد على مكالمات SIM تلقائياً إلا إذا دعمه النظام.")
        val enabled = switch("تفعيل جدول ساعات العمل", prefs.getBoolean("hours_enabled", false))
        val start = EditText(this).apply { hint = "وقت البداية مثال 09:00"; setText(prefs.getString("hours_start", "09:00")); setTextColor(0xFFFFFFFF.toInt()) }
        val end = EditText(this).apply { hint = "وقت النهاية مثال 17:00"; setText(prefs.getString("hours_end", "17:00")); setTextColor(0xFFFFFFFF.toInt()) }
        root.addView(start, match()); root.addView(end, match())
        val save = actionButton("حفظ ساعات العمل"); root.addView(save, match())
        save.setOnClickListener {
            if (!start.text.matches(Regex("\\d{2}:\\d{2}")) || !end.text.matches(Regex("\\d{2}:\\d{2}"))) {
                start.error = "استخدم الصيغة 09:00"; return@setOnClickListener
            }
            prefs.edit().putBoolean("hours_enabled", enabled.isChecked).putString("hours_start", start.text.toString()).putString("hours_end", end.text.toString()).apply()
            Toast.makeText(this, "تم حفظ الجدول على الهاتف", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showNumberLists() {
        header("إدارة الأرقام", "أضف أرقاماً مسموحة أو مستثناة. هذه القوائم تحفظ محلياً ولا تحجب المكالمات من تلقاء نفسها.")
        showNumberEditor("الأرقام المسموحة", "allowed_numbers")
        showNumberEditor("الأرقام المستثناة", "excluded_numbers")
    }

    private fun showNumberEditor(title: String, key: String) {
        addText(title, 17, true)
        val input = EditText(this).apply { hint = "أدخل رقم الهاتف"; setTextColor(0xFFFFFFFF.toInt()) }
        root.addView(input, match())
        val list = TextView(this).apply { setTextColor(0xFFCFDDF3.toInt()); text = prefs.getString(key, "") ?: "" }
        root.addView(list, match())
        val add = actionButton("إضافة رقم"); root.addView(add, match())
        add.setOnClickListener {
            val number = input.text.toString().trim()
            if (number.isBlank()) { input.error = "أدخل الرقم"; return@setOnClickListener }
            val values = (prefs.getString(key, "") ?: "").split("\n").filter { it.isNotBlank() }.toMutableSet()
            values.add(number)
            val joined = values.joinToString("\n")
            prefs.edit().putString(key, joined).apply()
            list.text = joined; input.text.clear()
        }
    }

    private fun showBackup() {
        header("النسخ الاحتياطي", "أنشئ ملف JSON محلياً يحتوي على إعدادات الردود والقوائم والتفضيلات.")
        val export = actionButton("تصدير نسخة احتياطية")
        root.addView(export, match())
        export.setOnClickListener {
            val payload = JSONObject()
                .put("quick_replies", readReplies())
                .put("assistant_settings", JSONObject(getSharedPreferences("waseel_assistant_custom", MODE_PRIVATE).all.mapValues { it.value }))
                .put("feature_settings", JSONObject(prefs.all.mapValues { it.value }))
            val share = Intent(Intent.ACTION_SEND).apply { type = "application/json"; putExtra(Intent.EXTRA_TEXT, payload.toString(2)) }
            startActivity(Intent.createChooser(share, "حفظ أو مشاركة نسخة وصيل"))
        }
        panel("الاستعادة من ملف خارجي لم تُفعّل بعد؛ لن أسمح باستيراد إعدادات غير متحقق منها.")
    }

    private fun showDiagnostics() {
        header("فحص النظام", "حالة إمكانات الجهاز الحالية.")
        val telephony = getSystemService(android.telecom.TelecomManager::class.java)
        panel("إصدار Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        panel("تطبيق الاتصال الافتراضي: ${telephony?.defaultDialerPackage ?: "غير معروف"}")
        panel("محرك النطق: يتم التحقق من الأصوات العربية المحلية عند فتح إعدادات الصوت.")
        panel("صوت المكالمة ثنائي الاتجاه: غير مثبت عبر واجهات Android العامة. لا يتم تشغيل محاكاة أو تسجيل وهمي.")
        panel("التعرف الصوتي دون إنترنت: يعتمد على وجود محرك محلي يدعم العربية في هذا الهاتف.")
    }

    private fun showCallDetail() {
        header("تفاصيل المكالمة", "لا توجد تفاصيل فعلية ما لم تتوفر مكالمة مسجلة في قاعدة بيانات وصيل.")
        panel("يمكن فتح سجل الهاتف لعرض تفاصيل المكالمات التي يسمح النظام بعرضها.")
        menuButton("فتح سجل المكالمات", "calls")
    }

    private fun header(title: String, subtitle: String) {
        addText(title, 25, true); addText(subtitle, 14, false)
        val back = actionButton("رجوع"); root.addView(back, match())
        back.setOnClickListener { finish() }
    }
    private fun menuButton(label: String, screen: String) = menuButton(label) {
        startActivity(Intent(this, WaseelFeatureActivity::class.java).putExtra(EXTRA_SCREEN, screen))
    }
    private fun menuButton(label: String, action: () -> Unit) {
        val b = actionButton(label); root.addView(b, match()); b.setOnClickListener { action() }
    }
    private fun addText(value: String, size: Int, bold: Boolean) {
        val t = TextView(this).apply { text = value; textSize = size.toFloat(); setTextColor(0xFFF4F8FF.toInt()); if (bold) setTypeface(null, 1); setPadding(0, dp(14), 0, dp(6)) }
        root.addView(t, match())
    }
    private fun panel(value: String): TextView {
        val t = TextView(this).apply { text = value; textSize = 15f; setTextColor(0xFFCFDDF3.toInt()); setPadding(dp(14)); setBackgroundColor(0xFF10264B.toInt()) }
        root.addView(t, match().apply { bottomMargin = dp(8) }); return t
    }
    private fun actionButton(label: String) = MaterialButton(this).apply { text = label }
    private fun switch(label: String, checked: Boolean) = android.widget.Switch(this).apply { text = label; isChecked = checked; setTextColor(0xFFF4F8FF.toInt()); root.addView(this, match()) }
    private fun readReplies(): JSONArray = runCatching { JSONArray(prefs.getString("quick_replies", "[]")) }.getOrDefault(JSONArray())
    private fun saveReplies(data: JSONArray) { prefs.edit().putString("quick_replies", data.toString()).apply() }
    private fun dayStart(time: Long): Long { val c = java.util.Calendar.getInstance().apply { timeInMillis = time; set(java.util.Calendar.HOUR_OF_DAY,0); set(java.util.Calendar.MINUTE,0); set(java.util.Calendar.SECOND,0); set(java.util.Calendar.MILLISECOND,0) }; return c.timeInMillis }
    private fun monthStart(time: Long): Long { val c = java.util.Calendar.getInstance().apply { timeInMillis=time; set(java.util.Calendar.DAY_OF_MONTH,1); set(java.util.Calendar.HOUR_OF_DAY,0); set(java.util.Calendar.MINUTE,0); set(java.util.Calendar.SECOND,0); set(java.util.Calendar.MILLISECOND,0) }; return c.timeInMillis }
    private fun formatDate(time: Long) = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar")).format(Date(time))
    private fun match() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    companion object { const val EXTRA_SCREEN = "waseel_screen" }
}
