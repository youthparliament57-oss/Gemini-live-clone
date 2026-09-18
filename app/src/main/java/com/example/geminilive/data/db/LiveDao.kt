package com.example.geminilive.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.geminilive.data.model.LiveMessageEntity
import com.example.geminilive.data.model.LiveSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LiveDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: LiveSessionEntity): Long

    @Update
    suspend fun updateSession(session: LiveSessionEntity)

    @Query("SELECT * FROM live_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<LiveSessionEntity>>

    @Query("SELECT * FROM live_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): LiveSessionEntity?

    @Query("DELETE FROM live_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: LiveMessageEntity): Long

    @Query("SELECT * FROM live_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<LiveMessageEntity>>

    @Query("SELECT * FROM live_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesListForSession(sessionId: Long): List<LiveMessageEntity>
}
