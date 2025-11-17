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

    fun syncProjects(userId: String, scope: CoroutineScope) {
        // Nếu là khách thì không đồng bộ
        if (userId == Constants.GUEST_USER_ID) return

        // Lắng nghe collection 'projects' của user này
        firestore.collection("users").document(userId).collection("projects")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    // Chuyển sang IO Thread để ghi vào Room
                    scope.launch(Dispatchers.IO) {
                        for (dc in snapshots.documentChanges) {
                            // Chuyển đổi Document Firestore thành ProjectEntity
                            val project = dc.document.toObject(ProjectEntity::class.java)

                            when (dc.type) {
                                DocumentChange.Type.ADDED -> {
                                    // Cloud có cái mới -> Thêm vào Room
                                    projectDao.insertProject(project)
                                    Log.d(TAG, "Sync: Added project ${project.name} from Cloud")
                                }
                                DocumentChange.Type.MODIFIED -> {
                                    // Cloud đã sửa -> Cập nhật Room
                                    projectDao.updateProject(project)
                                    Log.d(TAG, "Sync: Modified project ${project.name} from Cloud")
                                }
                                DocumentChange.Type.REMOVED -> {
                                    // Cloud đã xóa -> Xóa khỏi Room
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