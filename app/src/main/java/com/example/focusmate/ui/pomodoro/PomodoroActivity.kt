package com.example.focusmate.ui.pomodoro

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.focusmate.databinding.ActivityPomodoroBinding

class PomodoroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPomodoroBinding
    private val viewModel: PomodoroViewModel by viewModels()

    private val defaultTotalTime = 25 * 60 // 25 phút

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPomodoroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khóa portrait
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        observeViewModel()
        setupListeners()
    }

    private fun observeViewModel() {
        // Quan sát thời gian
        viewModel.timeLeft.observe(this) { seconds ->
            val minutes = seconds / 60
            val sec = seconds % 60
            binding.tvTimer.text = String.format("%02d:%02d", minutes, sec)

            // Cập nhật progress vòng tròn
            binding.cpvTimerProgress.setMaxProgress(defaultTotalTime.toFloat())
            binding.cpvTimerProgress.setProgress((defaultTotalTime - seconds).toFloat())
        }

        // Quan sát trạng thái Timer
        viewModel.state.observe(this) { state ->
            when (state) {
                TimerState.IDLE -> {
                    binding.btnStart.visibility = View.VISIBLE
                    binding.btnPause.visibility = View.GONE
                    binding.btnResume.visibility = View.GONE
                    binding.btnStop.visibility = View.GONE
                }
                TimerState.RUNNING -> {
                    binding.btnStart.visibility = View.GONE
                    binding.btnPause.visibility = View.VISIBLE
                    binding.btnResume.visibility = View.GONE
                    binding.btnStop.visibility = View.GONE
                }
                TimerState.PAUSED -> {
                    binding.btnStart.visibility = View.GONE
                    binding.btnPause.visibility = View.GONE
                    binding.btnResume.visibility = View.VISIBLE
                    binding.btnStop.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupListeners() {
        // Điều khiển Timer
        binding.btnStart.setOnClickListener { viewModel.startTimer() }
        binding.btnPause.setOnClickListener { viewModel.pauseTimer() }
        binding.btnResume.setOnClickListener { viewModel.resumeTimer() }
        binding.btnStop.setOnClickListener { viewModel.resetTimer() }

        // Mở chế độ toàn màn hình
        binding.llFocus.setOnClickListener { openFullscreenTimer() }
    }

    private fun openFullscreenTimer() {
        val intent = Intent(this, FullscreenTimerActivity::class.java)
        startActivity(intent)
    }
}
