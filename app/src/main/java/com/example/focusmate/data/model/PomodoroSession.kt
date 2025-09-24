package com.example.focusmate.data.model

data class PomodoroSession(
    val totalTime: Int,        // Tổng thời gian (giây)
    var remainingTime: Int,    // Thời gian còn lại
    var isRunning: Boolean     // Trạng thái hiện tại
)