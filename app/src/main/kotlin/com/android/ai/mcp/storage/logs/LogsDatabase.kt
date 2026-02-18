package com.android.ai.mcp.storage.logs

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CommandRunEntity::class,
        StepExecutionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class LogsDatabase : RoomDatabase() {

    abstract fun logsDao(): LogsDao

    companion object {
        private const val DB_NAME = "mcp_logs.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE command_runs ADD COLUMN modelId TEXT NOT NULL DEFAULT 'moonshotai/kimi-k2.5'"
                )
                db.execSQL(
                    "ALTER TABLE command_runs ADD COLUMN commandSource TEXT NOT NULL DEFAULT 'manual'"
                )
                db.execSQL(
                    "ALTER TABLE command_runs ADD COLUMN templateId INTEGER"
                )
            }
        }

        fun create(context: Context): LogsDatabase {
            return Room.databaseBuilder(
                context,
                LogsDatabase::class.java,
                DB_NAME
            ).addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
