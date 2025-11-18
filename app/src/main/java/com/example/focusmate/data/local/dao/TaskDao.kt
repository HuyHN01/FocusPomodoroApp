package com.example.focusmate.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.focusmate.data.local.entity.TaskEntity
import com.example.focusmate.data.local.entity.TaskStatus // Import Enum mới

@Dao
interface TaskDao {

    // SỬA LỖI 1: Thay thế 2 hàm get... bằng 1 hàm getTasksByStatus
    @Query("SELECT * FROM tasks WHERE status = :status AND userId = :userId ORDER BY createdAt DESC")
    fun getTasksByStatus(userId: String, status: TaskStatus): LiveData<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    // SỬA LỖI 2: Thay thế hàm toggle... bằng 2 hàm update mới

    /**
     * Cập nhật trạng thái (PENDING/COMPLETED) cho một Task
     */
    @Query("UPDATE tasks SET status = :newStatus WHERE taskId = :taskId")
    suspend fun updateTaskStatus(taskId: String, newStatus: TaskStatus)

    /**
     * Cập nhật thời gian hoàn thành
     */
    @Query("UPDATE tasks SET completedAt = :completedAt WHERE taskId = :taskId")
    suspend fun updateCompletedAt(taskId: String, completedAt: Long?)

    @Query("SELECT * FROM tasks WHERE status = 'COMPLETED' AND userId = :userId AND completedAt BETWEEN :startOfToday AND :endOfToday ORDER BY completedAt DESC")
    fun getTasksCompletedToday(userId: String, startOfToday: Long, endOfToday: Long): LiveData<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE status = 'PENDING' AND userId = :userId AND dueDate IS NOT NULL AND dueDate <= :endOfTodayTimestamp ORDER BY priority ASC, dueDate ASC")
    fun getUncompletedTasks(userId: String, endOfTodayTimestamp: Long): LiveData<List<TaskEntity>>

    @Query("DELETE FROM tasks")
    suspend fun clearAll()

    // SỬA LỖI 3: Sửa 'id' thành 'taskId' để khớp Entity
    @Query("SELECT * FROM tasks WHERE taskId = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)


}