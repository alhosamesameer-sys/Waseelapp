package com.waseel.assistant.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calls")
data class CallEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val callerName: String?, val callerNumber: String?, val startedAt: Long, val durationSeconds: Long = 0, val direction: String, val status: String, val handledByAssistant: Boolean = false, val consentTranscript: Boolean = false)

@Entity(tableName = "conversations")
data class ConversationEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val callId: Long, val createdAt: Long)

@Entity(tableName = "messages")
data class MessageEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val conversationId: Long, val speaker: String, val text: String, val timestamp: Long)

@Entity(tableName = "call_summaries")
data class CallSummaryEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val callId: Long, val summary: String, val priority: String, val editedAt: Long)

@Entity(tableName = "contacts")
data class ContactEntity(@PrimaryKey val phoneNumber: String, val displayName: String, val allowed: Boolean = true)

@Entity(tableName = "assistant_rules")
data class AssistantRuleEntity(@PrimaryKey val ruleId: String, val priority: Int, val phrasesJson: String, val response: String, val enabled: Boolean = true)

@Entity(tableName = "assistant_settings")
data class AssistantSettingsEntity(@PrimaryKey val id: Int = 1, val enabled: Boolean = false, val retentionDays: Int = 30, val transcriptEnabled: Boolean = true)

@Entity(tableName = "follow_up_tasks")
data class FollowUpTaskEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val callId: Long, val title: String, val description: String, val dueAt: Long?, val priority: String, val status: String = "pending")
