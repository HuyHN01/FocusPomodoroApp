package com.example.focusmate.ui.todolist

import android.app.Application
import androidx.lifecycle.*
import com.example.focusmate.data.local.AppDatabase
import com.example.focusmate.data.local.entity.TaskEntity
import com.example.focusmate.data.local.entity.TaskPriority
import com.example.focusmate.data.local.entity.TaskStatus
import com.example.focusmate.data.repository.AuthRepository
import com.example.focusmate.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class TomorrowViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository
    private val authRepository: AuthRepository
    private val currentUserId: String

    // LiveData cho Ngày Mai
    val uncompletedTasks: LiveData<List<TaskEntity>>
    val completedTasks: LiveData<List<TaskEntity>>

    val uncompletedCount = MediatorLiveData<Int>()
    val completedCount = MediatorLiveData<Int>()
    val estimatedTimeFormatted = MediatorLiveData<String>()

    // Quản lý Task đang chọn (để sửa/xóa)
    private val _currentTask = MutableStateFlow<TaskEntity?>(null)
    val currentTask: StateFlow<TaskEntity?> = _currentTask.asStateFlow()

    // Biến tạm cho UI (Priority picker)
    private val _tempSelectedPriority = MutableLiveData<TaskPriority>(TaskPriority.NONE)
    val tempSelectedPriority: LiveData<TaskPriority> = _tempSelectedPriority

    // LẤY LIVE DATA TỪ REPOSITORY ĐÃ TÍNH TOÁN (Đã có sẵn trong Init)
    private val startOfTomorrow: Long
    private val endOfTomorrow: Long

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TaskRepository(db.taskDao())
        authRepository = AuthRepository(application)
        currentUserId = authRepository.getCurrentUserId()

        // --- TÍNH TOÁN NGÀY MAI ---
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1) // Cộng 1 ngày -> Ngày mai

        // 00:00:00 Ngày mai
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        startOfTomorrow = cal.timeInMillis

        // 23:59:59 Ngày mai
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999) // Thêm 999ms để đảm bảo lấy hết giây cuối cùng
        endOfTomorrow = cal.timeInMillis

        // --- GỌI REPOSITORY LẤY ĐÚNG NGÀY MAI ---
        uncompletedTasks = repository.getTasksForDateRange(currentUserId, startOfTomorrow, endOfTomorrow, false)
        completedTasks = repository.getTasksForDateRange(currentUserId, startOfTomorrow, endOfTomorrow, true)

        // Setup bộ đếm
        uncompletedCount.addSource(uncompletedTasks) { uncompletedCount.value = it.size }
        completedCount.addSource(completedTasks) { completedCount.value = it.size }

        // LOGIC TÍNH TỔNG THỜI GIAN POMODORO
        estimatedTimeFormatted.addSource(uncompletedTasks) { tasks ->
            val totalPomodoros = tasks.sumOf { it.estimatedPomodoros }
            val totalMinutes = totalPomodoros * 25
            estimatedTimeFormatted.value = formatMinutesToHHMM(totalMinutes)
        }
    }

    // [GIỮ NGUYÊN] Hàm tiện ích đổi phút sang giờ:phút
    private fun formatMinutesToHHMM(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return String.format("%02d:%02d", hours, minutes)
    }

    // --- HÀM THÊM TASK CHO NGÀY MAI (Đã sửa lỗi dueDate) ---
    fun addNewTask(title: String, estimatedPomodoros: Int, priority: TaskPriority, dueDate: Long? = null,
                   projectId: String? = null) {
        viewModelScope.launch {
            // Tính toán finalDueDate: Nếu Fragment không truyền (dueDate=null) -> Tự động gán là startOfTomorrow
            // Nếu Fragment đã truyền (dueDate != null) -> Giữ nguyên giá trị từ Fragment
            val finalDueDate = dueDate ?: startOfTomorrow

            repository.addTask(
                title = title,
                estimatedPomodoros = estimatedPomodoros,
                userId = currentUserId,
                projectId = projectId, // Dùng projectId từ Fragment
                priority = priority,
                dueDate = finalDueDate
            )
        }
    }

    // --- CÁC HÀM XỬ LÝ LOGIC CƠ BẢN (Giữ nguyên) ---

    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId)
            if (task != null) {
                val newStatus = if (task.status == TaskStatus.PENDING) TaskStatus.COMPLETED else TaskStatus.PENDING

                // [SỬA LẠI] Gọi hàm update status trong Repository (cần đảm bảo Repository có hàm này)
                repository.updateTaskStatus(taskId, newStatus)
            }
        }
    }

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

    // Các hàm update (Giữ nguyên)
    fun updateCurrentTask(newTitle: String, newEstimatedPomodoros: Int, newNote: String?) {
        _currentTask.value?.let { task ->
            val updated = task.copy(title = newTitle, estimatedPomodoros = newEstimatedPomodoros, note = newNote, lastModified = System.currentTimeMillis())
            viewModelScope.launch { repository.updateTask(updated); _currentTask.value = updated }
        }
    }

    fun updateTaskPriority(newPriority: TaskPriority) {
        _currentTask.value?.let { task ->
            val updated = task.copy(priority = newPriority, lastModified = System.currentTimeMillis())
            viewModelScope.launch { repository.updateTask(updated); _currentTask.value = updated }
        }
    }

    fun updateTaskPomodoros(newCount: Int) {
        _currentTask.value?.let { task ->
            val updated = task.copy(estimatedPomodoros = newCount, lastModified = System.currentTimeMillis())
            viewModelScope.launch { repository.updateTask(updated); _currentTask.value = updated }
        }
    }

    fun setTempPriority(priority: TaskPriority) {
        _tempSelectedPriority.value = priority
    }

    fun clearCurrentTask() {
        _currentTask.value = null
    }
}