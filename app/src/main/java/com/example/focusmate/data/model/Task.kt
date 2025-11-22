package com.example.focusmate.data.model

data class Task(
    val id: Int,
    val title: String,
    val pomodoroCount: Int,
    var isCompleted: Boolean = false
)