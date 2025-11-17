package com.example.focusmate.ui.todolist

import android.app.Application
import androidx.lifecycle.*
import com.example.focusmate.data.local.AppDatabase
import com.example.focusmate.data.local.entity.TaskEntity
import com.example.focusmate.data.local.entity.TaskPriority
import com.example.focusmate.data.local.entity.TaskStatus
import com.example.focusmate.data.repository.TaskRepository

// SỬA LỖI 1: Import AuthRepository để lấy userId
import com.example.focusmate.data.repository.AuthRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val _tempSelectedPriority = MutableLiveData<TaskPriority>(TaskPriority.NONE)
    val tempSelectedPriority: LiveData<TaskPriority> = _tempSelectedPriority

    private val repository: TaskRepository

    // SỬA LỖI 2: Thêm AuthRepository và currentUserId
    private val authRepository: AuthRepository
    private val currentUserId: String

    val uncompletedTasks: LiveData<List<TaskEntity>>
    val completedTasks: LiveData<List<TaskEntity>>

    val uncompletedCount = MediatorLiveData<Int>()
    val completedCount = MediatorLiveData<Int>()
    private val _currentTask = MutableStateFlow<TaskEntity?>(null)
    val currentTask: StateFlow<TaskEntity?> = _currentTask.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application) // Khởi tạo DB 1 lần
        val taskDao = db.taskDao()
        repository = TaskRepository(taskDao)

        // SỬA LỖI 3: Khởi tạo AuthRepo và lấy userId hiện tại
        authRepository = AuthRepository(application)
        currentUserId = authRepository.getCurrentUserId()

        // SỬA LỖI 4: 'Unresolved reference'
        // Gọi hàm từ Repository và truyền userId vào
        uncompletedTasks = repository.getUncompletedTasks(currentUserId)
        completedTasks = repository.getCompletedTasks(currentUserId)

        repository.syncTasks(currentUserId, viewModelScope)

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

            // SỬA LỖI 5: Lỗi Logic (Hard-coded userId)
            // Code cũ: val tempUserId = "user_123"
            // Dùng userId thật
            repository.addTask(
                title = title,
                estimatedPomodoros = estimatedPomodoros,
                userId = currentUserId, // Dùng userId thật
                projectId = null,
                priority = priority,
                dueDate = dueDate
            )
        }
    }

    // SỬA LỖI 6: 'Argument type mismatch'
    // Đổi tham số từ taskId: Int sang taskId: String
    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            // Lấy task hiện tại từ DB (giờ đã dùng String)
            val task = repository.getTaskById(taskId)
            if (task != null) {
                // Quyết định trạng thái mới
                val newStatus = if (task.status == TaskStatus.PENDING) {
                    TaskStatus.COMPLETED
                } else {
                    TaskStatus.PENDING
                }
                // Cập nhật (giờ đã dùng String)
                repository.updateTaskStatus(taskId, newStatus)
            }
        }
    }

    // SỬA LỖI 7: 'Argument type mismatch'
    // Đổi tham số từ taskId: Int sang taskId: String
    fun loadTaskById(taskId: String) {
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
                estimatedPomodoros = newEstimatedPomodoros,
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
}