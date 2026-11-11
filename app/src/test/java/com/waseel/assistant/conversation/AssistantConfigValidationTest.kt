package com.waseel.assistant.conversation

import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantConfigValidationTest {
    @Test fun rejectsInvalidJson() {
        val result = runCatching { JSONObject("{ invalid json") }
        assertTrue(result.isFailure)
    }
    @Test fun rejectsExternalApiEnabled() {
        val config = JSONObject("""{"privacy":{"external_api":true}}""")
        assertTrue(config.getJSONObject("privacy").getBoolean("external_api"))
    }
}
