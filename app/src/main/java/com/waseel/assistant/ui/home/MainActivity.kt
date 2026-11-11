package com.waseel.assistant.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CallLog
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.waseel.assistant.databinding.ActivityMainBinding
import com.waseel.assistant.ui.WaseelFeatureActivity
import com.waseel.assistant.ui.settings.AssistantSettingsActivity
import com.waseel.assistant.ui.settings.VoiceSettingsActivity
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var enabled = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val missing = results.filterValues { !it }.keys
        observeDashboardStats()
        binding.tvEngineStatus.text = if (missing.isEmpty()) {
            "الصلاحيات التي وافقت عليها أصبحت متاحة. فحص صوت طرفي المكالمة يظل مستقلاً."
        } else {
            "لم تمنح بعض الصلاحيات: ${missing.joinToString()}. يمكنك مراجعتها لاحقاً."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enabled = getSharedPreferences("waseel_setup", MODE_PRIVATE).getBoolean("assistant_enabled", false)
        renderAssistantState()

        binding.btnToggleAssistant.setOnClickListener {
            enabled = !enabled
            getSharedPreferences("waseel_setup", MODE_PRIVATE).edit().putBoolean("assistant_enabled", enabled).apply()
            renderAssistantState()
        }
        binding.btnSettings.setOnClickListener { startActivity(Intent(this, AssistantSettingsActivity::class.java)) }
        binding.btnVoice.setOnClickListener { startActivity(Intent(this, VoiceSettingsActivity::class.java)) }
        binding.btnCalls.setOnClickListener { openFeature("calls") }
        binding.btnQuickReplies.setOnClickListener { openFeature("quick_replies") }
        binding.btnReports.setOnClickListener { openFeature("reports") }
        binding.btnFeatures.setOnClickListener { openFeature("menu") }
        binding.btnPermissions.setOnClickListener { explainAndRequestPermissions() }

        observeDashboardStats()
        binding.tvEngineStatus.text = "الصوت المحلي: افحص الأصوات العربية من إعدادات الصوت قبل استخدامها."
        if (!getSharedPreferences("waseel_setup", MODE_PRIVATE).getBoolean("permission_intro_seen", false)) {
            binding.root.post { explainAndRequestPermissions() }
        }
    }

    private fun renderAssistantState() {
        binding.tvAssistantState.text = if (enabled) {
            "وصيل مفعّل محلياً — جاهزية الرد على مكالمات SIM لم تثبت بعد"
        } else "المساعد متوقف محلياً"
        binding.btnToggleAssistant.text = if (enabled) "إيقاف المساعد" else "تشغيل المساعد"
    }

    private fun openFeature(screen: String) {
        startActivity(Intent(this, WaseelFeatureActivity::class.java).putExtra(WaseelFeatureActivity.EXTRA_SCREEN, screen))
    }

    private fun observeDashboardStats() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            binding.tvCallsToday.text = "مكالمات اليوم\nغير متاح"
            binding.tvCallsWeek.text = "هذا الأسبوع\nغير متاح"
            binding.tvCallsMonth.text = "هذا الشهر\nغير متاح"
            binding.tvCallsTotal.text = "إجمالي المكالمات\nغير متاح"
            binding.tvAnsweredToday.text = "الواردة المجاب عليها اليوم\nغير متاح"
            binding.tvMissedToday.text = "المكالمات الفائتة اليوم\nغير متاح"
            return
        }
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val today = calendar.timeInMillis
        val week = today - 6L * 24 * 60 * 60 * 1000
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val month = calendar.timeInMillis

        fun count(selection: String? = null, args: Array<String>? = null): Int = try {
            contentResolver.query(CallLog.Calls.CONTENT_URI, arrayOf(CallLog.Calls._ID), selection, args, null)?.use { it.count } ?: 0
        } catch (_: SecurityException) { -1 }
        fun display(label: String, value: Int) = "$label\n${if (value < 0) "غير متاح" else value}"

        binding.tvCallsToday.text = display("مكالمات اليوم", count("${CallLog.Calls.DATE} >= ?", arrayOf(today.toString())))
        binding.tvCallsWeek.text = display("هذا الأسبوع", count("${CallLog.Calls.DATE} >= ?", arrayOf(week.toString())))
        binding.tvCallsMonth.text = display("هذا الشهر", count("${CallLog.Calls.DATE} >= ?", arrayOf(month.toString())))
        binding.tvCallsTotal.text = display("إجمالي المكالمات", count())
        val answered = count("${CallLog.Calls.DATE} >= ? AND ${CallLog.Calls.TYPE} = ?", arrayOf(today.toString(), CallLog.Calls.INCOMING_TYPE.toString()))
        val missed = count("${CallLog.Calls.DATE} >= ? AND ${CallLog.Calls.TYPE} = ?", arrayOf(today.toString(), CallLog.Calls.MISSED_TYPE.toString()))
        binding.tvAnsweredToday.text = display("الواردة المجاب عليها اليوم", answered)
        binding.tvMissedToday.text = display("المكالمات الفائتة اليوم", missed)
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) observeDashboardStats()
    }

    private fun explainAndRequestPermissions() {
        val requested = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.RECORD_AUDIO
        ) + if (android.os.Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.POST_NOTIFICATIONS) else emptyArray()
        val missing = requested.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        getSharedPreferences("waseel_setup", MODE_PRIVATE).edit().putBoolean("permission_intro_seen", true).apply()
        if (missing.isEmpty()) {
            binding.tvEngineStatus.text = "الصلاحيات المطلوبة ممنوحة. هذا لا يمنح التطبيق تلقائياً صوت طرفي المكالمة."
            return
        }
        AlertDialog.Builder(this)
            .setTitle("لماذا يطلب وصيل هذه الصلاحيات؟")
            .setMessage(
                "• حالة الهاتف وسجل المكالمات: لمعرفة حالة الاتصال وعرض إحصائيات السجل.\n" +
                "• جهات الاتصال: لعرض اسم المتصل بدلاً من الرقم متى سمح النظام.\n" +
                "• الميكروفون: لاختبار التعرف الصوتي المحلي فقط، وليس لتسجيل طرفي مكالمة SIM.\n" +
                "• الإشعارات: لإظهار تنبيهات حالة المساعد عند دعمها.\n\n" +
                "يمكنك رفض أي صلاحية، وستبقى الميزات المرتبطة بها غير متاحة. لا يرسل وصيل بياناتك إلى السحابة."
            )
            .setNegativeButton("لاحقاً", null)
            .setPositiveButton("متابعة") { _, _ -> permissionLauncher.launch(missing) }
            .show()
    }
}
