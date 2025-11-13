package com.example.focusmate.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.focusmate.data.local.dao.TaskDao
import com.example.focusmate.data.local.dao.UserDao
import com.example.focusmate.data.local.entity.UserEntity
import com.example.focusmate.data.local.entity.TaskEntity

@Database(entities = [UserEntity::class, TaskEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focusmate_db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
