package com.example.focusmate.data.repository

import android.content.Context
import com.example.focusmate.data.local.AppDatabase
import com.example.focusmate.data.local.entity.UserEntity

class AuthRepository(context: Context) {
    private val userDao = AppDatabase.getDatabase(context).userDao()

    suspend fun signUp(email: String, password: String): Boolean {
        return try {
            val user = UserEntity(email = email, password = password)
            userDao.insertUser(user)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun signIn(email: String, password: String): UserEntity? {
        return userDao.login(email, password)
    }
}
