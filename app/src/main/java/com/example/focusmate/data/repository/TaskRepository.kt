package com.example.focusmate.data.repository

import androidx.lifecycle.LiveData
import com.example.focusmate.data.local.dao.TaskDao
import com.example.focusmate.data.local.entity.TaskEntity
import com.example.focusmate.data.local.entity.TaskPriority
import com.example.focusmate.data.local.entity.TaskStatus // Import Enum mới

class TaskRepository(private val taskDao: TaskDao) {

    fun getUncompletedTasks(userId: String): LiveData<List<TaskEntity>> {
        return taskDao.getTasksByStatus(userId, TaskStatus.PENDING)
    }
    fun getCompletedTasks(userId: String): LiveData<List<TaskEntity>> {
        return taskDao.getTasksByStatus(userId, TaskStatus.COMPLETED)
    }

    suspend fun addTask(
        title: String,
        estimatedPomodoros: Int,
        userId: String, // Bắt buộc
        projectId: String? = null, // 1. Thêm tham số này vào
        priority: TaskPriority,
        dueDate:Long?
    ) {
        val newTask = TaskEntity(
            title = title,
            estimatedPomodoros = estimatedPomodoros,
            userId = userId,
            projectId = projectId,
            priority = priority,
            dueDate = dueDate
        )
        taskDao.insertTask(newTask)
    }

    suspend fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
        taskDao.updateTaskStatus(taskId, newStatus)

        if (newStatus == TaskStatus.COMPLETED) {
            taskDao.updateCompletedAt(taskId, System.currentTimeMillis())
        } else {
            taskDao.updateCompletedAt(taskId, null)
        }
    }

    suspend fun clearAll() {
        taskDao.clearAll()
    }

    suspend fun getTaskById(taskId: String): TaskEntity? {
        return taskDao.getTaskById(taskId)
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }
}