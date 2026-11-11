package com.waseel.assistant.telecom

import android.content.Context
import android.telecom.TelecomManager
import android.os.Build

data class SimReadiness(val defaultDialer: Boolean, val inCallServiceAvailable: Boolean, val audioDuplexAvailable: Boolean, val note: String)

object SimCallReadiness {
    fun inspect(context: Context): SimReadiness {
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val isDefault = telecom.defaultDialerPackage == context.packageName
        return SimReadiness(
            defaultDialer = isDefault,
            inCallServiceAvailable = true,
            audioDuplexAvailable = false,
            note = if (!isDefault)
                "لم يتم تعيين وصيل كتطبيق الهاتف الافتراضي. يمكن طلب الدور من المستخدم، لكن ذلك لا يمنح صوت المكالمة."
            else
                "وصيل هو تطبيق الهاتف الافتراضي، لكن واجهات Android العامة لا تثبت توفر صوت المكالمة ثنائي الاتجاه."
        )
    }
}
