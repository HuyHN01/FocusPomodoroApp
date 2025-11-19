package com.example.focusmate.data.model

data class PomodoroSession(
    val totalTime: Int,        
    var remainingTime: Int,    
    var isRunning: Boolean     
)