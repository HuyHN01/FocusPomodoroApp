package com.example.focusmate.ui.pomodoro

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

enum class TimerState {
    IDLE, RUNNING, PAUSED
}

class PomodoroViewModel : ViewModel() {

    private var totalTime = 25 * 60
    private var timer: CountDownTimer? = null

    private val _timeLeft = MutableLiveData(totalTime)
    val timeLeft: LiveData<Int> = _timeLeft

    private val _state = MutableLiveData(TimerState.IDLE)
    val state: LiveData<TimerState> = _state

    fun startTimer() {
        if (_state.value == TimerState.RUNNING) return

        _state.value = TimerState.RUNNING
        timer = object : CountDownTimer(_timeLeft.value!! * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeLeft.value = (millisUntilFinished / 1000).toInt()
            }
            override fun onFinish() {
                _timeLeft.value = 0
                _state.value = TimerState.IDLE
            }
        }.start()
    }

    fun pauseTimer() {
        timer?.cancel()
        _state.value = TimerState.PAUSED
    }

    fun resumeTimer() {
        startTimer()
    }

    fun stopTimer() {
        timer?.cancel()
        _timeLeft.value = totalTime
        _state.value = TimerState.IDLE
    }

    fun setDuration(minutes: Int) {
        totalTime = minutes * 60
        stopTimer()
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }
}
