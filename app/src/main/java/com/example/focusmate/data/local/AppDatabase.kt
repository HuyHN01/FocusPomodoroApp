package com.example.focusmate.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.focusmate.data.local.dao.TaskDao
import com.example.focusmate.data.local.dao.UserDao
import com.example.focusmate.data.local.entity.UserEntity

import com.example.focusmate.data.local.dao.ProjectDao
import com.example.focusmate.data.local.entity.ProjectEntity
import com.example.focusmate.data.local.entity.TaskEntity

@Database(entities = [UserEntity::class, TaskEntity::class, ProjectEntity::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao

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
