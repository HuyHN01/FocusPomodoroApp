package com.example.focusmate.ui.todolist

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.focusmate.databinding.ActivityTodolistBinding
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.focusmate.data.local.entity.TaskPriority
import com.example.focusmate.ui.pomodoro.PomodoroActivity
import com.example.focusmate.ui.pomodoro.PomodoroViewModel
import androidx.core.view.isVisible
import androidx.core.view.updatePadding


class TodoListTodayActivity : AppCompatActivity(), AddTaskListener{
    private lateinit var binding: ActivityTodolistBinding
    private lateinit var taskViewModel: TaskViewModel
    private lateinit var pomodoroViewModel: PomodoroViewModel

    
    private lateinit var tasksAdapter: TasksAdapter 
    private lateinit var otherTasksAdapter: TasksAdapter 
    private lateinit var completedTasksAdapter: TasksAdapter

    override fun onTaskAddedFromFragment(title: String, pomodoros: Int, priority: TaskPriority, date: Long?, projectId: String?) {
        
        taskViewModel.addNewTask(
            title = title,
            estimatedPomodoros = pomodoros,
            priority = priority,
            dueDate = date,
            projectId = projectId 
        )

        
        binding.addTaskEditText.clearFocus()
        hideKeyboard()
        binding.addTaskFragment.visibility = View.GONE
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityTodolistBinding.inflate(layoutInflater)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )

        

        
        
        val originalPaddingTop = 16.dpToPx()

        ViewCompat.setOnApplyWindowInsetsListener(binding.headerLayout) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.updatePadding(
                top = originalPaddingTop + bars.top
            )
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollView) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                bottom = bars.bottom
            )
            insets
        }

        
        

        ViewCompat.setOnApplyWindowInsetsListener(binding.addTaskFragment) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            
            view.updatePadding(
                bottom = bars.bottom
            )
            insets
        }

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
        binding.backArrow.setOnClickListener {
            finish()
        }
        taskViewModel.timeElapsedFormatted.observe(this) { formattedTime ->
            binding.tvTotalTimeElapsed.text = formattedTime 
        }


        

        
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
                    putExtra("EXTRA_TASK_ID", task.taskId)
                }
                startActivity(intent)
            }
        )

        
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

        
        binding.tasksList.apply { 
            layoutManager = LinearLayoutManager(this@TodoListTodayActivity)
            adapter = tasksAdapter
        }
        binding.completedTasksList.apply { 
            layoutManager = LinearLayoutManager(this@TodoListTodayActivity)
            adapter = completedTasksAdapter
        }


        

        
        taskViewModel.estimatedTimeFormatted.observe(this) { formattedTime ->
            binding.tvTotalEstimatedTime.text = formattedTime
        }

        
        taskViewModel.uncompletedTasks.observe(this) { uncompletedToday ->
            tasksAdapter.submitList(uncompletedToday)
        }

        




        
        taskViewModel.completedTasks.observe(this) { completed ->
            completedTasksAdapter.submitList(completed)
        }

        
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
                    taskViewModel.addNewTask(
                        title = title,
                        estimatedPomodoros = 1,
                        priority = TaskPriority.NONE,
                        projectId = "inbox_id_placeholder",
                        dueDate = System.currentTimeMillis()
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

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}