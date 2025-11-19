package com.example.focusmate.ui.pomodoro

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.example.focusmate.R
import com.example.focusmate.data.repository.PomodoroRepository
import com.example.focusmate.databinding.ActivityPomodoroBinding
import com.example.focusmate.ui.todolist.TaskViewModel
import com.example.focusmate.util.PomodoroService
import com.example.focusmate.util.PomodoroSoundPlayer
import com.example.focusmate.util.SoundEvent
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.collection.LLRBNode

class PomodoroActivity : AppCompatActivity() {
    private var currentTaskId: String? = null
    private lateinit var taskViewModel: TaskViewModel
    

    private lateinit var binding: ActivityPomodoroBinding
    private val viewModel: PomodoroViewModel by viewModels()

    private val defaultTotalTime = 25 * 60 

    private lateinit var soundPlayer: PomodoroSoundPlayer

    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Snackbar.make(
                binding.root,
                "Cần quyền thông báo để hiển thị timer ở chế độ nền",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.dark(
                Color.TRANSPARENT
            )

        )

        binding = ActivityPomodoroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val rootLayout = findViewById<View>(R.id.root_layout)

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            

            
            val btnBack = findViewById<View>(R.id.btn_back)
            btnBack.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                
                topMargin = 32.dpToPx() + bars.top
            }

            
            val tvStatus = findViewById<View>(R.id.tv_status)
            tvStatus.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = 80.dpToPx() + bars.top
            }

            
            val fragmentContainer = findViewById<View>(R.id.pomodoro_task_fragment_container)
            fragmentContainer.setPadding(0, bars.top, 0, 0)


            

            
            val bottomNav = findViewById<View>(R.id.ll_bottom_nav)
            bottomNav.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                
                bottomMargin = 32.dpToPx() + bars.bottom
            }

            
            insets
        }

        soundPlayer = PomodoroSoundPlayer(this)

        
        checkNotificationPermission()

        
        PomodoroService.startService(this)

        observeViewModel()
        setupListeners()


        taskViewModel = ViewModelProvider(this).get(TaskViewModel::class.java)

        

        
        val intentTaskId = intent.getStringExtra("EXTRA_TASK_ID")

        if (intentTaskId != null) {
            
            currentTaskId = intentTaskId
            PomodoroRepository.currentTaskId = intentTaskId
        } else {
            
            
            currentTaskId = PomodoroRepository.currentTaskId
        }

        
        currentTaskId?.let { taskId ->
            taskViewModel.loadTaskById(taskId)
        }

        setupObserver()
    }

    private fun setupObserver() {
        
        taskViewModel.currentTask.asLiveData().observe(this) { task ->
            if (task != null) {
                
                binding.tvStatus.visibility = View.GONE
                binding.pomodoroTaskFragmentContainer.visibility = View.VISIBLE

                
                loadTaskFragment(task.title)

                
                if (viewModel.state.value == TimerState.IDLE) {
                    viewModel.startTimer()
                }

            } else {
                

                
                
                

                
                
                binding.tvStatus.visibility = View.VISIBLE
                binding.pomodoroTaskFragmentContainer.visibility = View.GONE
            }
        }
    }

    
    private fun loadTaskFragment(taskTitle: String) {
        val fragment = PomodoroTaskFragment.newInstance(taskTitle)

        supportFragmentManager.beginTransaction()
            .replace(R.id.pomodoro_task_fragment_container, fragment)
            .commit()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    
                    Snackbar.make(
                        binding.root,
                        "Ứng dụng cần quyền thông báo để hoạt động ở chế độ nền",
                        Snackbar.LENGTH_LONG
                    ).setAction("Cấp quyền") {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }.show()
                }
                else -> {
                    
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun observeViewModel() {
        
        viewModel.timeLeft.observe(this) { seconds ->
            val minutes = seconds / 60
            val sec = seconds % 60
            binding.tvTimer.text = String.format("%02d:%02d", minutes, sec)

            
            viewModel.sessionTotal.observe(this) { total ->
                binding.cpvTimerProgress.setMaxProgress(total.toFloat())
            }
            
            val total = viewModel.sessionTotal.value ?: defaultTotalTime
            binding.cpvTimerProgress.setProgress((total - seconds).toFloat())
        }

        
        viewModel.state.observe(this) { state ->
            when (state) {
                TimerState.IDLE -> {
                    binding.btnStart.visibility = View.VISIBLE
                    binding.btnPause.visibility = View.GONE
                    binding.btnResume.visibility = View.GONE
                    binding.btnStop.visibility = View.GONE
                    binding.btnStartBreak.visibility = View.GONE
                    binding.btnSkipBreak.visibility = View.GONE
                }
                TimerState.RUNNING -> {
                    binding.btnStart.visibility = View.GONE
                    binding.btnPause.visibility = View.VISIBLE
                    binding.btnResume.visibility = View.GONE
                    binding.btnStop.visibility = View.GONE
                    binding.btnStartBreak.visibility = View.GONE
                    binding.btnSkipBreak.visibility = View.GONE
                }
                TimerState.PAUSED -> {
                    binding.btnStart.visibility = View.GONE
                    binding.btnPause.visibility = View.GONE
                    binding.btnResume.visibility = View.VISIBLE
                    binding.btnStop.visibility = View.VISIBLE
                    binding.btnStartBreak.visibility = View.GONE
                    binding.btnSkipBreak.visibility = View.GONE
                }
                TimerState.BREAK_READY -> {
                    
                    binding.btnStart.visibility = View.GONE
                    binding.btnPause.visibility = View.GONE
                    binding.btnResume.visibility = View.GONE
                    binding.btnStop.visibility = View.GONE
                    binding.btnStartBreak.visibility = View.VISIBLE
                    binding.btnSkipBreak.visibility = View.GONE
                }
                TimerState.BREAK_RUNNING -> {
                    
                    binding.btnStart.visibility = View.GONE
                    binding.btnPause.visibility = View.GONE
                    binding.btnResume.visibility = View.GONE
                    binding.btnStop.visibility = View.GONE
                    binding.btnStartBreak.visibility = View.GONE
                    binding.btnSkipBreak.visibility = View.VISIBLE
                }
                TimerState.BREAK_PAUSED -> {
                    binding.btnStart.visibility = View.GONE
                    binding.btnPause.visibility = View.GONE
                    binding.btnResume.visibility = View.GONE
                    binding.btnStop.visibility = View.GONE
                    binding.btnStartBreak.visibility = View.GONE
                    binding.btnSkipBreak.visibility = View.VISIBLE
                }
            }
        }

        viewModel.soundEvent.observe(this) {event ->
            event?.let {
                soundPlayer.playSound(it)

                
                if (it == SoundEvent.END_FOCUS && currentTaskId != null) {

                    currentTaskId?.let { id ->

                        taskViewModel.incrementCompletedPomodoros(id)
                    }
                }
                viewModel.resetSoundEvent()
            }

        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            
            
            finish()

            
            
        }
        binding.btnStart.setOnClickListener { viewModel.startTimer() }         
        binding.btnPause.setOnClickListener { viewModel.pauseTimer() }
        binding.btnResume.setOnClickListener { viewModel.resumeTimer() }
        binding.btnStop.setOnClickListener { viewModel.resetTimer() }

        binding.btnStartBreak.setOnClickListener { viewModel.startBreak() }    
        binding.btnSkipBreak.setOnClickListener { viewModel.skipBreak() }      

        binding.llMeditation.setOnClickListener {
            
            
        }

        binding.llTimer.setOnClickListener {
            
            
        }

        binding.llFocus.setOnClickListener {
            
            openFullscreenTimer()
        }

        binding.llMusic.setOnClickListener {
            
            
            openFocusSoundDialog()
        }
    }

    private fun openFocusSoundDialog() {
        val currentSoundId = viewModel.focusSoundId.value ?: 0
        val currentVolume = viewModel.focusSoundVolume.value ?: 0.7f

        val dialog = FocusSoundDialog(
            currentSoundId = currentSoundId,
            currentVolume = currentVolume,
            onConfirm = { soundId, volume ->
                viewModel.setFocusSound(soundId, volume)
            }
        )

        dialog.show(supportFragmentManager, "FocusSoundDialogTag")
    }

    private fun openFullscreenTimer() {
        val intent = Intent(this, FullscreenTimerActivity::class.java)
        startActivity(intent)
    }

    private fun setActiveTab(selected: View) {
        val tabs = listOf(binding.llMeditation, binding.llTimer, binding.llFocus, binding.llMusic)

        tabs.forEach { tab ->
            tab.isSelected = (tab == selected)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPlayer.release()
        
        
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}