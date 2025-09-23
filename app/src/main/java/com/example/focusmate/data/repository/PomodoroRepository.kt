package com.example.focusmate.data.repository

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.focusmate.ui.pomodoro.TimerState

object PomodoroRepository {

    private const val DEFAULT_TIME = 25 * 60 // 25 phút

    private var totalTime = DEFAULT_TIME
    private var currentTime = DEFAULT_TIME

    private var timer: CountDownTimer? = null

    private val _timeLeft = MutableLiveData<Int>(DEFAULT_TIME)
    val timeLeft: LiveData<Int> get() = _timeLeft

    private val _state = MutableLiveData<TimerState>(TimerState.IDLE)
    val state: LiveData<TimerState> get() = _state

    fun startTimer() {
        if (_state.value == TimerState.RUNNING) return

        _state.value = TimerState.RUNNING

        timer = object : CountDownTimer(currentTime * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                currentTime = (millisUntilFinished / 1000).toInt()
                _timeLeft.postValue(currentTime)
            }

            override fun onFinish() {
                currentTime = 0
                _timeLeft.postValue(currentTime)
                _state.postValue(TimerState.IDLE)
                resetTimer()
            }
        }.start()
    }

    fun addOneMinute() {
        if (_state.value == TimerState.RUNNING) {
            timer?.cancel()
            currentTime += 60
            totalTime += 60
            _timeLeft.value = currentTime
            startTimer()
        }
    }

    fun pauseTimer() {
        timer?.cancel()
        _state.value = TimerState.PAUSED
    }

    fun resetTimer() {
        timer?.cancel()
        currentTime = totalTime
        _timeLeft.value = currentTime
        _state.value = TimerState.IDLE
    }

    fun resumeTimer() {
        startTimer()
    }
    fun setCustomTime(seconds: Int) {
        timer?.cancel()
        totalTime = seconds
        currentTime = seconds
        _timeLeft.value = currentTime
        _state.value = TimerState.IDLE
    }
}
