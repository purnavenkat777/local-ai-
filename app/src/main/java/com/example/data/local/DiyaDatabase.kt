package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        MemoryEntity::class,
        SettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DiyaDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun settingDao(): SettingDao

    companion object {
        @Volatile
        private var INSTANCE: DiyaDatabase? = null

        fun getDatabase(context: Context): DiyaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DiyaDatabase::class.java,
                    "diya.db"
                ).addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate personal profile memories once on creation
                        CoroutineScope(Dispatchers.IO).launch {
                            val memoryDao = getDatabase(context).memoryDao()
                            val initialMemories = listOf(
                                MemoryEntity(
                                    key = "User Name",
                                    value = "Nanupatruni Purna Venkat",
                                    category = "Personal",
                                    importance = 5
                                ),
                                MemoryEntity(
                                    key = "Programming Interests",
                                    value = "Learning Python, C, and C++ for system and AI development",
                                    category = "Personal",
                                    importance = 4
                                ),
                                MemoryEntity(
                                    key = "Active Project",
                                    value = "Building Diya, a local-first personal AI assistant",
                                    category = "Projects",
                                    importance = 5
                                ),
                                MemoryEntity(
                                    key = "Philosophy & Budget",
                                    value = "Prefers local, offline, open-source solutions where practical; respects limited budget and avoids cloud subscriptions",
                                    category = "Preferences",
                                    importance = 5
                                ),
                                MemoryEntity(
                                    key = "Technical Environment",
                                    value = "Runs Termux on Android with llama.cpp and GGUF models",
                                    category = "Technical",
                                    importance = 4
                                )
                            )
                            for (mem in initialMemories) {
                                memoryDao.insertMemory(mem)
                            }
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
