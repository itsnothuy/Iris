package com.nervesparks.iris.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database for the Iris application.
 *
 * Manages message and conversation persistence and provides access to DAOs.
 */
@Database(
    entities = [MessageEntity::class, ConversationEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Provides access to message persistence operations.
     */
    abstract fun messageDao(): MessageDao

    /**
     * Provides access to conversation persistence operations.
     */
    abstract fun conversationDao(): ConversationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DATABASE_NAME = "iris_database"

        /**
         * Migration from version 1 to 2: Add conversations table and conversationId to messages.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create conversations table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS conversations (
                        id TEXT PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        lastModified INTEGER NOT NULL,
                        messageCount INTEGER NOT NULL,
                        isPinned INTEGER NOT NULL,
                        isArchived INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )

                // Create a default conversation for existing messages
                val currentTime = System.currentTimeMillis()
                database.execSQL(
                    """
                    INSERT INTO conversations (id, title, createdAt, lastModified, messageCount, isPinned, isArchived)
                    VALUES ('default', 'Conversation', $currentTime, $currentTime, 0, 0, 0)
                    """.trimIndent(),
                )

                // Add conversationId column to messages with default value
                database.execSQL(
                    """
                    ALTER TABLE messages ADD COLUMN conversationId TEXT NOT NULL DEFAULT 'default'
                    """.trimIndent(),
                )

                // Create index on conversationId
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_messages_conversationId ON messages(conversationId)
                    """.trimIndent(),
                )

                // Update the message count in the default conversation
                database.execSQL(
                    """
                    UPDATE conversations 
                    SET messageCount = (SELECT COUNT(*) FROM messages WHERE conversationId = 'default')
                    WHERE id = 'default'
                    """.trimIndent(),
                )
            }
        }

        /**
         * Get the singleton database instance.
         * Thread-safe initialization using double-checked locking.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
