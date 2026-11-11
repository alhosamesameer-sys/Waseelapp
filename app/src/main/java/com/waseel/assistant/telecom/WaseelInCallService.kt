package com.waseel.assistant.telecom

import android.telecom.Call
import android.telecom.InCallService

class WaseelInCallService : InCallService() {
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        // لا يتم الرد أو التقاط صوت المكالمة تلقائياً هنا.
        // يتطلب التحكم الفعلي اختيار التطبيق كمعالج مكالمات افتراضي ودعم النظام.
    }
}
