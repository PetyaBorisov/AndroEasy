package com.heckpet.androeasy

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CommandDao {
    @Query("SELECT * FROM commands WHERE title LIKE '%' || :query || '%' OR command LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<Command>

    @Query("SELECT * FROM commands ORDER BY usageCount DESC LIMIT 10")
    suspend fun getPopular(): List<Command>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(command: Command)

    @Query("UPDATE commands SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsage(id: Int)

    @Query("SELECT * FROM commands WHERE isFavorite = 1")
    suspend fun getFavorites(): List<Command>
}