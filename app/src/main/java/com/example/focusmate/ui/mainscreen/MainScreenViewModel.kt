package com.example.focusmate.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.focusmate.R
import com.example.focusmate.data.local.AppDatabase
import com.example.focusmate.data.local.entity.ProjectEntity
import com.example.focusmate.data.model.MenuItem
import com.example.focusmate.data.repository.AuthRepository
import com.example.focusmate.data.repository.ProjectRepository
import kotlinx.coroutines.launch
import java.util.UUID

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProjectRepository
    private val authRepository: AuthRepository

    
    private var currentUserId: String

    
    private val _menuItems = MediatorLiveData<List<MenuItem>>()
    val menuItems: LiveData<List<MenuItem>> = _menuItems

    
    private val _currentUserInfo = MutableLiveData<String>()
    val currentUserInfo: LiveData<String> = _currentUserInfo

    
    private var currentProjectSource: LiveData<List<ProjectEntity>>? = null

    private val staticMenuItems: List<MenuItem> = listOf(
        MenuItem(id = null, R.drawable.wb_sunny_24px, "Hôm nay", "1h 15m", 5, null),
        MenuItem(id = null, R.drawable.wb_twilight_24px, "Ngày mai", "0m", 0, null),
        MenuItem(id = null, R.drawable.calendar_month_24px, "Tuần này", "1h 15m", 5, null),
        MenuItem(id = null, R.drawable.event_available_24px, "Đã lên kế hoạch", "1h 15m", 5, null),
        MenuItem(id = null, R.drawable.event_24px, "Sự kiện", "0m", 0, null),
        MenuItem(id = null, R.drawable.check_circle_24px, "Đã hoàn thành", "0m", 0, null),
        MenuItem(id = null, R.drawable.task_24px, "Nhiệm vụ", "1h 15m", 5, null)
    )

    init {
        val db = AppDatabase.getDatabase(application)
        val projectDao = db.projectDao()
        authRepository = AuthRepository(application)
        repository = ProjectRepository(projectDao)

        
        currentUserId = authRepository.getCurrentUserId()

        
        reloadData()
    }

    
    fun reloadData() {
        
        currentUserId = authRepository.getCurrentUserId()

        
        updateUserInfo()

        
        repository.syncProjects(currentUserId, viewModelScope)

        
        
        currentProjectSource?.let {
            _menuItems.removeSource(it)
        }

        
        val newSource = repository.getAllProjects(currentUserId)
        currentProjectSource = newSource

        
        _menuItems.addSource(newSource) { projects ->
            combineLists(projects)
        }
    }

    
    fun signOut() {
        viewModelScope.launch {
            
            authRepository.signOut()

            
            reloadData()
        }
    }

    
    fun checkUserStatus() {
        
        reloadData()
    }

    private fun updateUserInfo() {
        val user = authRepository.getCurrentFirebaseUser()
        if (user != null) {
            val displayName = user.displayName
            val email = user.email
            _currentUserInfo.value = if (!displayName.isNullOrBlank()) {
                displayName
            } else if (!email.isNullOrBlank()) {
                email
            } else {
                "Người dùng"
            }
        } else {
            _currentUserInfo.value = "Đăng Nhập | Đăng Ký"
        }
    }

    private fun combineLists(projects: List<ProjectEntity>?) {
        val newList = mutableListOf<MenuItem>()
        newList.addAll(staticMenuItems)

        projects?.forEach { projectEntity ->
            newList.add(
                MenuItem(
                    id = projectEntity.projectId,
                    iconRes = R.drawable.ic_circle,
                    title = projectEntity.name,
                    focusedTime = "0m",
                    taskCount = 0,
                    colorString = projectEntity.color
                )
            )
        }

        newList.add(
            MenuItem(id = null, iconRes = R.drawable.outline_add_24, title = "Thêm Dự Án", focusedTime = "", taskCount = -1, null)
        )

        _menuItems.value = newList
    }

    fun addProject(projectName: String, colorString: String) {
        viewModelScope.launch {
            
            
            val currentList = currentProjectSource?.value
            val newOrder = currentList?.size ?: 0

            val newProject = ProjectEntity(
                projectId = UUID.randomUUID().toString(),
                userId = currentUserId, 
                name = projectName,
                color = colorString,
                order = newOrder,
                createdAt = System.currentTimeMillis(),
                lastModified = System.currentTimeMillis()
            )
            repository.insert(newProject)
        }
    }

    fun deleteProject(menuItem: MenuItem) {
        if (menuItem.id == null) return

        
        val projectEntity = currentProjectSource?.value?.find { it.projectId == menuItem.id }

        if (projectEntity != null) {
            viewModelScope.launch {
                repository.delete(projectEntity)
            }
        }
    }

    fun updateProject(id: String, newName: String, newColorString: String) {
        viewModelScope.launch {
            val originalProject = currentProjectSource?.value?.find { it.projectId == id }
            if (originalProject != null) {
                val updatedProject = originalProject.copy(
                    name = newName,
                    color = newColorString,
                    lastModified = System.currentTimeMillis()
                )
                repository.update(updatedProject)
            }
        }
    }
}