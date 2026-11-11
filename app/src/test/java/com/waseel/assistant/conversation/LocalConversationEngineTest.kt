package com.waseel.assistant.conversation

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalConversationEngineTest {
    private val config = JSONObject("""{"intents":{"leave_message":{"phrases":["أبي أترك رسالة","بلغه"],"response":"تفضل قل رسالتك"},"unknown":{"response":"وضح لي"}}}""")
    @Test fun normalizesArabicLetters() {
        assertEquals("السلام عليكم", LocalConversationEngine(config).normalizeArabic("السَّلَامُ عَلَيْكُم"))
    }
    @Test fun matchesPhraseRule() {
        assertEquals("leave_message", LocalConversationEngine(config).reply("أبي أترك رسالة").intentId)
    }
    @Test fun usesFallbackForUnknown() {
        assertTrue(LocalConversationEngine(config).reply("شيء غير معروف").response.isNotBlank())
    }
}
