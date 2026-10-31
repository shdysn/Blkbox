package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.database.dao.CloneDao
import com.example.data.database.dao.InstalledAppDao
import com.example.data.database.entity.CloneInstanceEntity
import com.example.data.database.entity.CloneUsageEntity
import com.example.data.database.entity.InstalledAppEntity

@Database(
    entities = [
        CloneInstanceEntity::class,
        InstalledAppEntity::class,
        CloneUsageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CloneDatabase : RoomDatabase() {

    abstract fun cloneDao(): CloneDao
    abstract fun installedAppDao(): InstalledAppDao

    companion object {
        @Volatile
        private var INSTANCE: CloneDatabase? = null

        fun getInstance(context: Context): CloneDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CloneDatabase::class.java,
                    "app_cloner.db"
                ).fallbackToDestructiveMigration().build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
