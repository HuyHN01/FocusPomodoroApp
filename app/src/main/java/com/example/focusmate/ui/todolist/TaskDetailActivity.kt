package com.example.focusmate.ui.todolist

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.example.focusmate.R
import com.example.focusmate.data.local.entity.TaskPriority
import com.example.focusmate.data.local.entity.TaskStatus
import com.example.focusmate.databinding.ActivityTaskDetailBinding
import com.example.focusmate.ui.pomodoro.PomodoroActivity

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskDetailBinding
    private lateinit var taskViewModel: TaskViewModel
    private var currentTaskId: String? = null

    private var isObserving = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        taskViewModel = ViewModelProvider(this).get(TaskViewModel::class.java)

        currentTaskId = intent.getStringExtra("EXTRA_TASK_ID")

        currentTaskId?.let { safeTaskId ->
            
            taskViewModel.loadTaskById(safeTaskId)
        } ?: run {
            
            finish()
            return
        }
        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        taskViewModel.currentTask.asLiveData().observe(this) { task ->
            if (task != null) {
                isObserving = true

                
                binding.edittextTaskName.setText(task.title)

                
                if (task.status == TaskStatus.COMPLETED) {
                    
                    binding.checkboxComplete.isChecked = true

                    binding.checkboxComplete.setButtonDrawable(R.drawable.green_checkmark_icon)

                    binding.checkboxComplete.buttonTintList = null

                    binding.edittextTaskName.paintFlags = binding.edittextTaskName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    
                    binding.edittextTaskName.setTextColor(ContextCompat.getColor(this, R.color.priority_none))
                } else {
                    binding.checkboxComplete.isChecked = false
                    binding.checkboxComplete.setButtonDrawable(R.drawable.ellipse_shape_line_icon)

                    binding.edittextTaskName.paintFlags = binding.edittextTaskName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    
                    binding.edittextTaskName.setTextColor(ContextCompat.getColor(this, R.color.black))

                    
                    val priorityColorRes = when (task.priority) {
                        TaskPriority.HIGH -> R.color.priority_high
                        TaskPriority.MEDIUM -> R.color.priority_medium
                        TaskPriority.LOW -> R.color.priority_low
                        TaskPriority.NONE -> R.color.priority_none
                    }
                    val priorityColor = ContextCompat.getColor(this, priorityColorRes)

                    binding.checkboxComplete.buttonTintList = android.content.res.ColorStateList.valueOf(priorityColor)
                }
                binding.tvPomodoroValue.text = "${task.estimatedPomodoros} Pomodoro"
                
                binding.edittextNote.setText(task.note)
                

                
                val flagColor = when (task.priority) {
                    TaskPriority.HIGH -> R.color.priority_high
                    TaskPriority.MEDIUM -> R.color.priority_medium
                    TaskPriority.LOW -> R.color.priority_low
                    TaskPriority.NONE -> R.color.priority_none
                }
                binding.imageviewFlag.setColorFilter(
                    ContextCompat.getColor(this, flagColor)
                )

                binding.btnRemoveDueDate.setOnClickListener {
                    if (!isObserving) {
                        
                        taskViewModel.updateTaskDueDate(null)
                    }
                }

                if (task.dueDate != null) {
                    
                    binding.tvDueDateValue.text = formatDate(task.dueDate!!) 
                    binding.tvDueDateValue.setTextColor(ContextCompat.getColor(this, R.color.red_pomodoro))
                    binding.btnRemoveDueDate.visibility = View.VISIBLE
                } else {
                    
                    binding.tvDueDateValue.text = "Hôm nay"
                    binding.tvDueDateValue.setTextColor(ContextCompat.getColor(this, R.color.black))
                    binding.btnRemoveDueDate.visibility = View.GONE
                }
                
                binding.rowDueDate.setOnClickListener {
                    if (!isObserving) {
                        val currentDate = taskViewModel.currentTask.value?.dueDate

                        
                        val dateDialog = DatePickerDialogFragment(currentDate) { newTimestamp ->
                            taskViewModel.updateTaskDueDate(newTimestamp)
                        }

                        dateDialog.show(supportFragmentManager, "DatePickerDialog")
                    }
                }
                taskViewModel.allProjects.observe(this) { projects ->
                    
                    val currentProject = projects.find { it.projectId == task.projectId }

                    if (currentProject != null) {
                        binding.tvProjectValue.text = currentProject.name
                    } else {
                        
                        binding.tvProjectValue.text = "Nhiệm vụ"
                        binding.iconProject.imageTintList = ColorStateList.valueOf(Color.GRAY)
                    }
                }

                isObserving = false

            } else {
                if (!isObserving) finish()
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("EEE, dd 'thg' MM yyyy", java.util.Locale("vi", "VN"))
        return sdf.format(java.util.Date(timestamp))
    }
    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.checkboxComplete.setOnClickListener {
            
            currentTaskId?.let { safeTaskId ->
                taskViewModel.toggleTaskCompletion(safeTaskId)
            }
        }

        
        binding.imageviewFlag.setOnClickListener { view ->
            if (!isObserving) {

                showPriorityPopup(view)
            }
        }

        binding.rowPomodoro.setOnClickListener {
            if (!isObserving) {
                
                val currentCount = taskViewModel.currentTask.value?.estimatedPomodoros ?: 1

                
                val dialog = PomodoroCountPickerFragment(currentCount) { newCount ->
                    taskViewModel.updateTaskPomodoros(newCount)
                }

                
                dialog.show(supportFragmentManager, "PomoCountPicker")
            }
        }
        binding.rowProject.setOnClickListener {
            if (!isObserving) {
                
                ProjectPickerDialogFragment().show(supportFragmentManager, "ProjectPicker")
            }
        }
    }

    private fun showPriorityPopup(anchorView: View) {
        
        val popupBinding = com.example.focusmate.databinding.DialogPrioritySelectionBinding.inflate(layoutInflater)

        
        val popupWindow = android.widget.PopupWindow(
            popupBinding.root,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 
            true
        )

        popupWindow.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        popupWindow.elevation = 10f

        popupBinding.itemPriorityHigh.setOnClickListener {
            taskViewModel.updateTaskPriority(com.example.focusmate.data.local.entity.TaskPriority.HIGH)
            popupWindow.dismiss()
        }

        popupBinding.itemPriorityMedium.setOnClickListener {
            taskViewModel.updateTaskPriority(com.example.focusmate.data.local.entity.TaskPriority.MEDIUM)
            popupWindow.dismiss()
        }

        popupBinding.itemPriorityLow.setOnClickListener {
            taskViewModel.updateTaskPriority(com.example.focusmate.data.local.entity.TaskPriority.LOW)
            popupWindow.dismiss()
        }

        popupBinding.itemPriorityNone.setOnClickListener {
            taskViewModel.updateTaskPriority(com.example.focusmate.data.local.entity.TaskPriority.NONE)
            popupWindow.dismiss()
        }

        
        
        popupWindow.showAsDropDown(anchorView, 0, 0)
    }

    override fun onPause() {
        super.onPause()
        saveTaskChanges()
    }

    private fun saveTaskChanges() {
        if (currentTaskId == null) return

        val newTitle = binding.edittextTaskName.text.toString().trim()

        val newNote = binding.edittextNote.text.toString().trim()

        val currentTask = taskViewModel.currentTask.value
        val currentEstimated = currentTask?.estimatedPomodoros ?: 1

        if (newTitle.isNotEmpty()) {
            taskViewModel.updateCurrentTask(
                newTitle = newTitle,
                newEstimatedPomodoros = currentEstimated,
                newNote = newNote
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_task_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_play -> {
                val intent = Intent(this, PomodoroActivity::class.java)
                intent.putExtra("EXTRA_TASK_ID", currentTaskId)
                startActivity(intent)
                true
            }
            R.id.action_delete -> {
                taskViewModel.deleteTask()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}