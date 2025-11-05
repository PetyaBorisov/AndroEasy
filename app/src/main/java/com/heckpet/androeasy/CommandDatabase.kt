package com.heckpet.androeasy

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Command::class], version = 1, exportSchema = false)
abstract class CommandDatabase : RoomDatabase() {

    abstract fun commandDao(): CommandDao

    companion object {
        @Volatile
        private var INSTANCE: CommandDatabase? = null

        fun getInstance(context: Context): CommandDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CommandDatabase::class.java,
                    "command_database"
                )
                .addCallback(DatabaseCallback(CoroutineScope(Dispatchers.IO), context)) // ← УЛУЧШЕНИЕ
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    // УЛУЧШЕНИЕ: Заполняем базу данных при первом создании
    private class DatabaseCallback(private val scope: CoroutineScope, private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let {
                scope.launch {
                    populateDatabase(it.commandDao())
                }
            }
        }

        suspend fun populateDatabase(commandDao: CommandDao) {
            // Очищаем (на всякий случай)
            // commandDao.deleteAll()

            // Добавляем команды по умолчанию
            val defaultCommands = listOf(
                Command(title = "Перезагрузка", command = "reboot", category = "Система"),
                Command(title = "Перезагрузка в Recovery", command = "reboot recovery", category = "Система"),
                Command(title = "Очистить кэш Google Play", command = "pm clear com.android.vending", category = "Приложения"),
                Command(title = "Показать IP адрес", command = "ip addr show wlan0", category = "Сеть"),
                Command(title = "Список всех приложений", command = "pm list packages", category = "Приложения"),
                Command(title = "Принудительно остановить", command = "am force-stop com.example.app", category = "Приложения"),
                Command(title = "Сделать скриншот", command = "screencap -p /sdcard/screenshot.png", category = "Система")
            )

            defaultCommands.forEach { commandDao.insert(it) }
        }
    }
}
