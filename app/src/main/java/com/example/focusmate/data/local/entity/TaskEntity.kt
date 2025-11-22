package com.example.focusmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val taskId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    var projectId: String? = null,         
    val title: String = "",
    val note: String? = null,

    val status: TaskStatus = TaskStatus.PENDING, 
    val priority: TaskPriority = TaskPriority.NONE, 

    val dueDate: Long? = null,      
    
    val estimatedPomodoros: Int = 1,    
    val completedPomodoros: Int = 0,

    
    val createdAt: Long = System.currentTimeMillis(),
    var completedAt: Long? = null,
    var lastModified: Long = System.currentTimeMillis()
)


enum class TaskStatus { PENDING, COMPLETED }
enum class TaskPriority { NONE, LOW, MEDIUM, HIGH }
