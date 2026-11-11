package com.waseel.assistant.conversation

import org.json.JSONObject
import java.text.Normalizer

data class EngineReply(val intentId: String, val response: String, val confidence: Double)

class LocalConversationEngine(private val config: JSONObject) {
    fun normalizeArabic(input: String): String {
        val noMarks = input.replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
        return Normalizer.normalize(noMarks, Normalizer.Form.NFKC)
            .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
            .replace('ى', 'ي').replace('ة', 'ه')
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ").trim()
    }
    fun reply(input: String): EngineReply {
        val text = normalizeArabic(input)
        val intents = config.optJSONObject("intents") ?: JSONObject()
        val ordered = intents.keys().asSequence()
            .mapNotNull { id -> intents.optJSONObject(id)?.let { id to it } }
            .sortedByDescending { (_, item) -> item.optInt("priority", 0) }
        for ((id, item) in ordered) {
            val phrases = item.optJSONArray("phrases") ?: continue
            var hits = 0
            for (i in 0 until phrases.length()) {
                val phrase = normalizeArabic(phrases.optString(i))
                if (phrase.isNotBlank() && (text == phrase || text.contains(phrase))) hits++
            }
            if (hits > 0) return EngineReply(id, item.optString("response", ""), (0.72 + hits * 0.05).coerceAtMost(0.98))
        }
        val fallback = intents.optJSONObject("unknown")?.optString("response")
            ?: config.optJSONObject("fallbacks")?.optString("unknown")
            ?: "أعتذر منك، ممكن توضح لي طلبك؟"
        return EngineReply("unknown", fallback, 0.0)
    }
}
