package com.example.focusmate.ui.pomodoro

import android.os.Bundle
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

        observeViewModel()
        setupListeners()
    }

    private fun observeViewModel() {
        // Quan sát thời gian
        viewModel.timeLeft.observe(this) { seconds ->
            val minutes = seconds / 60
            val sec = seconds % 60
            binding.tvTimer.text = String.format("%02d:%02d", minutes, sec)

            // Cập nhật progress
            binding.cpvTimerProgress.setMaxProgress(defaultTotalTime.toFloat())
            binding.cpvTimerProgress.setProgress((defaultTotalTime - seconds).toFloat())
        }

        // Quan sát trạng thái chạy
        viewModel.isRunning.observe(this) { running ->
            binding.btnStartPause.text = if (running) "Tạm dừng" else "▶ Bắt đầu tập trung"
        }
    }

    private fun setupListeners() {
        binding.btnStartPause.setOnClickListener {
            if (viewModel.isRunning.value == true) {
                viewModel.pauseTimer()
            } else {
                viewModel.startTimer()
            }
        }

        // Ví dụ: nếu muốn có nút reset
        // binding.btnReset.setOnClickListener {
        //     viewModel.resetTimer()
        // }
    }
}