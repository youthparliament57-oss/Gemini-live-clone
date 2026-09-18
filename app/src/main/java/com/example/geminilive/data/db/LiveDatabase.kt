package com.example.geminilive.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.geminilive.data.model.LiveMessageEntity
import com.example.geminilive.data.model.LiveSessionEntity

@Database(entities = [LiveSessionEntity::class, LiveMessageEntity::class], version = 1, exportSchema = false)
abstract class LiveDatabase : RoomDatabase() {
    abstract fun liveDao(): LiveDao

    companion object {
        @Volatile
        private var INSTANCE: LiveDatabase? = null

        fun getInstance(context: Context): LiveDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LiveDatabase::class.java,
                    "gemini_live.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
