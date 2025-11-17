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
            // 'safeTaskId' là một bản sao String an toàn, không null
            taskViewModel.loadTaskById(safeTaskId)
        } ?: run {
            // Khối 'run' này sẽ chạy nếu 'currentTaskId' là null
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

                // 1. Hiển thị Tiêu đề
                binding.edittextTaskName.setText(task.title)

                // 2. Hiển thị Trạng thái (Checkbox & Gạch ngang)
                binding.checkboxComplete.isChecked = (task.status == TaskStatus.COMPLETED)
                if (task.status == TaskStatus.COMPLETED) {
                    binding.edittextTaskName.paintFlags = binding.edittextTaskName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    binding.edittextTaskName.paintFlags = binding.edittextTaskName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
                binding.tvPomodoroValue.text = "${task.estimatedPomodoros} Pomodoro"
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

                binding.btnRemoveDueDate.setOnClickListener {
                    if (!isObserving) {
                        // Gọi hàm mới, truyền 'null' để xóa ngày
                        taskViewModel.updateTaskDueDate(null)
                    }
                }

                if (task.dueDate != null) {
                    // Nếu CÓ ngày -> Hiển thị ngày (màu đỏ) và hiện nút X
                    binding.tvDueDateValue.text = formatDate(task.dueDate!!) // Gọi hàm formatDate em đã tạo
                    binding.tvDueDateValue.setTextColor(ContextCompat.getColor(this, R.color.red_pomodoro))
                    binding.btnRemoveDueDate.visibility = View.VISIBLE
                } else {
                    // Nếu KHÔNG có ngày (null) -> Hiển thị "Không" (màu đen) và ẩn nút X
                    binding.tvDueDateValue.text = "Không"
                    binding.tvDueDateValue.setTextColor(ContextCompat.getColor(this, R.color.black))
                    binding.btnRemoveDueDate.visibility = View.GONE
                }
                // 2. Click vào CẢ HÀNG "Ngày đến hạn"
                binding.rowDueDate.setOnClickListener {
                    if (!isObserving) {
                        val currentDate = taskViewModel.currentTask.value?.dueDate

                        // Tái sử dụng Dialog Lịch
                        val dateDialog = DatePickerDialogFragment(currentDate) { newTimestamp ->
                            taskViewModel.updateTaskDueDate(newTimestamp)
                        }

                        dateDialog.show(supportFragmentManager, "DatePickerDialog")
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

        binding.checkboxComplete.setOnCheckedChangeListener { _, _ ->
            currentTaskId?.let { safeTaskId ->
                taskViewModel.toggleTaskCompletion(safeTaskId)
            }
        }

        // Click vào cờ để đổi độ ưu tiên (Xoay vòng)
        binding.imageviewFlag.setOnClickListener { view ->
            if (!isObserving) {

                showPriorityPopup(view)
            }
        }

        binding.rowPomodoro.setOnClickListener {
            if (!isObserving) {
                // 1. Lấy số Pomo hiện tại
                val currentCount = taskViewModel.currentTask.value?.estimatedPomodoros ?: 1

                // 2. Tạo Dialog MỚI (PomodoroCountPickerFragment)
                val dialog = PomodoroCountPickerFragment(currentCount) { newCount ->
                    // 3. Khi Dialog trả về số Pomo mới, gọi ViewModel
                    // (Hàm này trong ViewModel chúng ta đã viết rồi)
                    taskViewModel.updateTaskPomodoros(newCount)
                }

                // 4. Hiển thị Dialog
                dialog.show(supportFragmentManager, "PomoCountPicker")
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