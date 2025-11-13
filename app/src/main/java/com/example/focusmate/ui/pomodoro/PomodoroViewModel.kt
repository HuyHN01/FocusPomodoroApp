package com.example.focusmate.ui.pomodoro

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.focusmate.data.repository.PomodoroRepository
import com.example.focusmate.util.SoundEvent

class PomodoroViewModel : ViewModel() {
    val timeLeft: LiveData<Int> = PomodoroRepository.timeLeft
    val sessionTotal: LiveData<Int> = PomodoroRepository.sessionTotal
    val state: LiveData<TimerState> = PomodoroRepository.state
    val soundEvent: LiveData<SoundEvent?> = PomodoroRepository.soundEvent
    val focusSoundId: LiveData<Int> = PomodoroRepository.focusSoundId
    val focusSoundVolume: LiveData<Float> = PomodoroRepository.focusSoundVolume

    fun startTimer() = PomodoroRepository.startTimer()
    fun startBreak() = PomodoroRepository.startBreak()
    fun skipBreak() = PomodoroRepository.skipBreak()
    fun addOneMinute() = PomodoroRepository.addOneMinute()
    fun pauseTimer() = PomodoroRepository.pauseTimer()
    fun resumeTimer() = PomodoroRepository.startTimer()
    fun resetTimer() = PomodoroRepository.resetTimer()
    fun setCustomTime(seconds: Int) = PomodoroRepository.setCustomTime(seconds)
    fun resetSoundEvent() = PomodoroRepository.resetSoundEvent()
    fun setFocusSound(soundId: Int, volume: Float) = PomodoroRepository.setFocusSound(soundId, volume)


}
