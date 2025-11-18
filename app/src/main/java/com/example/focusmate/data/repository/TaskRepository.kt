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

    // Khởi tạo Firestore
    private val firestore = Firebase.firestore
    private val TAG = "TaskRepository"

    fun getUncompletedTasks(userId: String, endOfTodayTimestamp: Long): LiveData<List<TaskEntity>> {
        return taskDao.getUncompletedTasks(userId, endOfTodayTimestamp)
    }

//    fun getCompletedTasks(userId: String): LiveData<List<TaskEntity>> {
//        return taskDao.getTasksByStatus(userId, TaskStatus.COMPLETED)
//    }

    fun getTasksCompletedToday(userId: String, startOfToday: Long, endOfToday: Long): LiveData<List<TaskEntity>> {
        return taskDao.getTasksCompletedToday(userId, startOfToday, endOfToday)
    }

    fun getAllTasks(userId: String) : LiveData<List<TaskEntity>> {
        return taskDao.getAllTasks(userId)
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

        // 1. LOCAL: Lưu vào Room trước (Luôn thành công nếu dữ liệu đúng)
        taskDao.insertTask(newTask)

        // 2. REMOTE: Đẩy lên Firestore (Nếu không phải khách)
        if (userId != Constants.GUEST_USER_ID) {
            try {
                firestore.collection("users").document(userId)
                    .collection("tasks").document(newTask.taskId)
                    .set(newTask) // Dùng set để ghi đè hoặc tạo mới
                    .await()
                Log.d(TAG, "Success: Task ${newTask.taskId} synced to Cloud.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed: Could not sync task to Cloud.", e)
            }
        }
    }

    suspend fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
        // 1. LOCAL: Cập nhật Room
        taskDao.updateTaskStatus(taskId, newStatus)
        val completedAt = if (newStatus == TaskStatus.COMPLETED) System.currentTimeMillis() else null
        taskDao.updateCompletedAt(taskId, completedAt)

        // 2. REMOTE: Cập nhật Firestore
        // Vì hàm này chỉ nhận taskId, ta cần lấy Task từ DB ra để biết userId là ai
        val task = taskDao.getTaskById(taskId)
        if (task != null && task.userId != Constants.GUEST_USER_ID) {
            try {
                val updates = mapOf(
                    "status" to newStatus, // Room Converter sẽ tự lo việc enum -> string khi lưu entity, nhưng với map ta nên cẩn thận
                    "completedAt" to completedAt
                )

                firestore.collection("users").document(task.userId)
                    .collection("tasks").document(taskId)
                    .update(updates) // Chỉ update các trường thay đổi
                    .await()
                Log.d(TAG, "Success: Task status updated on Cloud.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed: Could not update task status on Cloud.", e)
            }
        }
    }

    suspend fun clearAll() {
        // Hàm này nguy hiểm nếu dùng với Firestore (xóa hết trên mây)
        // Hiện tại chỉ xóa Local để test
        taskDao.clearAll()
    }

    suspend fun getTaskById(taskId: String): TaskEntity? {
        return taskDao.getTaskById(taskId)
    }

    suspend fun deleteTask(task: TaskEntity) {
        // 1. LOCAL
        taskDao.deleteTask(task)

        // 2. REMOTE
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
        // 1. LOCAL
        taskDao.updateTask(task)

        // 2. REMOTE
        if (task.userId != Constants.GUEST_USER_ID) {
            try {
                firestore.collection("users").document(task.userId)
                    .collection("tasks").document(task.taskId)
                    .set(task) // Ghi đè lại toàn bộ task đã sửa
                    .await()
                Log.d(TAG, "Success: Task updated on Cloud.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed: Could not update task on Cloud.", e)
            }
        }
    }

    fun syncTasks(userId: String, scope: CoroutineScope) {
        // Không đồng bộ cho khách
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
                            // Firestore tự động map String "PENDING" -> Enum TaskStatus.PENDING
                            // nhờ vào cấu trúc data class chuẩn
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