package com.example.focusmate.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.focusmate.data.local.dao.ProjectDao
import com.example.focusmate.data.local.entity.ProjectEntity
import com.example.focusmate.util.Constants
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.tasks.await

class ProjectRepository(private val projectDao: ProjectDao) {

    private val firestore = Firebase.firestore
    private val TAG = "ProjectRepository"

    fun getAllProjects(userId: String): LiveData<List<ProjectEntity>> {
        return projectDao.getAllProjects(userId)
    }

    suspend fun insert(project: ProjectEntity) {
        // 1. LOCAL
        projectDao.insertProject(project)

        // 2. REMOTE
        if (project.userId != Constants.GUEST_USER_ID) {
            try {
                firestore.collection("users").document(project.userId)
                    .collection("projects").document(project.projectId)
                    .set(project)
                    .await()
                Log.d(TAG, "Success: Project synced to Cloud.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed: Could not sync project to Cloud.", e)
            }
        }
    }

    suspend fun update(project: ProjectEntity) {
        // 1. LOCAL
        projectDao.updateProject(project)

        // 2. REMOTE
        if (project.userId != Constants.GUEST_USER_ID) {
            try {
                firestore.collection("users").document(project.userId)
                    .collection("projects").document(project.projectId)
                    .set(project)
                    .await()
                Log.d(TAG, "Success: Project updated on Cloud.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed: Could not update project on Cloud.", e)
            }
        }
    }

    suspend fun delete(project: ProjectEntity) {
        // 1. LOCAL
        projectDao.deleteProject(project)

        // 2. REMOTE
        if (project.userId != Constants.GUEST_USER_ID) {
            try {
                firestore.collection("users").document(project.userId)
                    .collection("projects").document(project.projectId)
                    .delete()
                    .await()
                Log.d(TAG, "Success: Project deleted on Cloud.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed: Could not delete project on Cloud.", e)
            }
        }
    }
}