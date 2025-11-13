package com.example.focusmate.ui.todolist

import android.content.Intent
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
    private var currentTaskId: Int = -1

    private var isObserving = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        taskViewModel = ViewModelProvider(this).get(TaskViewModel::class.java)

        currentTaskId = intent.getIntExtra("EXTRA_TASK_ID", -1)
        if (currentTaskId == -1) {
            finish()
            return
        }

        taskViewModel.loadTaskById(currentTaskId)
        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        taskViewModel.currentTask.asLiveData().observe(this) { task ->
            if (task != null) {
                isObserving = true

                // 1. Hiển thị Tiêu đề
                binding.edittextTaskName.setText(task.title)

                // 2. Hiển thị Trạng thái (Checkbox & Gạch ngang)
                binding.checkboxComplete.isChecked = (task.status == TaskStatus.COMPLETED)
                if (task.status == TaskStatus.COMPLETED) {
                    binding.edittextTaskName.paintFlags = binding.edittextTaskName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    binding.edittextTaskName.paintFlags = binding.edittextTaskName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }

                // 3. --- PHẦN MỚI: HIỂN THỊ GHI CHÚ ---
                binding.edittextNote.setText(task.note)
                // ------------------------------------

                // 4. Hiển thị Màu cờ ưu tiên
                val flagColor = when (task.priority) {
                    TaskPriority.HIGH -> R.color.priority_high
                    TaskPriority.MEDIUM -> R.color.priority_medium
                    TaskPriority.LOW -> R.color.priority_low
                    TaskPriority.NONE -> R.color.priority_none
                }
                binding.imageviewFlag.setColorFilter(
                    ContextCompat.getColor(this, flagColor)
                )

                isObserving = false
            } else {
                if (!isObserving) finish()
            }
        }
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.checkboxComplete.setOnCheckedChangeListener { _, _ ->
            if (!isObserving) {
                taskViewModel.toggleTaskCompletion(currentTaskId)
            }
        }

        // Click vào cờ để đổi độ ưu tiên (Xoay vòng)
        binding.imageviewFlag.setOnClickListener { view ->
            if (!isObserving) {

                showPriorityPopup(view)
            }
        }
    }

    private fun showPriorityPopup(anchorView: View) {
        // 1. Khởi tạo Layout (Dùng lại file XML em đã tạo)
        val popupBinding = com.example.focusmate.databinding.DialogPrioritySelectionBinding.inflate(layoutInflater)

        // 2. Tạo PopupWindow
        val popupWindow = android.widget.PopupWindow(
            popupBinding.root,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT, // Chiều rộng tự động
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT, // Chiều cao tự động
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

        // 4. HIỂN THỊ: Ngay bên dưới nút cờ (anchorView), lệch xuống 1 chút (yOffset)
        // xoff = 0, yoff = -20 (để nó đè lên một chút cho đẹp, hoặc để 0)
        popupWindow.showAsDropDown(anchorView, 0, 0)
    }

    override fun onPause() {
        super.onPause()
        saveTaskChanges()
    }

    private fun saveTaskChanges() {
        if (currentTaskId == -1) return

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