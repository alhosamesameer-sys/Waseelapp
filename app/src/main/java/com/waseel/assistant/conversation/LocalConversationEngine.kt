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
        val keys = intents.keys().asSequence().toList()
        for (id in keys) {
            val item = intents.optJSONObject(id) ?: continue
            val phrases = item.optJSONArray("phrases") ?: continue
            var hits = 0
            for (i in 0 until phrases.length()) {
                val phrase = normalizeArabic(phrases.optString(i))
                if (phrase.isNotBlank() && text.contains(phrase)) hits++
            }
            if (hits > 0) {
                val response = item.optString("response", "")
                return EngineReply(id, response, (0.72 + hits * 0.05).coerceAtMost(0.98))
            }
        }
        val fallback = config.optJSONObject("intents")?.optJSONObject("unknown")?.optString("response")
            ?: config.optJSONObject("fallbacks")?.optString("unknown")
            ?: "أعتذر منك، ممكن توضح لي طلبك؟"
        return EngineReply("unknown", fallback, 0.0)
    }
}
