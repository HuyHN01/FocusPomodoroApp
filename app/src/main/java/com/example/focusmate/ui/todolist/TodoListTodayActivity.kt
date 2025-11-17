package com.example.focusmate.ui.todolist

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
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


class TodoListTodayActivity : AppCompatActivity(){
    private lateinit var binding: ActivityTodolistBinding
    private lateinit var taskViewModel: TaskViewModel
    private lateinit var pomodoroViewModel: PomodoroViewModel
    private lateinit var tasksAdapter: TasksAdapter
    private lateinit var completedTasksAdapter: TasksAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityTodolistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        taskViewModel = ViewModelProvider(this)[TaskViewModel::class.java]
        pomodoroViewModel = ViewModelProvider(this)[PomodoroViewModel::class.java]


        binding.completedTasksHeader.setOnClickListener {
            if (binding.completedTasksList.isVisible) {
                binding.completedTasksList.visibility = View.GONE
                binding.completedTasksHeader.text = "Hiển thị những công việc đã hoàn thành ▼"
            } else {
                binding.completedTasksList.visibility = View.VISIBLE
                binding.completedTasksHeader.text = "Ẩn đi những công việc đã hoàn thành ▲"
            }
        }

        // Khởi tạo Adapter với các callback
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
                    // SỬA LỖI 1: Sửa 'id' thành 'taskId'
                    putExtra("EXTRA_TASK_ID", task.taskId)
                }
                startActivity(intent)
            }
        )

        binding.tasksList.apply {
            layoutManager = LinearLayoutManager(this@TodoListTodayActivity)
            adapter = tasksAdapter
        }
        binding.completedTasksList.apply {
            layoutManager = LinearLayoutManager(this@TodoListTodayActivity)
            adapter = completedTasksAdapter
        }

        taskViewModel.uncompletedTasks.observe(this) { uncompleted ->
            tasksAdapter.submitList(uncompleted)
        }

        taskViewModel.completedTasks.observe(this) { completed ->
            completedTasksAdapter.submitList(completed)
        }
//

        taskViewModel.uncompletedCount.observe(this, Observer { count ->
            binding.taskNeedCompleteTV.text = count.toString()
        })
        taskViewModel.completedCount.observe(this, Observer { count ->
            binding.taskCompleted.text = count.toString()
        })

        binding.addTaskEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val title = binding.addTaskEditText.text.toString().trim()
                if (title.isNotEmpty()) {
                    // Sửa lỗi: Thêm 'priority = TaskPriority.NONE'
                    taskViewModel.addNewTask(
                        title = title,
                        estimatedPomodoros = 1,
                        priority = TaskPriority.NONE, // <-- THÊM THAM SỐ NÀY
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

}