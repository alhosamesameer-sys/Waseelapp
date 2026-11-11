package com.waseel.assistant.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallDao {
    @Insert suspend fun insertCall(call: CallEntity): Long
    @Insert suspend fun insertMessage(message: MessageEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveSummary(summary: CallSummaryEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveSettings(settings: AssistantSettingsEntity)
    @Query("SELECT * FROM calls ORDER BY startedAt DESC") fun observeCalls(): Flow<List<CallEntity>>
    @Query("SELECT COUNT(*) FROM calls WHERE startedAt >= :from AND startedAt < :until") fun countBetween(from: Long, until: Long): Flow<Int>
    @Query("SELECT COUNT(*) FROM calls") fun totalCalls(): Flow<Int>
    @Query("SELECT COUNT(*) FROM calls WHERE handledByAssistant = 1") fun assistantHandledCalls(): Flow<Int>
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp") suspend fun messagesFor(conversationId: Long): List<MessageEntity>
    @Query("DELETE FROM calls WHERE id = :callId") suspend fun deleteCall(callId: Long)
    @Query("DELETE FROM messages WHERE conversationId IN (SELECT id FROM conversations WHERE callId = :callId)") suspend fun deleteMessagesForCall(callId: Long)
    @Query("DELETE FROM call_summaries WHERE callId = :callId") suspend fun deleteSummaryForCall(callId: Long)
    @Query("DELETE FROM follow_up_tasks WHERE callId = :callId") suspend fun deleteTasksForCall(callId: Long)
}
