package com.waseel.assistant.conversation

import org.json.JSONObject

object AssistantConfigValidator {
    private val required = listOf("schema_version", "assistant_identity", "privacy", "intents", "faq", "clarification", "appointments", "task_extraction", "call_ending", "summary", "local_engine")
    fun validate(candidate: String): Result<JSONObject> = runCatching {
        val parsed = JSONObject(candidate)
        required.forEach { require(parsed.has(it)) { "القسم الإلزامي مفقود: $it" } }
        val privacy = requireNotNull(parsed.optJSONObject("privacy")) { "قسم الخصوصية مفقود" }
        require(!privacy.optBoolean("external_api", false)) { "يُمنع تفعيل API خارجي." }
        require(!privacy.optBoolean("external_ai", false)) { "يُمنع تفعيل ذكاء اصطناعي خارجي." }
        require(!privacy.optBoolean("cloud_processing", false)) { "يُمنع تفعيل المعالجة السحابية." }
        parsed
    }
}
