package com.example.focusmate.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.focusmate.data.local.entity.ProjectEntity
import com.example.focusmate.data.local.entity.ProjectWithStats

@Dao
interface ProjectDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProject(project: ProjectEntity)

    @Query("SELECT * FROM projects WHERE userId = :userId ORDER BY `order` ASC")
    fun getAllProjects(userId: String): LiveData<List<ProjectEntity>>

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)
    @Query("""
        SELECT 
            projects.*, 
            COUNT(tasks.taskId) as taskCount, 
            COALESCE(SUM(tasks.estimatedPomodoros), 0) as totalPomodoros
        FROM projects
        LEFT JOIN tasks ON projects.projectId = tasks.projectId AND tasks.status = 'PENDING'
        WHERE projects.userId = :userId
        GROUP BY projects.projectId
        ORDER BY projects.`order` ASC
    """)
    fun getProjectsWithStats(userId: String): LiveData<List<ProjectWithStats>>
}