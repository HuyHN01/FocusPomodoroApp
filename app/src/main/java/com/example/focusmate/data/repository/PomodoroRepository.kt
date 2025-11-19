package com.example.focusmate.data.repository

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.focusmate.ui.pomodoro.TimerState
import com.example.focusmate.util.SoundEvent

object PomodoroRepository {

    private const val DEFAULT_POMODORO = 25
    private const val SHORT_BREAK = 5
    private const val LONG_BREAK = 15
    private const val POMODOROS_BEFORE_LONG_BREAK = 4

    private var sessionTotalTime = DEFAULT_POMODORO
    private var currentTime = DEFAULT_POMODORO

    private var timer: CountDownTimer? = null
    private var pomodoroCount = 0

    private val _timeLeft = MutableLiveData<Int>(currentTime)
    val timeLeft: LiveData<Int> get() = _timeLeft

    private val _sessionTotal = MutableLiveData<Int>(sessionTotalTime)
    val sessionTotal: LiveData<Int> get() = _sessionTotal

    private val _state = MutableLiveData<TimerState>(TimerState.IDLE)
    val state: LiveData<TimerState> get() = _state

    private val _soundEvent = MutableLiveData<SoundEvent?>()
    val soundEvent: LiveData<SoundEvent?> = _soundEvent

    private val _focusSoundId = MutableLiveData<Int>(0)
    val focusSoundId: LiveData<Int> = _focusSoundId

    private val _focusSoundVolume = MutableLiveData<Float>(0.7f)
    val focusSoundVolume: LiveData<Float> = _focusSoundVolume

    var currentTaskId: String? = null

    fun startTimer() {
        when (_state.value) {
            TimerState.RUNNING, TimerState.BREAK_RUNNING -> {
                return
            }
            TimerState.BREAK_READY -> {
                startBreak()
            }
            TimerState.BREAK_PAUSED -> {
                resumeBreak()
            }
            TimerState.PAUSED -> {
                resumePomodoro()
            }
            TimerState.IDLE -> {
                startPomodoro()
            }
            else -> startPomodoro()
        }
    }

    private fun startPomodoro() {
        timer?.cancel()
        sessionTotalTime = DEFAULT_POMODORO
        currentTime = sessionTotalTime //=DEFAULT_POMODORO = 25
        _sessionTotal.value = sessionTotalTime // = 25
        _timeLeft.value = currentTime //= 25
        _state.value = TimerState.RUNNING //Khi Pomodoro chạy -> trạng thái = Running
        startCountDown(isBreak = false)

        _soundEvent.value = SoundEvent.START_FOCUS
    }

    private fun resumePomodoro() {
        if (_state.value == TimerState.PAUSED) {
            _state.value = TimerState.RUNNING
            _sessionTotal.value = sessionTotalTime
            startCountDown(isBreak = false)
        }
    }

    private fun onPomodoroFinished() {
        pomodoroCount++
        val nextBreak = if (pomodoroCount % POMODOROS_BEFORE_LONG_BREAK == 0) LONG_BREAK else SHORT_BREAK
        sessionTotalTime = nextBreak
        currentTime = nextBreak
        _sessionTotal.postValue(sessionTotalTime)
        _timeLeft.postValue(currentTime)
        _state.postValue(TimerState.BREAK_READY)

        _soundEvent.value = SoundEvent.END_FOCUS
    }

    fun startBreak() {
        if (_state.value == TimerState.BREAK_READY || _state.value == TimerState.BREAK_PAUSED) {
            _state.value = TimerState.BREAK_RUNNING
            _sessionTotal.value = sessionTotalTime
            startCountDown(isBreak = true)
        }

        _soundEvent.value = SoundEvent.START_BREAK
    }

    private fun resumeBreak() {
        if (_state.value == TimerState.BREAK_PAUSED) {
            _state.value = TimerState.BREAK_RUNNING
            _sessionTotal.value = sessionTotalTime
            startCountDown(isBreak = true)
        }
    }

    fun skipBreak() {
        timer?.cancel()
        sessionTotalTime = DEFAULT_POMODORO
        currentTime = sessionTotalTime
        _sessionTotal.value = sessionTotalTime
        _timeLeft.value = currentTime
        _state.value = TimerState.IDLE
    }

    private fun onBreakFinished() {
        sessionTotalTime = DEFAULT_POMODORO
        currentTime = sessionTotalTime
        _sessionTotal.postValue(sessionTotalTime)
        _timeLeft.postValue(currentTime)
        _state.postValue(TimerState.IDLE)
        _soundEvent.value = SoundEvent.END_BREAK
    }

    fun pauseTimer() {
        timer?.cancel()
        when (_state.value) {
            TimerState.RUNNING -> _state.value = TimerState.PAUSED
            TimerState.BREAK_RUNNING -> _state.value = TimerState.BREAK_PAUSED
            else -> { /* nothing */ }
        }
    }

    fun resetTimer() {
        timer?.cancel()
        sessionTotalTime = DEFAULT_POMODORO
        currentTime = sessionTotalTime
        _sessionTotal.value = sessionTotalTime
        _timeLeft.value = currentTime
        _state.value = TimerState.IDLE
    }

    fun addOneMinute() {
        if (_state.value == TimerState.RUNNING || _state.value == TimerState.BREAK_RUNNING) {
            timer?.cancel()
            currentTime += 60
            sessionTotalTime += 60
            _timeLeft.value = currentTime
            _sessionTotal.value = sessionTotalTime
            val isBreak = _state.value == TimerState.BREAK_RUNNING
            startCountDown(isBreak)
        }
    }

    private fun startCountDown(isBreak: Boolean) {
        timer?.cancel()
        timer = object : CountDownTimer(currentTime * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                currentTime = (millisUntilFinished / 1000).toInt()
                _timeLeft.postValue(currentTime)
            }

            override fun onFinish() {
                // ensure 0 is posted
                currentTime = 0
                _timeLeft.postValue(0)
                if (isBreak) {
                    onBreakFinished()
                } else {
                    onPomodoroFinished()
                }
            }
        }.start()
    }

    fun setCustomTime(seconds: Int) {
        timer?.cancel()
        sessionTotalTime = seconds
        currentTime = seconds
        _sessionTotal.value = sessionTotalTime
        _timeLeft.value = currentTime
        _state.value = TimerState.IDLE
    }

    fun resetSoundEvent() {
        _soundEvent.value = null
    }

    fun setFocusSound(soundId: Int, volume: Float) {
        _focusSoundId.value = soundId
        _focusSoundVolume.value = volume
    }
}