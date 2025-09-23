package com.example.focusmate.ui.pomodoro

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.focusmate.data.repository.PomodoroRepository

enum class TimerState {
    IDLE, RUNNING, PAUSED
}

class PomodoroViewModel : ViewModel() {
    val timeLeft: LiveData<Int> = PomodoroRepository.timeLeft
    val state: LiveData<TimerState> = PomodoroRepository.state

    fun startTimer() = PomodoroRepository.startTimer()
    fun addOneMinute() = PomodoroRepository.addOneMinute()
    fun pauseTimer() = PomodoroRepository.pauseTimer()
    fun resumeTimer() = PomodoroRepository.resumeTimer()
    fun resetTimer() = PomodoroRepository.resetTimer()
    fun setCustomTime(seconds: Int) = PomodoroRepository.setCustomTime(seconds)
}
