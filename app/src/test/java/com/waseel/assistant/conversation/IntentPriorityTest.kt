package com.waseel.assistant.conversation

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class IntentPriorityTest {
    @Test fun higherPriorityRuleWinsWhenPhrasesOverlap() {
        val config = JSONObject("""{"intents":{"general":{"priority":10,"phrases":["اتصال"],"response":"عام"},"urgent":{"priority":100,"phrases":["اتصال ضروري","ضروري"],"response":"عاجل"},"unknown":{"response":"غير معروف"}}}""")
        assertEquals("urgent", LocalConversationEngine(config).reply("هذا اتصال ضروري").intentId)
    }
}
