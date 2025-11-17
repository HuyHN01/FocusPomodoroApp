package com.example.focusmate.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.example.focusmate.R
import com.example.focusmate.data.local.AppDatabase
import com.example.focusmate.data.repository.ProjectRepository
import com.example.focusmate.data.model.MenuItem
import com.example.focusmate.data.local.entity.ProjectEntity

// 1. Thêm import cho AuthRepository
import com.example.focusmate.data.repository.AuthRepository

import kotlinx.coroutines.launch
import java.util.UUID

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProjectRepository

    // 2. Thêm AuthRepository để lấy userId
    private val authRepository: AuthRepository

    // 3. Thêm biến để giữ userId hiện tại (có thể là GUEST hoặc UID Firebase)
    private val currentUserId: String

    private val _menuItems = MediatorLiveData<List<MenuItem>>()
    val menuItems: LiveData<List<MenuItem>> = _menuItems

    private val projectsFromDb: LiveData<List<ProjectEntity>>
    private val staticMenuItems: List<MenuItem>

    init {
        // Khởi tạo DB một lần
        val db = AppDatabase.getDatabase(application)

        // Khởi tạo DAO
        val projectDao = db.projectDao()

        // 4. Khởi tạo cả hai Repository
        // (Giả sử AuthRepository của em có constructor nhận Application)
        authRepository = AuthRepository(application)
        repository = ProjectRepository(projectDao)

        // 5. LẤY userId HIỆN TẠI
        // Đây là mấu chốt: Lấy ID từ AuthRepository
        currentUserId = authRepository.getCurrentUserId()

        // 6. SỬA LỖI BIÊN DỊCH
        // Code cũ (lỗi): projectsFromDb = repository.allProjects
        // Code mới (đúng): Gọi hàm và truyền userId vào
        projectsFromDb = repository.getAllProjects(currentUserId)

        // Phần code còn lại của em để tạo menu tĩnh
        staticMenuItems = listOf(
            MenuItem(id = null, R.drawable.wb_sunny_24px, "Hôm nay", "1h 15m", 5, null),
            MenuItem(id = null, R.drawable.wb_twilight_24px, "Ngày mai", "0m", 0, null),
            MenuItem(id = null, R.drawable.calendar_month_24px, "Tuần này", "1h 15m", 5, null),
            MenuItem(id = null, R.drawable.event_available_24px, "Đã lên kế hoạch", "1h 15m", 5, null),
            MenuItem(id = null, R.drawable.event_24px, "Sự kiện", "0m", 0, null),
            MenuItem(id = null, R.drawable.check_circle_24px, "Đã hoàn thành", "0m", 0, null),
            MenuItem(id = null, R.drawable.task_24px, "Nhiệm vụ", "1h 15m", 5, null)
        )

        _menuItems.addSource(projectsFromDb) { projects ->
            combineLists(projects)
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
            val newOrder = projectsFromDb.value?.size ?: 0

            val newProject = ProjectEntity(
                projectId = UUID.randomUUID().toString(),

                // 7. SỬA LỖI LOGIC
                // Code cũ (lỗi): userId = "default_user",
                // Code mới (đúng): Dùng userId động đã lấy ở 'init'
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

        val projectEntity = projectsFromDb.value?.find { it.projectId == menuItem.id }

        if (projectEntity != null) {
            viewModelScope.launch {
                repository.delete(projectEntity)
            }
        }
    }

    fun updateProject(id: String, newName: String, newColorString: String) {
        viewModelScope.launch {
            val originalProject = projectsFromDb.value?.find { it.projectId == id }
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