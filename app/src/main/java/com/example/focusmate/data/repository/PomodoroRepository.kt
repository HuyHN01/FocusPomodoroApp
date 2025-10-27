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

    private var sessionTotalTime = DEFAULT_POMODORO // tổng thời gian của session hiện tại (dùng cho progress max)
    private var currentTime = DEFAULT_POMODORO      // thời gian còn lại (giây)

    private var timer: CountDownTimer? = null
    private var pomodoroCount = 0 // số Pomodoro 25 phút đã hoàn thành

    // LiveData
    private val _timeLeft = MutableLiveData<Int>(currentTime)
    val timeLeft: LiveData<Int> get() = _timeLeft

    private val _sessionTotal = MutableLiveData<Int>(sessionTotalTime)
    val sessionTotal: LiveData<Int> get() = _sessionTotal

    private val _state = MutableLiveData<TimerState>(TimerState.IDLE)
    val state: LiveData<TimerState> get() = _state

    private val _soundEvent = MutableLiveData<SoundEvent?>()
    val soundEvent: LiveData<SoundEvent?> = _soundEvent

    // START / RESUME entry point - thông minh: tự biết đang ở chế độ nào
    fun startTimer() {
        when (_state.value) {
            TimerState.RUNNING, TimerState.BREAK_RUNNING -> {
                // đang chạy -> nothing
                return
            }
            TimerState.BREAK_READY -> {
                // user pressed "Bắt đầu giải lao"
                startBreak()
            }
            TimerState.BREAK_PAUSED -> {
                // resume break
                resumeBreak()
            }
            TimerState.PAUSED -> {
                // resume pomodoro
                resumePomodoro()
            }
            TimerState.IDLE -> {
                // start a new pomodoro
                startPomodoro()
            }
            else -> startPomodoro()
        }
    }

    private fun startPomodoro() {
        timer?.cancel()
        sessionTotalTime = DEFAULT_POMODORO
        currentTime = sessionTotalTime
        _sessionTotal.value = sessionTotalTime
        _timeLeft.value = currentTime
        _state.value = TimerState.RUNNING
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

    // Called when a pomodoro finishes
    private fun onPomodoroFinished() {
        pomodoroCount++
        // determine next break length
        val nextBreak = if (pomodoroCount % POMODOROS_BEFORE_LONG_BREAK == 0) LONG_BREAK else SHORT_BREAK
        // set up break ready state
        sessionTotalTime = nextBreak
        currentTime = nextBreak
        _sessionTotal.postValue(sessionTotalTime)
        _timeLeft.postValue(currentTime)
        _state.postValue(TimerState.BREAK_READY)
        // do NOT auto-start break — wait for user to press "Bắt đầu giải lao" (per requirement)

        _soundEvent.value = SoundEvent.END_FOCUS
    }

    // Start break when user presses Start Break
    fun startBreak() {
        if (_state.value == TimerState.BREAK_READY || _state.value == TimerState.BREAK_PAUSED) {
            _state.value = TimerState.BREAK_RUNNING
            // sessionTotalTime and currentTime should already be set by onPomodoroFinished
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

    // If user chooses to skip break (or pressed "Bỏ qua Giải lao")
    fun skipBreak() {
        // stop any running timer and prepare next pomodoro (do not increment pomodoroCount)
        timer?.cancel()
        sessionTotalTime = DEFAULT_POMODORO
        currentTime = sessionTotalTime
        _sessionTotal.value = sessionTotalTime
        _timeLeft.value = currentTime
        _state.value = TimerState.IDLE
    }

    // When break finishes naturally
    private fun onBreakFinished() {
        // after break (short or long), prepare next pomodoro
        sessionTotalTime = DEFAULT_POMODORO
        currentTime = sessionTotalTime
        _sessionTotal.postValue(sessionTotalTime)
        _timeLeft.postValue(currentTime)
        _state.postValue(TimerState.IDLE)

        _soundEvent.value = SoundEvent.END_BREAK
    }

    // Pause (works for both pomodoro and break)
    fun pauseTimer() {
        timer?.cancel()
        when (_state.value) {
            TimerState.RUNNING -> _state.value = TimerState.PAUSED
            TimerState.BREAK_RUNNING -> _state.value = TimerState.BREAK_PAUSED
            else -> { /* nothing */ }
        }
    }

    // Reset (Ngừng lại) => reset current session to default pomodoro and set IDLE
    fun resetTimer() {
        timer?.cancel()
        sessionTotalTime = DEFAULT_POMODORO
        currentTime = sessionTotalTime
        _sessionTotal.value = sessionTotalTime
        _timeLeft.value = currentTime
        _state.value = TimerState.IDLE
        // NOTE: pomodoroCount is kept as-is (so long break logic remains correct)
    }

    // Add one minute to the current session (works when RUNNING or BREAK_RUNNING)
    fun addOneMinute() {
        if (_state.value == TimerState.RUNNING || _state.value == TimerState.BREAK_RUNNING) {
            timer?.cancel()
            currentTime += 60
            sessionTotalTime += 60
            _timeLeft.value = currentTime
            _sessionTotal.value = sessionTotalTime
            // restart countdown with updated time
            // isBreak boolean depends on current state
            val isBreak = _state.value == TimerState.BREAK_RUNNING
            startCountDown(isBreak)
        }
    }

    // Generic countdown starter
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

    // Allow manual setting of session time (for settings dialog)
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
}
