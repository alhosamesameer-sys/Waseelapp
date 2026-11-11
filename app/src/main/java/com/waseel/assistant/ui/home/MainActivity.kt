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

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var enabled = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.filterValues { it }.keys
        val missing = results.filterValues { !it }.keys
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
        binding.btnToggleAssistant.setOnClickListener {
            enabled = !enabled
            binding.tvAssistantState.text = if (enabled) "وصيل مفعّل محلياً — ميزات المكالمات بانتظار التحقق" else "المساعد متوقف"
            binding.btnToggleAssistant.text = if (enabled) "إيقاف المساعد" else "تشغيل المساعد"
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, AssistantSettingsActivity::class.java))
        }
        binding.btnPermissions.setOnClickListener { explainAndRequestPermissions() }
        binding.tvEngineStatus.text = "الصوت المحلي: يلزم فحص محرك الجهاز قبل تفعيل أي محادثة صوتية."
        if (!getSharedPreferences("waseel_setup", MODE_PRIVATE).getBoolean("permission_intro_seen", false)) {
            binding.root.post { explainAndRequestPermissions() }
        }
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
