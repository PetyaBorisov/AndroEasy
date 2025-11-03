// CommandExecutor.kt
package com.heckpet.androeasy

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CommandExecutor {
    suspend fun executeWithAI(context: Context, query: String): String = withContext(Dispatchers.IO) {
        val db = Room.databaseBuilder(
            context.applicationContext,
            CommandDatabase::class.java,
            "command_database"
        )
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()

        // 1. Поиск в локальной БД
        val localResults = db.commandDao().search(query)
        if (localResults.isNotEmpty()) {
            val cmd = localResults.first()
            db.commandDao().incrementUsage(cmd.id)
            return@withContext RootShell.exec(cmd.command)
        }

        // 2. Проверка промптов
        if (!SubscriptionManager.consumePrompt(context)) {
            return@withContext "Лимит промптов исчерпан. Осталось: ${SubscriptionManager.getRemainingPrompts(context)}"
        }

        // 3. Запрос к GigaChat
        val aiCommand = GigaChatApi.generateCommand(query)
        if (aiCommand.startsWith("Ошибка")) {
            return@withContext aiCommand  // Ошибка API
        }

        val output = RootShell.exec(aiCommand)

        // 4. Сохраняем в БД (обучаемость!)
        val newCmd = Command(
            title = query.take(50),
            command = aiCommand,
            source = "ai",
            usageCount = 1
        )
        db.commandDao().insert(newCmd)

        return@withContext output
    }
}