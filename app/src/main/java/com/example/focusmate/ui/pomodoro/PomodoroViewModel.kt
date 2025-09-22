package com.example.focusmate.ui.pomodoro

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PomodoroViewModel : ViewModel() {

    private var totalTime = 25 * 60   // 25 phút
    private var timer: CountDownTimer? = null

    private val _timeLeft = MutableLiveData(totalTime)
    val timeLeft: LiveData<Int> = _timeLeft

    private val _isRunning = MutableLiveData(false)
    val isRunning: LiveData<Boolean> = _isRunning

    fun startTimer() {
        if (_isRunning.value == true) return

        _isRunning.value = true
        timer = object : CountDownTimer(_timeLeft.value!! * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeLeft.value = (millisUntilFinished / 1000).toInt()
            }
            override fun onFinish() {
                _timeLeft.value = 0
                _isRunning.value = false
            }
        }.start()
    }

    fun pauseTimer() {
        timer?.cancel()
        _isRunning.value = false
    }

    fun resetTimer() {
        timer?.cancel()
        _timeLeft.value = totalTime
        _isRunning.value = false
    }

    fun setDuration(minutes: Int) {
        totalTime = minutes * 60
        resetTimer()
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }
}