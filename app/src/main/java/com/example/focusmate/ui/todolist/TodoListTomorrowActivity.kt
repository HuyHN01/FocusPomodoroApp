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
import com.example.focusmate.data.local.entity.TaskStatus 
import com.example.focusmate.ui.pomodoro.PomodoroActivity
import com.example.focusmate.ui.pomodoro.PomodoroViewModel
import java.util.Calendar

class TodoListTomorrowActivity : AppCompatActivity(), AddTaskListener {

    private lateinit var binding: ActivityTodolisttomorrowBinding
    private lateinit var tomorrowViewModel: TomorrowViewModel
    private lateinit var pomodoroViewModel: PomodoroViewModel

    
    private lateinit var tasksAdapter: TasksAdapter
    private lateinit var completedTasksAdapter: TasksAdapter

    
    override fun onTaskAddedFromFragment(title: String, pomodoros: Int, priority: TaskPriority, dueDate: Long?, projectId: String?) {
        tomorrowViewModel.addNewTask(
            title = title,
            estimatedPomodoros = pomodoros,
            priority = priority,
            dueDate = dueDate,
            projectId = projectId
        )

        binding.addTaskFragment.visibility = View.GONE
        binding.addTaskEditText.clearFocus()
        hideKeyboard()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        binding = ActivityTodolisttomorrowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        
        tomorrowViewModel = ViewModelProvider(this)[TomorrowViewModel::class.java]
        pomodoroViewModel = ViewModelProvider(this)[PomodoroViewModel::class.java]

        setupAdapters() 
        setupRecyclerViews() 
        setupUIListeners() 
        observeData() 
    }

    

    private fun setupAdapters() {
        
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
            onPlayClick = {  }
        )
    }

    private fun setupRecyclerViews() {
        
        binding.tasksList.apply {
            layoutManager = LinearLayoutManager(this@TodoListTomorrowActivity)
            adapter = tasksAdapter
            isNestedScrollingEnabled = false 
        }

        
        binding.completedTasksList.apply {
            layoutManager = LinearLayoutManager(this@TodoListTomorrowActivity)
            adapter = completedTasksAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupUIListeners() {
        
        binding.backArrow.setOnClickListener { finish() }

        
        binding.completedTasksHeader.setOnClickListener {
            if (binding.completedTasksList.visibility == View.VISIBLE) {
                binding.completedTasksList.visibility = View.GONE
                binding.completedTasksHeader.text = "Hiển thị những công việc đã hoàn thành ▼"
            } else {
                binding.completedTasksList.visibility = View.VISIBLE
                binding.completedTasksHeader.text = "Ẩn đi những công việc đã hoàn thành ▲"
            }
        }

        
        binding.addTaskEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val title = binding.addTaskEditText.text.toString().trim()
                if (title.isNotEmpty()) {
                    tomorrowViewModel.addNewTask(
                        title = title,
                        estimatedPomodoros = 1,
                        priority = TaskPriority.NONE,
                        dueDate = null, 
                        projectId = null
                    )
                    binding.addTaskEditText.text.clear()
                    binding.addTaskEditText.clearFocus()
                }
                true
            } else {
                false
            }
        }

        
        binding.addTaskEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val tomorrowTimestamp = getTomorrowTimestamp()
                val addTaskFragment = AddTaskFragment.newInstance(tomorrowTimestamp)

                supportFragmentManager.beginTransaction()
                    .replace(binding.addTaskFragment.id, addTaskFragment)
                    .commit()

                binding.addTaskFragment.visibility = View.VISIBLE
            } else {
                binding.addTaskFragment.visibility = View.GONE
            }
        }
    }

    
    private fun observeData() {
        
        tomorrowViewModel.uncompletedTasks.observe(this) { uncompleted ->
            tasksAdapter.submitList(uncompleted)


            if (uncompleted.isEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.tasksContainer.visibility = View.GONE
            } else {
                binding.emptyStateLayout.visibility = View.GONE
                binding.tasksContainer.visibility = View.VISIBLE
            }
        }

        tomorrowViewModel.completedTasks.observe(this) { completed ->
            completedTasksAdapter.submitList(completed)

            
            val completedCount = completed.size
            val arrow = if (binding.completedTasksList.visibility == View.VISIBLE) "▲" else "▼"
            binding.completedTasksHeader.text = "Đã hoàn thành ($completedCount) $arrow"
        }

        
        tomorrowViewModel.uncompletedCount.observe(this) { count ->
            binding.taskNeedCompleteTV.text = count.toString()
        }
        tomorrowViewModel.estimatedTimeFormatted.observe(this) { time ->
            binding.estimatedTimeTv.text = time
        }

        
    }

    
    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun getTomorrowTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}