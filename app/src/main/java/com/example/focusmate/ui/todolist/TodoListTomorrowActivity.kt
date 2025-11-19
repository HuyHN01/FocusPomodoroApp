package com.example.focusmate.ui.todolist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.example.focusmate.databinding.ActivityTodolisttomorrowBinding
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.focusmate.data.local.entity.TaskPriority
import com.example.focusmate.ui.pomodoro.PomodoroActivity
import com.example.focusmate.ui.pomodoro.PomodoroViewModel

class TodoListTomorrowActivity : AppCompatActivity(), AddTaskListener {

    private lateinit var binding: ActivityTodolisttomorrowBinding
    private lateinit var tomorrowViewModel: TomorrowViewModel
    private lateinit var pomodoroViewModel: PomodoroViewModel
    private lateinit var tasksAdapter: TasksAdapter
    private lateinit var completedTasksAdapter: TasksAdapter
    override fun onTaskAddedFromFragment(title: String, pomodoros: Int, priority: TaskPriority, date: Long?) {
        // Activity Ngày Mai nhận được lệnh -> Gọi ViewModel Ngày Mai
        tomorrowViewModel.addNewTask(
            title = title,
            estimatedPomodoros = pomodoros,
            priority = priority
        )

        // Xử lý ẩn bàn phím (copy logic từ chỗ EditorAction xuống đây nếu cần)
        binding.addTaskEditText.clearFocus()
        hideKeyboard()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Inflate layout
        binding = ActivityTodolisttomorrowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. KHỞI TẠO VIEWMODEL
        tomorrowViewModel = ViewModelProvider(this)[TomorrowViewModel::class.java]
        pomodoroViewModel = ViewModelProvider(this)[PomodoroViewModel::class.java]

        // 2. XỬ LÝ NÚT BACK (QUAN TRỌNG)
        binding.backArrow.setOnClickListener {
            finish() // Đóng màn hình quay về Main
        }

        // --- LƯU Ý: Đã xóa đoạn try-catch chỉnh visibility ---
        // Lý do: File XML mới của bạn đã ẩn sẵn các phần thừa rồi,
        // không cần code để ẩn nữa, xóa đi cho đỡ bị lỗi đỏ.

        // 3. SETUP ADAPTERS
        tasksAdapter = TasksAdapter(
            onTaskClick = { task ->
                val intent = Intent(this, TaskDetailActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", task.taskId)
                }
                startActivity(intent)
            },
            onCompleteClick = { task -> tomorrowViewModel.toggleTaskCompletion(task.taskId) },
            onPlayClick = { task ->
                val intent = Intent(this, PomodoroActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", task.taskId)
                }
                startActivity(intent)
            }
        )

        completedTasksAdapter = TasksAdapter(
            onTaskClick = { task ->
                val intent = Intent(this, TaskDetailActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", task.taskId)
                }
                startActivity(intent)
            },
            onCompleteClick = { task -> tomorrowViewModel.toggleTaskCompletion(task.taskId) },
            onPlayClick = { task ->
                val intent = Intent(this, PomodoroActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", task.taskId)
                }
                startActivity(intent)
            }
        )

        // 4. SETUP RECYCLERVIEW
        binding.tasksList.apply {
            layoutManager = LinearLayoutManager(this@TodoListTomorrowActivity)
            adapter = tasksAdapter
        }
        binding.completedTasksList.apply {
            layoutManager = LinearLayoutManager(this@TodoListTomorrowActivity)
            adapter = completedTasksAdapter
        }

        // 5. OBSERVE DỮ LIỆU (QUAN TRỌNG)
        tomorrowViewModel.uncompletedTasks.observe(this) { uncompleted ->
            tasksAdapter.submitList(uncompleted)

            // --- [SỬA] BỎ COMMENT ĐOẠN NÀY ĐỂ HIỆN HÌNH EMPTY ---
            if (uncompleted.isEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.tasksContainer.visibility = View.GONE
            } else {
                binding.emptyStateLayout.visibility = View.GONE
                binding.tasksContainer.visibility = View.VISIBLE
            }
            // ---------------------------------------------------
        }

        tomorrowViewModel.completedTasks.observe(this) { completed ->
            completedTasksAdapter.submitList(completed)
        }

        // --- [THÊM] CẬP NHẬT SỐ LƯỢNG TASK LÊN MÀN HÌNH ---
        tomorrowViewModel.uncompletedCount.observe(this) { count ->
            // Cập nhật số to màu đỏ (Task cần làm)
            binding.taskNeedCompleteTV.text = count.toString()
        }
        tomorrowViewModel.estimatedTimeFormatted.observe(this) { time ->
            // Cập nhật TextView 00:00 (thời gian ước tính)
            // Đảm bảo ID trong layout của bạn đúng là 'estimatedTimeTv'
            // (Nếu trong layout bạn đặt là 'estimated_time_tv' thì sửa code bên dưới cho khớp)
            binding.estimatedTimeTv.text = time
        }

        // Header ẩn/hiện task hoàn thành
        binding.completedTasksHeader.setOnClickListener {
            if (binding.completedTasksList.visibility == View.VISIBLE) {
                binding.completedTasksList.visibility = View.GONE
                binding.completedTasksHeader.text = "Hiển thị những công việc đã hoàn thành ▼"
            } else {
                binding.completedTasksList.visibility = View.VISIBLE
                binding.completedTasksHeader.text = "Ẩn đi những công việc đã hoàn thành ▲"
            }
        }

        // 6. XỬ LÝ THÊM TASK MỚI
        binding.addTaskEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val title = binding.addTaskEditText.text.toString().trim()
                if (title.isNotEmpty()) {
                    // Thêm vào ngày mai
                    tomorrowViewModel.addNewTask(
                        title = title,
                        estimatedPomodoros = 1,
                        priority = TaskPriority.NONE
                    )
                    binding.addTaskEditText.text.clear()
                }
                true
            } else {
                false
            }
        }

        binding.addTaskEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.addTaskFragment.visibility = View.VISIBLE
            } else {
                binding.addTaskFragment.visibility = View.GONE
            }
        }
    }
    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}