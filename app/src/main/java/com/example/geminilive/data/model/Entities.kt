package com.example.geminilive.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "live_sessions")
data class LiveSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val messageCount: Int = 0,
    val durationSeconds: Long = 0
)

@Entity(tableName = "live_messages")
data class LiveMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val role: String, // "user" or "gemini"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val hasImage: Boolean = false
)
