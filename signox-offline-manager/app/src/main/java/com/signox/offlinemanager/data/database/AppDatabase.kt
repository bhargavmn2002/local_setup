package com.signox.offlinemanager.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.signox.offlinemanager.data.model.*

@Database(
    entities = [
        Media::class,
        Playlist::class,
        PlaylistItem::class,
        Layout::class,
        LayoutZone::class,
        Schedule::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun mediaDao(): MediaDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun layoutDao(): LayoutDao
    abstract fun scheduleDao(): ScheduleDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "signox_offline_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}