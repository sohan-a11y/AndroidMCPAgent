package com.android.ai.mcp.storage.automation

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CredentialEntryEntity::class,
        TaskTemplateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AutomationDatabase : RoomDatabase() {

    abstract fun automationDao(): AutomationDao

    companion object {
        private const val DB_NAME = "mcp_automation.db"

        fun create(context: Context): AutomationDatabase {
            return Room.databaseBuilder(
                context,
                AutomationDatabase::class.java,
                DB_NAME
            ).build()
        }
    }
}
