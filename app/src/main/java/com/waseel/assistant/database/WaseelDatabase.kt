package com.waseel.assistant.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CallEntity::class, ConversationEntity::class, MessageEntity::class, CallSummaryEntity::class, CallRecordingEntity::class, ContactEntity::class, AssistantRuleEntity::class, AssistantSettingsEntity::class, FollowUpTaskEntity::class], version = 2, exportSchema = true)
abstract class WaseelDatabase : RoomDatabase() {
    abstract fun callDao(): CallDao
    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN audioStartMillis INTEGER")
                db.execSQL("ALTER TABLE messages ADD COLUMN audioEndMillis INTEGER")
                db.execSQL("ALTER TABLE assistant_settings ADD COLUMN audioRecordingEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS call_recordings (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, callId INTEGER NOT NULL, encryptedFilePath TEXT NOT NULL, mimeType TEXT NOT NULL, sampleRate INTEGER NOT NULL, channels INTEGER NOT NULL, durationMillis INTEGER NOT NULL, consentedAt INTEGER NOT NULL, routeKind TEXT NOT NULL)")
            }
        }
        @Volatile private var INSTANCE: WaseelDatabase? = null
        fun get(context: Context): WaseelDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, WaseelDatabase::class.java, "waseel.db")
                .addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
        }
    }
}
