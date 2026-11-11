package com.waseel.assistant.conversation

enum class ConversationSpeaker { CALLER, WASEEL }
data class ConversationTurn(val speaker: ConversationSpeaker, val text: String, val timestampMillis: Long, val audioStartMillis: Long? = null, val audioEndMillis: Long? = null)
