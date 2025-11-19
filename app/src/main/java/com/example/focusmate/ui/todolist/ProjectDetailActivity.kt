package com.example.focusmate.ui.todolist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.focusmate.databinding.ActivityTodolistBinding
import com.example.focusmate.data.local.entity.TaskPriority
import com.example.focusmate.ui.pomodoro.PomodoroActivity

class ProjectDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTodolistBinding
    private lateinit var viewModel: ProjectDetailViewModel
    private lateinit var tasksAdapter: TasksAdapter
    private lateinit var completedTasksAdapter: TasksAdapter

    private var currentProjectId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityTodolistBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val projectId = intent.getStringExtra("EXTRA_PROJECT_ID") ?: run { finish(); return }

        val projectName = intent.getStringExtra("EXTRA_PROJECT_NAME") ?: "Dự án"

        currentProjectId = projectId

        viewModel = ViewModelProvider(this)[ProjectDetailViewModel::class.java]
        viewModel.setProjectId(projectId)

        binding.headerTitle.text = projectName

        binding.backArrow.setOnClickListener { finish() }

        tasksAdapter = TasksAdapter(
            onTaskClick = { task ->
                val intent = Intent(this, TaskDetailActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", task.taskId)
                }
                startActivity(intent)
            },
            onCompleteClick = { task -> viewModel.toggleTaskCompletion(task.taskId) },
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
            onCompleteClick = { task -> viewModel.toggleTaskCompletion(task.taskId) },
            onPlayClick = { task ->
                val intent = Intent(this, PomodoroActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", task.taskId)
                }
                startActivity(intent)
            }
        )

        binding.tasksList.apply {
            layoutManager = LinearLayoutManager(this@ProjectDetailActivity)
            adapter = tasksAdapter
        }
        binding.completedTasksList.apply {
            layoutManager = LinearLayoutManager(this@ProjectDetailActivity)
            adapter = completedTasksAdapter
        }

        
        viewModel.uncompletedTasks.observe(this) { tasks -> tasksAdapter.submitList(tasks) }
        viewModel.completedTasks.observe(this) { tasks -> completedTasksAdapter.submitList(tasks) }

        viewModel.uncompletedCount.observe(this) { count -> binding.taskNeedCompleteTV.text = count.toString() }
        viewModel.estimatedTimeFormatted.observe(this) { time -> binding.tvTotalEstimatedTime.text = time }

        binding.addTaskEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val title = binding.addTaskEditText.text.toString().trim()
                if (title.isNotEmpty()) {
                    viewModel.addNewTask(title, 1, TaskPriority.NONE)
                    binding.addTaskEditText.text.clear()
                }
                binding.addTaskEditText.clearFocus()
                hideKeyboard()
                true
            } else { false }
        }

        binding.addTaskEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.addTaskFragment.visibility = View.VISIBLE
            else binding.addTaskFragment.visibility = View.GONE
        }

        binding.completedTasksHeader.setOnClickListener {
            if (binding.completedTasksList.isVisible) {
                binding.completedTasksList.visibility = View.GONE
                binding.completedTasksHeader.text = "Hiển thị những công việc đã hoàn thành ▼"
            } else {
                binding.completedTasksList.visibility = View.VISIBLE
                binding.completedTasksHeader.text = "Ẩn đi những công việc đã hoàn thành ▲"
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