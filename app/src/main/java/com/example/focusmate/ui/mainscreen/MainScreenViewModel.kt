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
import com.example.focusmate.data.local.entity.ProjectWithStats
import com.example.focusmate.data.local.entity.TaskEntity
import com.example.focusmate.data.repository.TaskRepository
import java.util.Calendar

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProjectRepository
    private val taskRepository: TaskRepository
    private val authRepository: AuthRepository


    private var currentUserId: String


    private val _menuItems = MediatorLiveData<List<MenuItem>>()
    val menuItems: LiveData<List<MenuItem>> = _menuItems


    private val _currentUserInfo = MutableLiveData<String>()
    val currentUserInfo: LiveData<String> = _currentUserInfo


    private var sourceProjects: LiveData<List<ProjectWithStats>>? = null
    private var sourceTasks: LiveData<List<TaskEntity>>? = null
    private var cachedProjects: List<ProjectWithStats> = emptyList()
    private var cachedTasks: List<TaskEntity> = emptyList()

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
        taskRepository = TaskRepository(db.taskDao())
        
        currentUserId = authRepository.getCurrentUserId()

        
        reloadData()
    }


    fun reloadData() {
        currentUserId = authRepository.getCurrentUserId()
        updateUserInfo()
        repository.syncProjects(currentUserId, viewModelScope)

        sourceProjects?.let { _menuItems.removeSource(it) }
        sourceTasks?.let { _menuItems.removeSource(it) }

        val newProjectSource = repository.getProjectsWithStats(currentUserId)
        val newTaskSource = taskRepository.getAllPendingTasks(currentUserId)

        sourceProjects = newProjectSource
        sourceTasks = newTaskSource

        _menuItems.addSource(newProjectSource) { projects ->
            cachedProjects = projects
            combineDataAndNotify()
        }

        _menuItems.addSource(newTaskSource) { tasks ->
            cachedTasks = tasks
            combineDataAndNotify()
        }
    }

    private fun combineDataAndNotify() {
        val newList = mutableListOf<MenuItem>()
        val cal = Calendar.getInstance()

        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val startToday = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 1)
        val startTomorrow = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 1)
        val startDayAfterTomorrow = cal.timeInMillis

        val todayTasks = cachedTasks.filter { it.dueDate != null && it.dueDate!! < startTomorrow }
        val todayCount = todayTasks.size
        val todayTime = formatMinutesToTime(todayTasks.sumOf { it.estimatedPomodoros } * 25)

        val tomorrowTasks = cachedTasks.filter { it.dueDate != null && it.dueDate!! >= startTomorrow && it.dueDate!! < startDayAfterTomorrow }
        val tomorrowCount = tomorrowTasks.size
        val tomorrowTime = formatMinutesToTime(tomorrowTasks.sumOf { it.estimatedPomodoros } * 25)

        cal.timeInMillis = startToday
        cal.add(Calendar.DAY_OF_YEAR, 7)
        val endOfWeek = cal.timeInMillis
        val weekTasks = cachedTasks.filter { it.dueDate != null && it.dueDate!! >= startToday && it.dueDate!! < endOfWeek }
        val weekCount = weekTasks.size
        val weekTime = formatMinutesToTime(weekTasks.sumOf { it.estimatedPomodoros } * 25)

        newList.add(MenuItem(id = null, R.drawable.wb_sunny_24px, "Hôm nay", todayTime, todayCount, null))
        newList.add(MenuItem(id = null, R.drawable.wb_twilight_24px, "Ngày mai", tomorrowTime, tomorrowCount, null))
        newList.add(MenuItem(id = null, R.drawable.calendar_month_24px, "Tuần này", weekTime, weekCount, null))

        newList.add(MenuItem(id = null, R.drawable.event_available_24px, "Đã lên kế hoạch", "0m", 0, null))
        newList.add(MenuItem(id = null, R.drawable.event_24px, "Sự kiện", "0m", 0, null))
        newList.add(MenuItem(id = null, R.drawable.check_circle_24px, "Đã hoàn thành", "0m", 0, null))

        val inboxTasks = cachedTasks.filter { it.dueDate == null }
        val inboxCount = inboxTasks.size
        val inboxTime = formatMinutesToTime(inboxTasks.sumOf { it.estimatedPomodoros } * 25)
        newList.add(MenuItem(id = null, R.drawable.task_24px, "Nhiệm vụ", inboxTime, inboxCount, null))

        cachedProjects.forEach { item ->
            val totalMinutes = item.totalPomodoros * 25
            val timeString = formatMinutesToTime(totalMinutes)

            newList.add(
                MenuItem(
                    id = item.project.projectId,
                    iconRes = R.drawable.ic_circle,
                    title = item.project.name,
                    focusedTime = timeString,
                    taskCount = item.taskCount,
                    colorString = item.project.color
                )
            )
        }

        newList.add(
            MenuItem(id = null, R.drawable.outline_add_24, "Thêm Dự Án", "", -1, null)
        )

        _menuItems.value = newList
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

    private fun combineLists(projectStatsList: List<ProjectWithStats>?) {
        val newList = mutableListOf<MenuItem>()
        newList.addAll(staticMenuItems)

        projectStatsList?.forEach { item ->
            val totalMinutes = item.totalPomodoros * 25
            val timeString = formatMinutesToTime(totalMinutes)

            newList.add(
                MenuItem(
                    id = item.project.projectId,
                    iconRes = R.drawable.ic_circle,
                    title = item.project.name,
                    focusedTime = timeString,
                    taskCount = item.taskCount,
                    colorString = item.project.color
                )
            )
        }

        newList.add(
            MenuItem(id = null, iconRes = R.drawable.outline_add_24, title = "Thêm Dự Án", focusedTime = "", taskCount = -1, null)
        )

        _menuItems.value = newList
    }
    private fun formatMinutesToTime(totalMinutes: Int): String {
        if (totalMinutes == 0) return "0m"
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    fun addProject(projectName: String, colorString: String) {
        viewModelScope.launch {
            val newOrder = cachedProjects.size

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

        val itemToDelete = cachedProjects.find { it.project.projectId == menuItem.id }
        if (itemToDelete != null) {
            viewModelScope.launch {
                repository.delete(itemToDelete.project)
            }
        }
    }

    fun updateProject(id: String, newName: String, newColorString: String) {
        viewModelScope.launch {
            val originalItem = cachedProjects.find { it.project.projectId == id }

            if (originalItem != null) {
                val updatedProject = originalItem.project.copy(
                    name = newName,
                    color = newColorString,
                    lastModified = System.currentTimeMillis()
                )
                repository.update(updatedProject)
            }
        }
    }
}