package com.example.focusmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID // <-- Nhớ import

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val projectId: String = UUID.randomUUID().toString(), // Tự tạo UUID
    val userId: String, // Sẽ cần xử lý ở ViewModel
    val name: String,
    val color: String, // Dạng HEx: "#FF5722"
    val order: Int, // Để sắp xếp
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)