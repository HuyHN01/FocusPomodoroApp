package com.example.focusmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val email: String,
    val displayName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long? = null
)
