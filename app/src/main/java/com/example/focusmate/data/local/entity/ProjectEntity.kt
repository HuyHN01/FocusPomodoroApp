package com.example.focusmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID // <-- Nhớ import

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val projectId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val name: String = "",
    val color: String = "#FFFFFF",
    val order: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)