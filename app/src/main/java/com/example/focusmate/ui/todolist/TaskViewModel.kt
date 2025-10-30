package com.example.focusmate.ui.todolist

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.example.focusmate.data.model.Task
import com.example.focusmate.data.repository.TaskRepository

class TaskViewModel : ViewModel() {

    val tasks = TaskRepository.tasks

    val completedCount: LiveData<Int> = TaskRepository.tasks.map { tasks ->
        tasks.count { it.isCompleted }
    }
    val uncompletedCount: LiveData<Int> = TaskRepository.tasks.map { tasks ->
        tasks.count { !it.isCompleted }
    }

    // Danh sách chưa xong
    val uncompletedTasks: LiveData<List<Task>> = tasks.map { list ->
        list.filter { !it.isCompleted }
    }

    // Danh sách đã xong
    val completedTasks: LiveData<List<Task>> = tasks.map { list ->
        list.filter { it.isCompleted }
    }
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