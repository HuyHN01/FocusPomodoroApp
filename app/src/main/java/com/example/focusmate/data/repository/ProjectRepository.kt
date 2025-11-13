package com.example.focusmate.data.repository

import androidx.lifecycle.LiveData
import com.example.focusmate.data.local.dao.ProjectDao
import com.example.focusmate.data.local.entity.ProjectEntity

class ProjectRepository(private val projectDao: ProjectDao) {

    val allProjects: LiveData<List<ProjectEntity>> = projectDao.getAllProjects()

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