package com.waseel.assistant.conversation

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AssistantConfigRepository(private val context: Context) {
    fun loadDefaults(): JSONObject {
        val source = context.assets.open("waseel_assistant.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        return JSONObject(source)
    }

    /** Applies user-edited local preferences to a fresh copy of the validated built-in rules. */
    fun loadEffectiveConfig(): JSONObject {
        val config = loadDefaults()
        val prefs = context.getSharedPreferences("waseel_assistant_custom", Context.MODE_PRIVATE)
        val identity = config.optJSONObject("assistant_identity") ?: JSONObject().also { config.put("assistant_identity", it) }
        identity.put("name", prefs.getString("assistant_name", identity.optString("name", "وصيل")))
        identity.put("self_intro", prefs.getString("intro", identity.optString("self_intro")))
        config.optJSONObject("greetings")?.put("default", prefs.getString("greeting", config.optJSONObject("greetings")?.optString("default")))
        config.optJSONObject("fallbacks")?.put("unknown", prefs.getString("unknown_reply", config.optJSONObject("fallbacks")?.optString("unknown")))
        val modes = config.optJSONObject("operating_modes")
        modes?.put("answer_delay_seconds", prefs.getInt("answer_delay", modes?.optInt("answer_delay_seconds", 5) ?: 5))
        modes?.put("auto_answer_default", prefs.getBoolean("auto_answer", false))

        val intents = config.optJSONObject("intents") ?: JSONObject().also { config.put("intents", it) }
        val faqText = prefs.getString("faq", "").orEmpty()
        faqText.lineSequence().forEachIndexed { index, line ->
            val parts = line.split("|", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                intents.put("custom_faq_$index", JSONObject()
                    .put("priority", 60)
                    .put("phrases", JSONArray().put(parts[0].trim()))
                    .put("response", parts[1].trim()))
            }
        }

        val customRules = context.getSharedPreferences("waseel_feature_data", Context.MODE_PRIVATE)
        val quickReplies = runCatching { JSONArray(customRules.getString("quick_replies", "[]")) }.getOrDefault(JSONArray())
        for (i in 0 until quickReplies.length()) {
            val item = quickReplies.optJSONObject(i) ?: continue
            val phrase = item.optString("title").trim()
            val response = item.optString("response").trim()
            if (phrase.isNotEmpty() && response.isNotEmpty()) {
                intents.put("quick_reply_$i", JSONObject()
                    .put("priority", 55)
                    .put("phrases", JSONArray().put(phrase))
                    .put("response", response))
            }
        }

        val policy = config.optJSONObject("conversation_policy")
        val rules = policy?.optJSONArray("rules") ?: JSONArray()
        prefs.getString("instructions", "").orEmpty().lineSequence()
            .map { it.trim() }.filter { it.isNotEmpty() }
            .forEach { rules.put(it) }
        policy?.put("rules", rules)
        return config
    }

    fun validate(candidate: String): Result<JSONObject> = AssistantConfigValidator.validate(candidate)
}
