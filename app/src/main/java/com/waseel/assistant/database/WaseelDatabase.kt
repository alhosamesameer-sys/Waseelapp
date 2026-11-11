package com.waseel.assistant.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CallEntity::class, ConversationEntity::class, MessageEntity::class, CallSummaryEntity::class, ContactEntity::class, AssistantRuleEntity::class, AssistantSettingsEntity::class, FollowUpTaskEntity::class], version = 1, exportSchema = true)
abstract class WaseelDatabase : RoomDatabase() {
    abstract fun callDao(): CallDao
    companion object {
        @Volatile private var INSTANCE: WaseelDatabase? = null
        fun get(context: Context): WaseelDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, WaseelDatabase::class.java, "waseel.db").build().also { INSTANCE = it }
        }
    }
}
