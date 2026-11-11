package com.waseel.assistant.conversation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantConfigValidationTest {
    private val base = """{"schema_version":"1","assistant_identity":{},"privacy":{"external_api":false,"external_ai":false,"cloud_processing":false},"intents":{},"faq":[],"clarification":{},"appointments":{},"task_extraction":{},"call_ending":{},"summary":{},"local_engine":{}}"""
    @Test fun rejectsInvalidJson() { assertFalse(AssistantConfigValidator.validate("{ invalid json").isSuccess) }
    @Test fun rejectsExternalApiEnabled() {
        val unsafe = base.replace("\"external_api\":false", "\"external_api\":true")
        assertTrue(AssistantConfigValidator.validate(unsafe).isFailure)
    }
    @Test fun acceptsLocalOnlyConfiguration() { assertTrue(AssistantConfigValidator.validate(base).isSuccess) }
}
