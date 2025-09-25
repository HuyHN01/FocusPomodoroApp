package com.example.focusmate.ui.pomodoro

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.focusmate.databinding.ActivityFullscreenTimerBinding

class FullscreenTimerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullscreenTimerBinding
    private val viewModel: PomodoroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFullscreen()

        binding = ActivityFullscreenTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()
        setupClickListeners()
    }

    private fun setupFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun observeViewModel() {
        viewModel.timeLeft.observe(this) { seconds ->
            val minutes = seconds / 60
            val sec = seconds % 60
            binding.tvMinutes.text = String.format("%02d", minutes)
            binding.tvSeconds.text = String.format("%02d", sec)
        }

        // Cập nhật thanh control theo state
        viewModel.state.observe(this) { state ->
            when (state) {
                TimerState.IDLE -> {
                    // only show play
                    binding.btnPlay.visibility = View.VISIBLE
                    binding.btnAddOne.visibility = View.GONE
                    binding.divider.visibility = View.GONE
                    binding.btnPause.visibility = View.GONE
                    binding.btnStop.visibility = View.GONE
                }
                TimerState.RUNNING -> {
                    binding.btnPlay.visibility = View.GONE
                    binding.btnAddOne.visibility = View.VISIBLE
                    binding.divider.visibility = View.VISIBLE
                    binding.btnPause.visibility = View.VISIBLE
                    binding.btnStop.visibility = View.GONE
                }
                TimerState.PAUSED -> {
                    binding.btnPlay.visibility = View.VISIBLE
                    binding.btnAddOne.visibility = View.VISIBLE
                    binding.divider.visibility = View.VISIBLE
                    binding.btnPause.visibility = View.GONE
                    binding.btnStop.visibility = View.VISIBLE
                }
                TimerState.BREAK_READY -> {
                    binding.btnPlay.visibility = View.VISIBLE
                    binding.btnAddOne.visibility = View.GONE
                    binding.divider.visibility = View.GONE
                    binding.btnPause.visibility = View.GONE
                    binding.btnStop.visibility = View.GONE
                }
                TimerState.BREAK_RUNNING -> {
                    binding.btnPlay.visibility = View.GONE
                    binding.btnAddOne.visibility = View.GONE // allow +1 break? per previous design yes
                    binding.divider.visibility = View.GONE
                    binding.btnPause.visibility = View.GONE
                    binding.btnStop.visibility = View.VISIBLE
                }
                TimerState.BREAK_PAUSED -> {
                    binding.btnPlay.visibility = View.GONE
                    binding.btnAddOne.visibility = View.GONE
                    binding.divider.visibility = View.GONE
                    binding.btnPause.visibility = View.GONE
                    binding.btnStop.visibility = View.GONE
                }
            }
            adjustControlsContainerWidth(binding.llBottomControls!!)
        }
    }

    private fun setupClickListeners() {
        binding.btnPlay?.setOnClickListener {
            viewModel.startTimer()
        }

        binding.btnPause?.setOnClickListener {
            viewModel.pauseTimer()
        }

        binding.btnStop?.setOnClickListener {
            viewModel.resetTimer()
        }

        binding.btnAddOne?.setOnClickListener {
            viewModel.addOneMinute()
        }

        binding.btnCloseFullscreen?.setOnClickListener {
            finish()
        }

        binding.btnVolume?.setOnClickListener { /* TODO */ }
        binding.btnTimerSettings?.setOnClickListener { /* TODO */ }
        binding.btnSettings?.setOnClickListener { /* TODO */ }
    }

    fun adjustControlsContainerWidth(container: ConstraintLayout?) {
        container?.let { c ->
            c.post {
                var totalWidth = 0
                var visibleCount = 0

                for (i in 0 until c.childCount) {
                    val child = c.getChildAt(i)
                    if (child.visibility == View.VISIBLE) {
                        var w = child.measuredWidth
                        if (w == 0) {
                            child.measure(
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                            )
                            w = child.measuredWidth
                        }
                        totalWidth += w
                        visibleCount++
                    }
                }

                // spacing giữa các nút (ví dụ 16dp)
                val spacingPx = (24 * c.resources.displayMetrics.density).toInt()
                if (visibleCount > 1) {
                    totalWidth += (visibleCount - 1) * spacingPx
                }

                totalWidth += c.paddingLeft + c.paddingRight

                val params = c.layoutParams
                params.width = totalWidth
                c.layoutParams = params
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
