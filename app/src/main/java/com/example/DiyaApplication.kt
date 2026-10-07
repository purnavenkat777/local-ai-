package com.example

import android.app.Application
import com.example.data.local.DiyaDatabase
import com.example.network.LlamaClient
import com.example.repository.DiyaRepository

class DiyaApplication : Application() {
    val database: DiyaDatabase by lazy {
        DiyaDatabase.getDatabase(this)
    }

    val llamaClient: LlamaClient by lazy {
        LlamaClient()
    }

    val repository: DiyaRepository by lazy {
        DiyaRepository(
            conversationDao = database.conversationDao(),
            messageDao = database.messageDao(),
            memoryDao = database.memoryDao(),
            settingDao = database.settingDao(),
            llamaClient = llamaClient
        )
    }
}
