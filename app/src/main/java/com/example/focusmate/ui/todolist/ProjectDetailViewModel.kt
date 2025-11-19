package com.example.focusmate.ui.todolist

import android.app.Application
import androidx.lifecycle.*
import com.example.focusmate.data.local.AppDatabase
import com.example.focusmate.data.local.entity.TaskEntity
import com.example.focusmate.data.local.entity.TaskPriority
import com.example.focusmate.data.local.entity.TaskStatus
import com.example.focusmate.data.repository.AuthRepository
import com.example.focusmate.data.repository.TaskRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.switchMap

class ProjectDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository
    private val authRepository: AuthRepository
    private val currentUserId: String

    
    private val _currentProjectId = MutableLiveData<String>()

    
    val uncompletedTasks: LiveData<List<TaskEntity>> = _currentProjectId.switchMap { projectId ->
        repository.getUncompletedTasksByProject(currentUserId, projectId)
    }

    val completedTasks: LiveData<List<TaskEntity>> = _currentProjectId.switchMap { projectId ->
        repository.getCompletedTasksByProject(currentUserId, projectId)
    }

    val uncompletedCount = MediatorLiveData<Int>()
    val completedCount = MediatorLiveData<Int>()
    val estimatedTimeFormatted = MediatorLiveData<String>()

    
    private val _tempSelectedPriority = MutableLiveData<TaskPriority>(TaskPriority.NONE)
    val tempSelectedPriority: LiveData<TaskPriority> = _tempSelectedPriority

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TaskRepository(db.taskDao())
        authRepository = AuthRepository(application)
        currentUserId = authRepository.getCurrentUserId()

        
        uncompletedCount.addSource(uncompletedTasks) { uncompletedCount.value = it.size }
        completedCount.addSource(completedTasks) { completedCount.value = it.size }

        
        estimatedTimeFormatted.addSource(uncompletedTasks) { tasks ->
            val totalMinutes = tasks.sumOf { it.estimatedPomodoros } * 25
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            estimatedTimeFormatted.value = String.format("%02d:%02d", hours, minutes)
        }
    }

    
    fun setProjectId(id: String) {
        _currentProjectId.value = id
    }

    
    fun addNewTask(title: String, estimatedPomodoros: Int, priority: TaskPriority, dueDate: Long? = null) {
        val projectId = _currentProjectId.value ?: return 

        viewModelScope.launch {
            repository.addTask(
                title = title,
                estimatedPomodoros = estimatedPomodoros,
                userId = currentUserId,
                projectId = projectId, 
                priority = priority,
                dueDate = dueDate
            )
        }
    }

    
    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId)
            if (task != null) {
                val newStatus = if (task.status == TaskStatus.PENDING) TaskStatus.COMPLETED else TaskStatus.PENDING
                repository.updateTaskStatus(taskId, newStatus)
            }
        }
    }

    fun setTempPriority(priority: TaskPriority) {
        _tempSelectedPriority.value = priority
    }
}