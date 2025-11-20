package com.example.focusmate.ui.todolist

import android.app.Application
import androidx.lifecycle.*
import com.example.focusmate.data.local.AppDatabase
import com.example.focusmate.data.local.entity.ProjectEntity
import com.example.focusmate.data.local.entity.TaskEntity
import com.example.focusmate.data.local.entity.TaskPriority
import com.example.focusmate.data.local.entity.TaskStatus
import com.example.focusmate.data.repository.TaskRepository
import com.example.focusmate.data.repository.AuthRepository
import com.example.focusmate.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    

    private val _tempSelectedProject = MutableLiveData<ProjectEntity?>(null)
    val tempSelectedProject: LiveData<ProjectEntity?> = _tempSelectedProject


    private val _tempSelectedPriority = MutableLiveData<TaskPriority>(TaskPriority.NONE)
    val tempSelectedPriority: LiveData<TaskPriority> = _tempSelectedPriority
    private val _currentTask = MutableStateFlow<TaskEntity?>(null)
    val currentTask: StateFlow<TaskEntity?> = _currentTask.asStateFlow()

    
    private val repository: TaskRepository
    private val authRepository: AuthRepository
    private val currentUserId: String
    val allTasks: LiveData<List<TaskEntity>>

    
    val uncompletedTasks: LiveData<List<TaskEntity>> 
    val completedTasks: LiveData<List<TaskEntity>>
    val timeElapsedFormatted = MediatorLiveData<String>()
    
    val uncompletedCount = MediatorLiveData<Int>()
    val completedCount = MediatorLiveData<Int>()
    val estimatedTimeFormatted = MediatorLiveData<String>()
    private val projectRepository: ProjectRepository
    val allProjects: LiveData<List<ProjectEntity>>

    init {
        
        val db = AppDatabase.getDatabase(application)
        val taskDao = db.taskDao()
        val projectDao = db.projectDao()
        projectRepository = ProjectRepository(projectDao)
        repository = TaskRepository(taskDao)


        authRepository = AuthRepository(application)
        currentUserId = authRepository.getCurrentUserId()
        allProjects = projectRepository.getAllProjects(currentUserId)
        
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis 

        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfDay = calendar.timeInMillis 
        
        uncompletedTasks = repository.getUncompletedTasks(currentUserId, endOfDay)
        completedTasks = repository.getTasksCompletedToday(currentUserId, startOfDay, endOfDay)
        repository.syncTasks(currentUserId, viewModelScope)

        
        allTasks = repository.getAllTasks(currentUserId)
        
        uncompletedCount.addSource(uncompletedTasks) { list ->
            uncompletedCount.value = list.size
        }
        completedCount.addSource(completedTasks) { list ->
            completedCount.value = list.size
        }
        estimatedTimeFormatted.addSource(uncompletedTasks) { tasks ->
            val totalPomodoros = tasks.sumOf { it.estimatedPomodoros }
            val totalMinutes = totalPomodoros * 25
            estimatedTimeFormatted.value = formatMinutesToHHMM(totalMinutes)
        }

        timeElapsedFormatted.addSource(completedTasks) { tasksCompletedToday ->
            
            val totalCompletedPomodoros = tasksCompletedToday.sumOf { it.completedPomodoros }

            
            val totalMinutes = totalCompletedPomodoros * 25 

            
            timeElapsedFormatted.value = formatMinutesToHHMM(totalMinutes)
        }

    }

    fun setTempProject(project: ProjectEntity?) {
        _tempSelectedProject.value = project
    }
    private fun formatMinutesToHHMM(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return String.format("%02d:%02d", hours, minutes)
    }

    

    fun addNewTask(
        title: String,
        estimatedPomodoros: Int,
        priority: TaskPriority,
        dueDate:Long?,
        projectId: String?
    ) {
        viewModelScope.launch {
            repository.addTask(
                title = title,
                estimatedPomodoros = estimatedPomodoros,
                userId = currentUserId,
                projectId = projectId,
                priority = priority,
                dueDate = dueDate,

            )
        }
    }

    
    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId) 
            if (task != null) {
                val newStatus = if (task.status == TaskStatus.PENDING) {
                    TaskStatus.COMPLETED
                } else {
                    TaskStatus.PENDING
                }
                repository.updateTaskStatus(taskId, newStatus) 
                val completedAtTime = if (newStatus == TaskStatus.COMPLETED) System.currentTimeMillis() else null
                val updatedTask = task.copy(
                    status = newStatus,
                    completedAt = completedAtTime
                )

                
                _currentTask.value = updatedTask
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

    fun clearCurrentTask() { _currentTask.value = null }
    fun setTempPriority(priority: TaskPriority) { _tempSelectedPriority.value = priority }

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
        _currentTask.value?.let { currentTask ->
            val updatedTask = currentTask.copy(
                estimatedPomodoros = newCount,
                lastModified = System.currentTimeMillis()
            )
            viewModelScope.launch {
                repository.updateTask(updatedTask)
                _currentTask.value = updatedTask
            }
        }
    }

    fun updateTaskDueDate(newDueDate: Long?) {
        _currentTask.value?.let { currentTask ->
            val updatedTask = currentTask.copy(
                dueDate = newDueDate,
                lastModified = System.currentTimeMillis()
            )
            viewModelScope.launch {
                repository.updateTask(updatedTask)
                _currentTask.value = updatedTask
            }
        }
    }
    fun incrementCompletedPomodoros(taskId: String) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId)
            if (task != null) {
                
                val updatedTask = task.copy(
                    completedPomodoros = task.completedPomodoros + 1
                )
                
                repository.updateTask(updatedTask)
            }
        }
    }

    fun addNewProject(projectName: String) {
        viewModelScope.launch {
            val newProject = ProjectEntity(
                
                userId = currentUserId, 
                name = projectName,
                color = "#FFFFFF" 
                
            )
            projectRepository.insert(newProject)
        }
    }
    fun updateTaskProject(newProjectId: String?) {
        _currentTask.value?.let { currentTask ->
            
            val updatedTask = currentTask.copy(
                projectId = newProjectId,
                lastModified = System.currentTimeMillis()
            )

            
            viewModelScope.launch {
                repository.updateTask(updatedTask)
                _currentTask.value = updatedTask
            }
        }
    }
    fun startAddTask() {
        _tempSelectedProject.value = null
        _tempSelectedPriority.value = TaskPriority.NONE
    }
}