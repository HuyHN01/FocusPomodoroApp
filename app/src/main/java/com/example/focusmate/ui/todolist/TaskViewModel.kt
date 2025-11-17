package com.example.focusmate.ui.todolist

import android.app.Application
import androidx.lifecycle.*
import com.example.focusmate.data.local.AppDatabase
import com.example.focusmate.data.local.entity.TaskEntity
import com.example.focusmate.data.local.entity.TaskPriority
import com.example.focusmate.data.local.entity.TaskStatus // Import mới
import com.example.focusmate.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val _tempSelectedPriority = MutableLiveData<TaskPriority>(TaskPriority.NONE)
    val tempSelectedPriority: LiveData<TaskPriority> = _tempSelectedPriority
    private val repository: TaskRepository

    val uncompletedTasks: LiveData<List<TaskEntity>>
    val completedTasks: LiveData<List<TaskEntity>>

    val uncompletedCount = MediatorLiveData<Int>()
    val completedCount = MediatorLiveData<Int>()
    private val _currentTask = MutableStateFlow<TaskEntity?>(null)
    val currentTask: StateFlow<TaskEntity?> = _currentTask.asStateFlow()

    init {
        val taskDao = AppDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(taskDao)


        uncompletedTasks = repository.uncompletedTasks
        completedTasks = repository.completedTasks

        // Đếm số lượng
        uncompletedCount.addSource(uncompletedTasks) { list ->
            uncompletedCount.value = list.size
        }
        completedCount.addSource(completedTasks) { list ->
            completedCount.value = list.size
        }
    }

    fun addNewTask(
        title: String,
        estimatedPomodoros: Int,
        priority: TaskPriority,
        dueDate:Long?
    ) {
        viewModelScope.launch {

            val tempUserId = "user_123"

            repository.addTask(
                title = title,
                estimatedPomodoros = estimatedPomodoros,
                userId = tempUserId,
                projectId = null,
                priority = priority,
                dueDate = dueDate
            )
        }
    }

    fun toggleTaskCompletion(taskId: Int) {
        viewModelScope.launch {
            // Lấy task hiện tại từ DB
            val task = repository.getTaskById(taskId)
            if (task != null) {
                // Quyết định trạng thái mới
                val newStatus = if (task.status == TaskStatus.PENDING) {
                    TaskStatus.COMPLETED
                } else {
                    TaskStatus.PENDING
                }
                repository.updateTaskStatus(taskId, newStatus)
            }
        }
    }

    fun loadTaskById(taskId: Int) {
        viewModelScope.launch {
            _currentTask.value = repository.getTaskById(taskId)
        }
    }

    fun deleteTask() {
        _currentTask.value?.let { taskToDelete ->
            viewModelScope.launch {
                repository.deleteTask(taskToDelete)
                _currentTask.value = null
            }
        }
    }

    fun updateCurrentTask(
        newTitle: String,
        newEstimatedPomodoros: Int,
        newNote: String?

    ) {

        _currentTask.value?.let { taskToUpdate ->
            val updatedTask = taskToUpdate.copy(
                title = newTitle,
                estimatedPomodoros = newEstimatedPomodoros, // Sửa 'pomodoroCount'
                note = newNote,
                lastModified = System.currentTimeMillis()
            )
            viewModelScope.launch {
                repository.updateTask(updatedTask)
                _currentTask.value = updatedTask
            }
        }
    }

    fun clearCurrentTask() {
        _currentTask.value = null
    }
    fun setTempPriority(priority: TaskPriority) {
        _tempSelectedPriority.value = priority
    }

    fun updateTaskPriority(newPriority: TaskPriority) {
        _currentTask.value?.let { currentTask ->
            val updatedTask = currentTask.copy(
                priority = newPriority,
                lastModified = System.currentTimeMillis()
            )
            viewModelScope.launch {
                repository.updateTask(updatedTask)
                _currentTask.value = updatedTask
            }
        }
    }
    fun updateTaskPomodoros(newCount: Int) {
        // 1. Chỉ chạy nếu _currentTask đã được tải
        _currentTask.value?.let { currentTask ->

            // 2. Tạo một bản sao Task với số Pomo mới
            val updatedTask = currentTask.copy(
                estimatedPomodoros = newCount,
                lastModified = System.currentTimeMillis() // Cập nhật thời gian
            )

            // 3. Lưu vào DB và cập nhật lại giao diện
            viewModelScope.launch {
                repository.updateTask(updatedTask)
                _currentTask.value = updatedTask // Cập nhật StateFlow
            }
        }
    }

    fun updateTaskDueDate(newDueDate: Long?) {
        _currentTask.value?.let { currentTask ->
            // 1. Tạo bản sao Task với dueDate mới
            val updatedTask = currentTask.copy(
                dueDate = newDueDate,
                lastModified = System.currentTimeMillis()
            )

            // 2. Lưu vào DB và cập nhật UI
            viewModelScope.launch {
                repository.updateTask(updatedTask)
                _currentTask.value = updatedTask
            }
        }
    }
}