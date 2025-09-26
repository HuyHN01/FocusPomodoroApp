
package com.example.focusmate.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.focusmate.data.model.Task
import java.util.UUID

object TaskRepository {

    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> get() = _tasks

    private val taskList = mutableListOf<Task>()

    init {
        // Khởi tạo một vài task mẫu
        taskList.add(Task(UUID.randomUUID().toString(), "Sau khi bắt đầu đếm thời gian, hãy kiên quyết...", 1))
        taskList.add(Task(UUID.randomUUID().toString(), "Nhấn Nút Tròn bên trái để hoàn thành công việc đó", 1))
        _tasks.value = taskList
    }

    fun addTask(title: String, pomodoroCount: Int) {
        val newTask = Task(UUID.randomUUID().toString(), title, pomodoroCount)
        taskList.add(newTask)
        _tasks.value = taskList // Cập nhật LiveData để thông báo cho ViewModel/View
    }

    fun completeTask(taskId: String) {
        val taskToComplete = taskList.find { it.id == taskId }
        taskToComplete?.let {
            it.isCompleted = true
            _tasks.value = taskList // Cập nhật LiveData
        }
    }
    fun toggleTaskCompletion(taskId: String) {
        val taskToUpdate = taskList.find { it.id == taskId }
        taskToUpdate?.let {
            // Đảo ngược giá trị của isCompleted
            it.isCompleted = !it.isCompleted
            _tasks.value = taskList // Cập nhật LiveData để thông báo cho UI
        }
    }
    fun deleteTask(taskId: String) {
        taskList.removeIf { it.id == taskId }
        _tasks.value = taskList // Cập nhật LiveData
    }
}