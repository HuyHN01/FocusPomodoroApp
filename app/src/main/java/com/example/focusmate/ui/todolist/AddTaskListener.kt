package com.example.focusmate.ui.todolist

import com.example.focusmate.data.local.entity.TaskPriority

interface AddTaskListener {
    fun onTaskAddedFromFragment(
        title: String,
        pomodoros: Int,
        priority: TaskPriority,
        dueDate: Long?,
        projectId: String?
    )
}