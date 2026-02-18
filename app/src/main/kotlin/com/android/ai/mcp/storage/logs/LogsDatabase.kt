package com.android.ai.mcp.storage.logs

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CommandRunEntity::class,
        StepExecutionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LogsDatabase : RoomDatabase() {

    abstract fun logsDao(): LogsDao

    companion object {
        private const val DB_NAME = "mcp_logs.db"

        fun create(context: Context): LogsDatabase {
            return Room.databaseBuilder(
                context,
                LogsDatabase::class.java,
                DB_NAME
            ).build()
        }
    }
}
