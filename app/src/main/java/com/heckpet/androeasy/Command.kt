package com.heckpet.androeasy

import androidx.room.Entity
import androidx.room.PrimaryKey

// Command.kt
@Entity(tableName = "commands")
data class Command(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,        // "Перезагрузка", "Очистка кэша"
    val command: String,      // "reboot", "pm clear com.example.app"
    val category: String = "Общие", // "Система", "Приложения", "Root"
    val isFavorite: Boolean = false,
    val usageCount: Int = 0,
    val source: String = "local" // "local", "community", "ai"
)