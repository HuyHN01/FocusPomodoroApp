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
    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY createdAt DESC")
    fun getTasksByStatus(status: TaskStatus): LiveData<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    // SỬA LỖI 2: Thay thế hàm toggle... bằng 2 hàm update mới

    /**
     * Cập nhật trạng thái (PENDING/COMPLETED) cho một Task
     */
    @Query("UPDATE tasks SET status = :newStatus WHERE taskId = :taskId")
    suspend fun updateTaskStatus(taskId: Int, newStatus: TaskStatus)

    /**
     * Cập nhật thời gian hoàn thành
     */
    @Query("UPDATE tasks SET completedAt = :completedAt WHERE taskId = :taskId")
    suspend fun updateCompletedAt(taskId: Int, completedAt: Long?)


    @Query("DELETE FROM tasks")
    suspend fun clearAll()

    // SỬA LỖI 3: Sửa 'id' thành 'taskId' để khớp Entity
    @Query("SELECT * FROM tasks WHERE taskId = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Int): TaskEntity?

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)
}