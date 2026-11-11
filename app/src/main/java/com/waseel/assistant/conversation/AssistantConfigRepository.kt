package com.waseel.assistant.conversation

import android.content.Context
import org.json.JSONObject

class AssistantConfigRepository(private val context: Context) {
    fun loadDefaults(): JSONObject {
        val source = context.assets.open("waseel_assistant.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        return JSONObject(source)
    }
    fun validate(candidate: String): Result<JSONObject> = runCatching {
        val parsed = JSONObject(candidate)
        val required = listOf("schema_version", "assistant_identity", "privacy", "intents", "faq", "clarification", "appointments", "task_extraction", "call_ending", "summary", "local_engine")
        required.forEach { require(parsed.has(it)) { "القسم الإلزامي مفقود: $it" } }
        require(!parsed.optJSONObject("privacy")!!.optBoolean("external_api", false)) { "يُمنع تفعيل API خارجي في هذا الإصدار." }
        parsed
    }
}
