package com.example.focusmate.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.focusmate.data.local.dao.ProjectDao
import com.example.focusmate.data.local.entity.ProjectEntity
import com.example.focusmate.util.Constants
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProjectRepository(private val projectDao: ProjectDao) {

    private val firestore = Firebase.firestore
    private val TAG = "ProjectRepository"

    fun getAllProjects(userId: String): LiveData<List<ProjectEntity>> {
        return projectDao.getAllProjects(userId)
    }

    suspend fun insert(project: ProjectEntity) {
        
        projectDao.insertProject(project)

        
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
        
        projectDao.updateProject(project)

        
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
        
        projectDao.deleteProject(project)

        
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

    fun syncProjects(userId: String, scope: CoroutineScope) {
        
        if (userId == Constants.GUEST_USER_ID) return

        
        firestore.collection("users").document(userId).collection("projects")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    
                    scope.launch(Dispatchers.IO) {
                        for (dc in snapshots.documentChanges) {
                            
                            val project = dc.document.toObject(ProjectEntity::class.java)

                            when (dc.type) {
                                DocumentChange.Type.ADDED -> {
                                    
                                    projectDao.insertProject(project)
                                    Log.d(TAG, "Sync: Added project ${project.name} from Cloud")
                                }
                                DocumentChange.Type.MODIFIED -> {
                                    
                                    projectDao.updateProject(project)
                                    Log.d(TAG, "Sync: Modified project ${project.name} from Cloud")
                                }
                                DocumentChange.Type.REMOVED -> {
                                    
                                    projectDao.deleteProject(project)
                                    Log.d(TAG, "Sync: Removed project ${project.name} from Cloud")
                                }
                            }
                        }
                    }
                }
            }
    }
}