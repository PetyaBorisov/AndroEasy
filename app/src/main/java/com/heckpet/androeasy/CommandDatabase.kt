// CommandDatabase.kt
package com.heckpet.androeasy

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Command::class], version = 1, exportSchema = false)
abstract class CommandDatabase : RoomDatabase() {
    abstract fun commandDao(): CommandDao
}