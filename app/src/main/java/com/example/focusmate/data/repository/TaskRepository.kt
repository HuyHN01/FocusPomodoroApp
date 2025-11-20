package com.example.focusmate.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.focusmate.data.local.dao.TaskDao
import com.example.focusmate.data.local.entity.TaskEntity
import com.example.focusmate.data.local.entity.TaskPriority
import com.example.focusmate.data.local.entity.TaskStatus
import com.example.focusmate.util.Constants
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskRepository(private val taskDao: TaskDao) {

    
    private val firestore = Firebase.firestore
    private val TAG = "TaskRepository"

    fun getUncompletedTasksByProject(userId: String, projectId: String): LiveData<List<TaskEntity>> {
        return taskDao.getUncompletedTasksByProject(userId, projectId)
    }

    fun getCompletedTasksByProject(userId: String, projectId: String): LiveData<List<TaskEntity>> {
        return taskDao.getCompletedTasksByProject(userId, projectId)
    }
    fun getUncompletedTasks(userId: String, endOfTodayTimestamp: Long): LiveData<List<TaskEntity>> {
        return taskDao.getUncompletedTasks(userId, endOfTodayTimestamp)
    }





    fun getTasksCompletedToday(userId: String, startOfToday: Long, endOfToday: Long): LiveData<List<TaskEntity>> {
        return taskDao.getTasksCompletedToday(userId, startOfToday, endOfToday)
    }

    fun getAllTasks(userId: String) : LiveData<List<TaskEntity>> {
        return taskDao.getAllTasks(userId)
    }
    fun getAllPendingTasks(userId: String): LiveData<List<TaskEntity>> {
        return taskDao.getAllPendingTasks(userId)
    }
    suspend fun addTask(
        title: String,
        estimatedPomodoros: Int,
        userId: String,
        projectId: String? = null,
        priority: TaskPriority,
        dueDate: Long?
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

        
        if (userId != Constants.GUEST_USER_ID) {
            try {
                firestore.collection("users").document(userId)
                    .collection("tasks").document(newTask.taskId)
                    .set(newTask) 
                    .await()
                Log.d(TAG, "Success: Task ${newTask.taskId} synced to Cloud.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed: Could not sync task to Cloud.", e)
            }
        }
    }

    suspend fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
        
        taskDao.updateTaskStatus(taskId, newStatus)
        val completedAt = if (newStatus == TaskStatus.COMPLETED) System.currentTimeMillis() else null
        taskDao.updateCompletedAt(taskId, completedAt)

        
        
        val task = taskDao.getTaskById(taskId)
        if (task != null && task.userId != Constants.GUEST_USER_ID) {
            try {
                val updates = mapOf(
                    "status" to newStatus, 
                    "completedAt" to completedAt
                )

                firestore.collection("users").document(task.userId)
                    .collection("tasks").document(taskId)
                    .update(updates) 
                    .await()
                Log.d(TAG, "Success: Task status updated on Cloud.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed: Could not update task status on Cloud.", e)
            }
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

        
        if (task.userId != Constants.GUEST_USER_ID) {
            try {
                firestore.collection("users").document(task.userId)
                    .collection("tasks").document(task.taskId)
                    .delete()
                    .await()
                Log.d(TAG, "Success: Task deleted on Cloud.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed: Could not delete task on Cloud.", e)
            }
        }
    }

    suspend fun updateTask(task: TaskEntity) {
        
        taskDao.updateTask(task)

        
        if (task.userId != Constants.GUEST_USER_ID) {
            try {
                firestore.collection("users").document(task.userId)
                    .collection("tasks").document(task.taskId)
                    .set(task) 
                    .await()
                Log.d(TAG, "Success: Task updated on Cloud.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed: Could not update task on Cloud.", e)
            }
        }
    }
    fun getTasksForDateRange(userId: String, startTime: Long, endTime: Long, isCompleted: Boolean): LiveData<List<TaskEntity>> {
        val status = if (isCompleted) TaskStatus.COMPLETED else TaskStatus.PENDING
        return taskDao.getTasksByDateRange(userId, status, startTime, endTime)
    }
    fun syncTasks(userId: String, scope: CoroutineScope) {
        
        if (userId == com.example.focusmate.util.Constants.GUEST_USER_ID) return

        firestore.collection("users").document(userId).collection("tasks")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    scope.launch(Dispatchers.IO) {
                        for (dc in snapshots.documentChanges) {
                            
                            
                            val task = dc.document.toObject(TaskEntity::class.java)

                            when (dc.type) {
                                com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                                    taskDao.insertTask(task)
                                    Log.d(TAG, "Sync: Added task ${task.title}")
                                }
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                    taskDao.updateTask(task)
                                    Log.d(TAG, "Sync: Modified task ${task.title}")
                                }
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                    taskDao.deleteTask(task)
                                    Log.d(TAG, "Sync: Removed task ${task.title}")
                                }
                            }
                        }
                    }
                }
            }
    }

}