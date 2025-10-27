package com.example.focusmate.ui.pomodoro

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.focusmate.data.repository.PomodoroRepository
import com.example.focusmate.util.SoundEvent

enum class TimerState {
    IDLE, RUNNING, PAUSED,
    BREAK_READY, BREAK_RUNNING, BREAK_PAUSED
}

class PomodoroViewModel : ViewModel() {
    val timeLeft: LiveData<Int> = PomodoroRepository.timeLeft
    val sessionTotal: LiveData<Int> = PomodoroRepository.sessionTotal
    val state: LiveData<TimerState> = PomodoroRepository.state
    val soundEvent: LiveData<SoundEvent?> = PomodoroRepository.soundEvent
    fun startTimer() = PomodoroRepository.startTimer()
    fun startBreak() = PomodoroRepository.startBreak()
    fun skipBreak() = PomodoroRepository.skipBreak()
    fun addOneMinute() = PomodoroRepository.addOneMinute()
    fun pauseTimer() = PomodoroRepository.pauseTimer()
    fun resumeTimer() = PomodoroRepository.startTimer()
    fun resetTimer() = PomodoroRepository.resetTimer()
    fun setCustomTime(seconds: Int) = PomodoroRepository.setCustomTime(seconds)
    fun resetSoundEvent() = PomodoroRepository.resetSoundEvent()
}
