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
    //ket thuc them vao

    private lateinit var binding: ActivityPomodoroBinding
    private val viewModel: PomodoroViewModel by viewModels()

    private val defaultTotalTime = 25 * 60 // 25 phút

    private lateinit var soundPlayer: PomodoroSoundPlayer

    // Request notification permission for Android 13+
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

        // Khóa portrait
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val rootLayout = findViewById<View>(R.id.root_layout)

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // --- XỬ LÝ PHẦN TRÊN (STATUS BAR) ---

            // A. Nút Back (Đang có margin top 32dp)
            val btnBack = findViewById<View>(R.id.btn_back)
            btnBack.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                // Logic: Margin gốc (32dp) + Chiều cao Status Bar
                topMargin = 32.dpToPx() + bars.top
            }

            // B. Dòng chữ "Vui lòng chọn..." (Đang có margin top 80dp)
            val tvStatus = findViewById<View>(R.id.tv_status)
            tvStatus.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = 80.dpToPx() + bars.top
            }

            // C. Fragment Container (Nếu cần nó né status bar)
            val fragmentContainer = findViewById<View>(R.id.pomodoro_task_fragment_container)
            fragmentContainer.setPadding(0, bars.top, 0, 0)


            // --- XỬ LÝ PHẦN DƯỚI (NAVIGATION BAR) ---

            // D. Thanh menu dưới cùng (Đang có margin bottom 32dp)
            val bottomNav = findViewById<View>(R.id.ll_bottom_nav)
            bottomNav.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                // Logic: Margin gốc (32dp) + Chiều cao Navigation Bar (thanh vuốt/nút điều hướng)
                bottomMargin = 32.dpToPx() + bars.bottom
            }

            // Trả về insets (để hệ thống tiếp tục xử lý nếu cần)
            insets
        }

        soundPlayer = PomodoroSoundPlayer(this)

        // Request notification permission if needed
        checkNotificationPermission()

        // Start foreground service
        PomodoroService.startService(this)

        observeViewModel()
        setupListeners()


        taskViewModel = ViewModelProvider(this).get(TaskViewModel::class.java)

        // --- SỬA ĐOẠN LOGIC NHẬN INTENT Ở ĐÂY ---

        // 1. Thử lấy ID từ Intent (Trường hợp mở từ màn hình Todolist)
        val intentTaskId = intent.getStringExtra("EXTRA_TASK_ID")

        if (intentTaskId != null) {
            // Nếu có ID mới từ Intent -> Cập nhật vào biến toàn cục & Repository
            currentTaskId = intentTaskId
            PomodoroRepository.currentTaskId = intentTaskId
        } else {
            // Nếu Intent không có ID (Trường hợp mở từ Notification)
            // -> Lấy lại ID từ "trí nhớ" của Repository
            currentTaskId = PomodoroRepository.currentTaskId
        }

        // 2. Nếu tìm được ID (dù từ nguồn nào), hãy load Task đó lên
        currentTaskId?.let { taskId ->
            taskViewModel.loadTaskById(taskId)
        }

        setupObserver()
    }

    private fun setupObserver() {
        // Quan sát Task hiện tại
        taskViewModel.currentTask.asLiveData().observe(this) { task ->
            if (task != null) {
                // ĐÃ CÓ TASK: Ẩn TextView, Hiện Fragment
                binding.tvStatus.visibility = View.GONE
                binding.pomodoroTaskFragmentContainer.visibility = View.VISIBLE

                // Tải Fragment mới với tên task
                loadTaskFragment(task.title)

                // Chỉ start nếu đang IDLE (để tránh reset time nếu đang chạy)
                if (viewModel.state.value == TimerState.IDLE) {
                    viewModel.startTimer()
                }

            } else {
                // KHÔNG CÓ TASK: Hiện TextView, Ẩn Fragment

                // --- SỬA LỖI TẠI ĐÂY ---
                // XÓA hoặc COMMENT dòng này:
                // viewModel.pauseTimer()

                // Logic đúng: Dù không có Task hiển thị, nhưng nếu Timer đang chạy thì cứ để nó chạy tiếp
                // Chỉ hiển thị giao diện mặc định
                binding.tvStatus.visibility = View.VISIBLE
                binding.pomodoroTaskFragmentContainer.visibility = View.GONE
            }
        }
    }

    // Hàm mới để tải Fragment
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
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show rationale and request permission
                    Snackbar.make(
                        binding.root,
                        "Ứng dụng cần quyền thông báo để hoạt động ở chế độ nền",
                        Snackbar.LENGTH_LONG
                    ).setAction("Cấp quyền") {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }.show()
                }
                else -> {
                    // Request permission directly
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun observeViewModel() {
        // thời gian
        viewModel.timeLeft.observe(this) { seconds ->
            val minutes = seconds / 60
            val sec = seconds % 60
            binding.tvTimer.text = String.format("%02d:%02d", minutes, sec)

            // cập nhật progress max theo session total (hỗ trợ +1 min)
            viewModel.sessionTotal.observe(this) { total ->
                binding.cpvTimerProgress.setMaxProgress(total.toFloat())
            }
            // set progress
            val total = viewModel.sessionTotal.value ?: defaultTotalTime
            binding.cpvTimerProgress.setProgress((total - seconds).toFloat())
        }

        // trạng thái
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
                    // show start break and skip
                    binding.btnStart.visibility = View.GONE
                    binding.btnPause.visibility = View.GONE
                    binding.btnResume.visibility = View.GONE
                    binding.btnStop.visibility = View.GONE
                    binding.btnStartBreak.visibility = View.VISIBLE
                    binding.btnSkipBreak.visibility = View.GONE
                }
                TimerState.BREAK_RUNNING -> {
                    // show pause + skip (per requirement)
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

                //Them doan nay de nam duoc so luong pomo da hoan thanh
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
            // Cách 1: Đóng Activity hiện tại.
            // Activity trước đó (MainScreen) sẽ tự động hiện ra vì nó nằm bên dưới trong Stack.
            finish()

            // Hoặc Cách 2 (Hiện đại hơn): Giả lập hành động nhấn nút Back hệ thống/vuốt Back
            // onBackPressedDispatcher.onBackPressed()
        }
        binding.btnStart.setOnClickListener { viewModel.startTimer() }         // start pomodoro
        binding.btnPause.setOnClickListener { viewModel.pauseTimer() }
        binding.btnResume.setOnClickListener { viewModel.resumeTimer() }
        binding.btnStop.setOnClickListener { viewModel.resetTimer() }

        binding.btnStartBreak.setOnClickListener { viewModel.startBreak() }    // start break
        binding.btnSkipBreak.setOnClickListener { viewModel.skipBreak() }      // skip break

        binding.llMeditation.setOnClickListener {
            //setActiveTab(binding.llMeditation)
            // TODO: mở chế độ Meditation
        }

        binding.llTimer.setOnClickListener {
            //setActiveTab(binding.llTimer)
            // TODO: mở chế độ Timer
        }

        binding.llFocus.setOnClickListener {
            //setActiveTab(binding.llFocus)
            openFullscreenTimer()
        }

        binding.llMusic.setOnClickListener {
            //setActiveTab(binding.llMusic)
            // TODO: mở chế độ Music
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
        // NOTE: Không stop service ở đây vì ta muốn timer chạy ngay cả khi Activity bị destroy
        // Service sẽ tự stop khi user nhấn Stop trong notification
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}