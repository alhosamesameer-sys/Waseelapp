package com.waseel.assistant.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.waseel.assistant.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var enabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvAssistantState.text = "المساعد متوقف"
        binding.btnToggleAssistant.setOnClickListener {
            enabled = !enabled
            binding.tvAssistantState.text = if (enabled) "المساعد مفعّل محلياً — بانتظار إعداد الاتصالات والصوت" else "المساعد متوقف"
            binding.btnToggleAssistant.text = if (enabled) "إيقاف المساعد" else "تشغيل المساعد"
        }
        binding.tvEngineStatus.text = "الصوت المحلي: يلزم فحص محرك الجهاز قبل تفعيل أي محادثة صوتية."
    }
}
