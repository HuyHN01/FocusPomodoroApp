package com.example.focusmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val taskId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    var projectId: String? = null,         // Có thể null nếu là task "Inbox"
    val title: String = "",
    val note: String? = null,

    val status: TaskStatus = TaskStatus.PENDING, // Dùng Enum (PENDING, COMPLETED)
    val priority: TaskPriority = TaskPriority.NONE, // Dùng Enum (NONE, LOW, MEDIUM, HIGH)

    val dueDate: Long? = null,      // Lưu dạng Timestamp
    // Pomodoro fields
    val estimatedPomodoros: Int = 1,    // Số Pomodoro dự kiến
    val completedPomodoros: Int = 0,// Số Pomodoro đã hoàn thành

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    var completedAt: Long? = null,
    var lastModified: Long = System.currentTimeMillis()
)

// Dùng TypeConverter cho Enum trong Room
enum class TaskStatus { PENDING, COMPLETED }
enum class TaskPriority { NONE, LOW, MEDIUM, HIGH }
