package com.waseel.assistant.conversation

import android.content.Context
import org.json.JSONObject

class AssistantConfigRepository(private val context: Context) {
    fun loadDefaults(): JSONObject {
        val source = context.assets.open("waseel_assistant.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        return JSONObject(source)
    }
    fun validate(candidate: String): Result<JSONObject> = AssistantConfigValidator.validate(candidate)
}
