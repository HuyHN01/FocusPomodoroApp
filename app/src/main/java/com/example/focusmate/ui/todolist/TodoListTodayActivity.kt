package com.example.focusmate.ui.todolist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.focusmate.databinding.ActivityTodolistBinding
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.focusmate.data.local.entity.TaskPriority
import com.example.focusmate.ui.pomodoro.PomodoroActivity
import com.example.focusmate.ui.pomodoro.PomodoroViewModel
import androidx.core.view.isVisible


class TodoListTodayActivity : AppCompatActivity(), AddTaskListener{
    private lateinit var binding: ActivityTodolistBinding
    private lateinit var taskViewModel: TaskViewModel
    private lateinit var pomodoroViewModel: PomodoroViewModel

    // --- 1. KHAI BÁO 3 ADAPTER ---
    private lateinit var tasksAdapter: TasksAdapter // Cho task "Hôm nay"
    private lateinit var otherTasksAdapter: TasksAdapter // CHO TASK "KHÁC" (MỚI)
    private lateinit var completedTasksAdapter: TasksAdapter
    override fun onTaskAddedFromFragment(title: String, pomodoros: Int, priority: TaskPriority, date: Long?) {
        // Activity Hôm Nay nhận lệnh -> Gọi ViewModel Hôm Nay
        // dueDate = date (để null thì ViewModel tự xử là hôm nay, hoặc truyền date nếu user chọn)
        taskViewModel.addNewTask(
            title = title,
            estimatedPomodoros = pomodoros,
            priority = priority,
            dueDate = date
        )

        // Xử lý ẩn bàn phím & giao diện
        binding.addTaskEditText.clearFocus()
        hideKeyboard()
        binding.addTaskFragment.visibility = View.GONE
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityTodolistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        taskViewModel = ViewModelProvider(this)[TaskViewModel::class.java]
        pomodoroViewModel = ViewModelProvider(this)[PomodoroViewModel::class.java]

        // --- 2. XỬ LÝ CLICK CÁC HEADER ---
        binding.completedTasksHeader.setOnClickListener {
            if (binding.completedTasksList.isVisible) {
                binding.completedTasksList.visibility = View.GONE
                binding.completedTasksHeader.text = "Hiển thị những công việc đã hoàn thành ▼"
            } else {
                binding.completedTasksList.visibility = View.VISIBLE
                binding.completedTasksHeader.text = "Ẩn đi những công việc đã hoàn thành ▲"
            }
        }
        binding.backArrow.setOnClickListener {
            finish()
        }
        taskViewModel.timeElapsedFormatted.observe(this) { formattedTime ->
            binding.tvTotalTimeElapsed.text = formattedTime // Dùng ID mới ở Bước 1
        }


        // --- 3. KHỞI TẠO 3 ADAPTER ---

        // Adapter 1: Task "Hôm nay" (Code cũ của em)
        tasksAdapter = TasksAdapter(
            onTaskClick = { task ->
                val intent = Intent(this, TaskDetailActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", task.taskId)
                }
                startActivity(intent)
            },
            onCompleteClick = { task ->
                taskViewModel.toggleTaskCompletion(task.taskId)
            },
            onPlayClick = { task ->
                val intent = Intent(this, PomodoroActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", task.taskId)
                }
                startActivity(intent)
            }
        )

        // Adapter 2: Task "Hoàn thành" (Code cũ của em)
        completedTasksAdapter = TasksAdapter(
            onTaskClick = { task ->
                val intent = Intent(this, TaskDetailActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", task.taskId)
                }
                startActivity(intent)
            },
            onCompleteClick = { task ->
                taskViewModel.toggleTaskCompletion(task.taskId)
            },
            onPlayClick = { task ->
                val intent = Intent(this, PomodoroActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", task.taskId)
                }
                startActivity(intent)
            }
        )

        // Adapter 3: Task "Khác" (MỚI - Copy y hệt)
        otherTasksAdapter = TasksAdapter(
            onTaskClick = { task ->
                val intent = Intent(this, TaskDetailActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", task.taskId)
                }
                startActivity(intent)
            },
            onCompleteClick = { task ->
                taskViewModel.toggleTaskCompletion(task.taskId)
            },
            onPlayClick = { task ->
                val intent = Intent(this, PomodoroActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", task.taskId)
                }
                startActivity(intent)
            }
        )

        // --- 4. SETUP 3 RECYCLERVIEW ---
        binding.tasksList.apply { // List "Hôm nay"
            layoutManager = LinearLayoutManager(this@TodoListTodayActivity)
            adapter = tasksAdapter
        }
        binding.completedTasksList.apply { // List "Hoàn thành"
            layoutManager = LinearLayoutManager(this@TodoListTodayActivity)
            adapter = completedTasksAdapter
        }


        // --- 5. SỬA LẠI CÁC OBSERVER ---

        // Observer "Thời gian" (Code cũ của em - Đã đúng)
        taskViewModel.estimatedTimeFormatted.observe(this) { formattedTime ->
            binding.tvTotalEstimatedTime.text = formattedTime
        }

        // Sửa lỗi cú pháp và logic
        taskViewModel.uncompletedTasks.observe(this) { uncompletedToday ->
            tasksAdapter.submitList(uncompletedToday)
        }

        // Thêm observer mới cho "Công việc khác"
//        taskViewModel.otherPendingTasks.observe(this) { otherTasks ->
//            otherTasksAdapter.submitList(otherTasks)
//        }

        // Observer "Hoàn thành" (Giữ nguyên)
        taskViewModel.completedTasks.observe(this) { completed ->
            completedTasksAdapter.submitList(completed)
        }

        // Các observer đếm số lượng (Giữ nguyên)
        taskViewModel.uncompletedCount.observe(this, Observer { count ->
            binding.taskNeedCompleteTV.text = count.toString()
        })
        taskViewModel.completedCount.observe(this, Observer { count ->
            binding.taskCompleted.text = count.toString()
        })

        // Thêm task nhanh (Giữ nguyên)
        binding.addTaskEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val title = binding.addTaskEditText.text.toString().trim()
                if (title.isNotEmpty()) {
                    taskViewModel.addNewTask(
                        title = title,
                        estimatedPomodoros = 1,
                        priority = TaskPriority.NONE,
                        dueDate = null
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