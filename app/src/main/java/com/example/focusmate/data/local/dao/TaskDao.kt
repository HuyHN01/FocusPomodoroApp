package com.example.focusmate.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.focusmate.data.local.entity.TaskEntity
import com.example.focusmate.data.local.entity.TaskStatus 

@Dao
interface TaskDao {

    
    @Query("SELECT * FROM tasks WHERE status = :status AND userId = :userId ORDER BY createdAt DESC")
    fun getTasksByStatus(userId: String, status: TaskStatus): LiveData<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    

    
    @Query("UPDATE tasks SET status = :newStatus WHERE taskId = :taskId")
    suspend fun updateTaskStatus(taskId: String, newStatus: TaskStatus)

    
    @Query("UPDATE tasks SET completedAt = :completedAt WHERE taskId = :taskId")
    suspend fun updateCompletedAt(taskId: String, completedAt: Long?)

    @Query("SELECT * FROM tasks WHERE status = 'COMPLETED' AND userId = :userId AND completedAt BETWEEN :startOfToday AND :endOfToday ORDER BY completedAt DESC")
    fun getTasksCompletedToday(userId: String, startOfToday: Long, endOfToday: Long): LiveData<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE status = 'PENDING' AND userId = :userId AND dueDate IS NOT NULL AND dueDate <= :endOfTodayTimestamp ORDER BY priority ASC, dueDate ASC")
    fun getUncompletedTasks(userId: String, endOfTodayTimestamp: Long): LiveData<List<TaskEntity>>

    @Query("DELETE FROM tasks")
    suspend fun clearAll()

    
    @Query("SELECT * FROM tasks WHERE taskId = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)









@Query("""
    SELECT * FROM tasks 
    WHERE userId = :userId 
    AND status = :status 
    AND dueDate >= :startTime 
    AND dueDate <= :endTime 
    ORDER BY priority ASC, dueDate ASC
""")
    fun getTasksByDateRange(userId: String, status: TaskStatus, startTime: Long, endTime: Long): LiveData<List<TaskEntity>>
    @Query("""
        SELECT * FROM tasks 
        WHERE userId = :userId 
        AND status = 'PENDING' 
        AND dueDate <= :endTime 
        ORDER BY priority ASC, dueDate ASC
    """)
    fun getUncompletedTasksUntil(userId: String, endTime: Long): LiveData<List<TaskEntity>>
  
    @Query("SELECT * FROM tasks WHERE userId = :userId")
    fun getAllTasks(userId: String): LiveData<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND projectId = :projectId AND status = 'PENDING' ORDER BY createdAt DESC")
    fun getUncompletedTasksByProject(userId: String, projectId: String): LiveData<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND projectId = :projectId AND status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedTasksByProject(userId: String, projectId: String): LiveData<List<TaskEntity>>
}