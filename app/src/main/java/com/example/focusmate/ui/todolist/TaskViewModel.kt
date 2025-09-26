package com.example.focusmate.ui.todolist

import androidx.lifecycle.ViewModel
import com.example.focusmate.data.repository.TaskRepository

class TaskViewModel : ViewModel() {

    val tasks = TaskRepository.tasks

    fun addNewTask(title: String, pomodoroCount: Int) {
        TaskRepository.addTask(title, pomodoroCount)
    }
    fun toggleTaskCompletion(taskId: String) {
        TaskRepository.toggleTaskCompletion(taskId)
    }

    fun completeTask(taskId: String) {
        TaskRepository.completeTask(taskId)
    }

    fun deleteTask(taskId: String) {
        TaskRepository.deleteTask(taskId)
    }
}