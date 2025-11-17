package com.example.focusmate.data.repository

import androidx.lifecycle.LiveData
import com.example.focusmate.data.local.dao.ProjectDao
import com.example.focusmate.data.local.entity.ProjectEntity

class ProjectRepository(private val projectDao: ProjectDao) {

    fun getAllProjects(userId: String): LiveData<List<ProjectEntity>> {
        return projectDao.getAllProjects(userId)
    }

    suspend fun insert(project: ProjectEntity) {
        projectDao.insertProject(project)
    }
    suspend fun update(project: ProjectEntity) {
        projectDao.updateProject(project)
    }

    suspend fun delete(project: ProjectEntity) {
        projectDao.deleteProject(project)
    }
}