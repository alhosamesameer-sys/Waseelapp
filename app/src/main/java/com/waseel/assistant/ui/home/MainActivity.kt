package com.waseel.assistant.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.waseel.assistant.databinding.ActivityMainBinding
import com.waseel.assistant.ui.settings.AssistantSettingsActivity
import com.waseel.assistant.ui.WaseelFeatureActivity
import android.provider.CallLog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var enabled = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.filterValues { it }.keys
        val missing = results.filterValues { !it }.keys
        observeDashboardStats()
        binding.tvEngineStatus.text = if (missing.isEmpty()) {
            "تم منح الصلاحيات المطلوبة التي وافقت عليها. سيظل فحص صوت المكالمة منفصلاً عن صلاحيات الميكروفون."
        } else {
            "بعض الصلاحيات لم تُمنح: ${missing.joinToString()} . يمكنك مراجعتها من إعدادات الهاتف."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvAssistantState.text = "المساعد متوقف محلياً — أكمل إعداد وصيل أولاً"
        enabled = getSharedPreferences("waseel_setup", MODE_PRIVATE).getBoolean("assistant_enabled", false)
        binding.btnToggleAssistant.text = if (enabled) "إيقاف المساعد" else "تشغيل المساعد"
        binding.tvAssistantState.text = if (enabled) "وصيل مفعّل محلياً — ميزات المكالمات بانتظار التحقق" else "المساعد متوقف محلياً"
        binding.btnToggleAssistant.setOnClickListener {
            enabled = !enabled
            getSharedPreferences("waseel_setup", MODE_PRIVATE).edit().putBoolean("assistant_enabled", enabled).apply()
            binding.tvAssistantState.text = if (enabled) "وصيل مفعّل محلياً — ميزات المكالمات بانتظار التحقق" else "المساعد متوقف"
            binding.btnToggleAssistant.text = if (enabled) "إيقاف المساعد" else "تشغيل المساعد"
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, AssistantSettingsActivity::class.java))
        }
        binding.btnPermissions.setOnClickListener { explainAndRequestPermissions() }
        binding.btnFeatures.setOnClickListener { startActivity(Intent(this, WaseelFeatureActivity::class.java).putExtra(WaseelFeatureActivity.EXTRA_SCREEN, "menu")) }
        observeDashboardStats()
        binding.tvEngineStatus.text = "الصوت المحلي: يلزم فحص محرك الجهاز قبل تفعيل أي محادثة صوتية."
        if (!getSharedPreferences("waseel_setup", MODE_PRIVATE).getBoolean("permission_intro_seen", false)) {
            binding.root.post { explainAndRequestPermissions() }
        }
    }

    private fun observeDashboardStats() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            binding.tvCallsToday.text = "مكالمات اليوم\nغير متاح"
            binding.tvCallsWeek.text = "هذا الأسبوع\nغير متاح"
            binding.tvCallsMonth.text = "هذا الشهر\nغير متاح"
            binding.tvCallsTotal.text = "إجمالي المكالمات\nغير متاح"
            return
        }
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }
        val today = calendar.timeInMillis
        val week = today - 6L * 24 * 60 * 60 * 1000
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val month = calendar.timeInMillis
        fun countCalls(from: Long?): Int = try {
            val selection = if (from == null) null else "${CallLog.Calls.DATE} >= ?"
            val args = if (from == null) null else arrayOf(from.toString())
            contentResolver.query(CallLog.Calls.CONTENT_URI, arrayOf(CallLog.Calls._ID), selection, args, null)?.use { it.count } ?: 0
        } catch (_: SecurityException) { -1 }
        binding.tvCallsToday.text = "مكالمات اليوم\n${countCalls(today).let { if (it < 0) "غير متاح" else it }}"
        binding.tvCallsWeek.text = "هذا الأسبوع\n${countCalls(week).let { if (it < 0) "غير متاح" else it }}"
        binding.tvCallsMonth.text = "هذا الشهر\n${countCalls(month).let { if (it < 0) "غير متاح" else it }}"
        binding.tvCallsTotal.text = "إجمالي المكالمات\n${countCalls(null).let { if (it < 0) "غير متاح" else it }}"
    }

    override fun onResume() { super.onResume(); if (::binding.isInitialized) observeDashboardStats() }

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
            binding.tvEngineStatus.text = "الصلاحيات المطلوبة ممنوحة. هذا لا يعني أن Android يسمح بالوصول إلى صوت طرفي المكالمة."
            return
        }
        AlertDialog.Builder(this)
            .setTitle("لماذا يطلب وصيل هذه الصلاحيات؟")
            .setMessage(
                "• حالة الهاتف وسجل المكالمات: لمعرفة حالة الاتصال وعرض سجل المكالمات إذا سمح النظام.\n" +
                "• جهات الاتصال: لتمييز الأسماء المحفوظة بدلاً من الأرقام فقط.\n" +
                "• الميكروفون: لاختبار التعرف الصوتي المحلي فقط، وليس لتسجيل صوت المكالمة أو ادعاء سماع الطرفين.\n" +
                "• الإشعارات: لإظهار تنبيه وملخص بعد المكالمة.\n\n" +
                "يمكنك رفض أي صلاحية، وستبقى الميزات المرتبطة بها معطلة. لا يرسل وصيل بياناتك إلى السحابة."
            )
            .setNegativeButton("لاحقاً", null)
            .setPositiveButton("متابعة") { _, _ -> permissionLauncher.launch(missing) }
            .show()
    }
}
