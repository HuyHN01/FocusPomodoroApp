package com.example.focusmate.data.repository

import com.example.focusmate.data.local.dao.UserDao
import com.example.focusmate.data.local.entity.UserEntity
import com.example.focusmate.data.remote.FirebaseAuthSource

class UserRepository(
    private val firebase: FirebaseAuthSource,
    private val userDao: UserDao
) {

}