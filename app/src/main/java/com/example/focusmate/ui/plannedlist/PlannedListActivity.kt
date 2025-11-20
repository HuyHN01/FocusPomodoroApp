package com.example.focusmate.ui.plannedlist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.focusmate.data.local.entity.TaskEntity
import com.example.focusmate.data.local.entity.TaskPriority
import com.example.focusmate.data.local.entity.TaskStatus
import com.example.focusmate.databinding.ActivityTodolistBinding
import com.example.focusmate.databinding.ActivityTodolisttomorrowBinding // <--- Đổi Binding theo tên file XML của bạn
import com.example.focusmate.ui.pomodoro.PomodoroActivity
import com.example.focusmate.ui.todolist.AddTaskListener
import com.example.focusmate.ui.todolist.TaskDetailActivity
import com.example.focusmate.ui.todolist.TaskViewModel
import com.example.focusmate.ui.weeklist.WeekListItem
import com.example.focusmate.ui.weeklist.WeekTaskAdapter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PlannedListActivity : AppCompatActivity(), AddTaskListener {

    // Binding này tương ứng với file xml bạn copy (ví dụ: activity_planned_list.xml)
    private lateinit var binding: ActivityTodolisttomorrowBinding
    private val viewModel: TaskViewModel by viewModels()

    private lateinit var pendingAdapter: WeekTaskAdapter
    private lateinit var completedAdapter: WeekTaskAdapter

    private var isCompletedListVisible = false

    override fun onTaskAddedFromFragment(title: String, pomodoros: Int, priority: TaskPriority, dueDate: Long?, projectId: String?) {
        viewModel.addNewTask(
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

        binding = ActivityTodolisttomorrowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerViews()
        observeData()

    }

    private fun setupUI() {
        binding.headerTitle.text = "Đã lên kế hoạch"

        binding.backArrow.setOnClickListener { finish() }

        binding.completedTasksHeader.setOnClickListener {
            toggleCompletedList()
        }

        binding.completedTasksHeader.visibility = View.GONE

        binding.addTaskLayout.setOnClickListener {
            binding.addTaskFragment.visibility = View.VISIBLE
            viewModel.startAddTask()
        }

        binding.addTaskEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val title = binding.addTaskEditText.text.toString().trim()
                if (title.isNotEmpty()) {
                    // Thêm task mới (mặc định là hôm nay nếu add nhanh)
                    viewModel.addNewTask(
                        title = title,
                        estimatedPomodoros = 1,
                        priority = TaskPriority.NONE,
                        projectId = "inbox_id_placeholder",
                        dueDate = System.currentTimeMillis()
                    )
                    binding.addTaskEditText.text.clear()
                    binding.addTaskEditText.clearFocus()
                    hideKeyboard()
                }
                true
            } else {
                false
            }
        }

        // 6. Xử lý Focus EditText -> Mở Fragment
        binding.addTaskEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.addTaskFragment.visibility = View.VISIBLE
                viewModel.startAddTask()
            } else {
                binding.addTaskFragment.visibility = View.GONE
            }
        }
    }

    private fun setupRecyclerViews() {
        pendingAdapter = WeekTaskAdapter(
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
            layoutManager = LinearLayoutManager(this@PlannedListActivity)
            adapter = pendingAdapter
            isNestedScrollingEnabled = false
        }

        completedAdapter = WeekTaskAdapter(
            onTaskClick = { task ->
                val intent = Intent(this, TaskDetailActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", task.taskId)
                }
                startActivity(intent)
            },
            onCompleteClick = { task -> viewModel.toggleTaskCompletion(task.taskId) },
            onPlayClick = {}
        )
        binding.completedTasksList.apply {
            layoutManager = LinearLayoutManager(this@PlannedListActivity)
            adapter = completedAdapter
            isNestedScrollingEnabled = false
            visibility = View.GONE // Mặc định ẩn
        }
    }

    private fun observeData() {
        viewModel.allTasks.observe(this) { allTasks ->

            val plannedTasks = allTasks.filter { it.dueDate != null }

            val pendingTasks = plannedTasks.filter { it.status == TaskStatus.PENDING }
            val completedTasks = plannedTasks.filter { it.status == TaskStatus.COMPLETED }

            updateStats(plannedTasks)

            if (plannedTasks.isEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.tasksContainer.visibility = View.GONE
            } else {
                binding.emptyStateLayout.visibility = View.GONE
                binding.tasksContainer.visibility = View.VISIBLE
            }

            val groupedPending = groupTasksByDate(pendingTasks)
            pendingAdapter.submitList(groupedPending) {
                binding.tasksList.requestLayout()
            }

            val groupedCompleted = groupTasksByDate(completedTasks)
            completedAdapter.submitList(groupedCompleted) {
                binding.completedTasksList.requestLayout()
            }

            val arrow = if (isCompletedListVisible) "▼" else "▲"
            binding.completedTasksHeader.text = "Đã hoàn thành (${completedTasks.size}) $arrow"
        }
    }

    private fun groupTasksByDate(tasks: List<TaskEntity>): List<WeekListItem> {
        if (tasks.isEmpty()) return emptyList()

        val sortedTasks = tasks.sortedBy { it.dueDate }
        val resultList = mutableListOf<WeekListItem>()
        val dateMap = mutableMapOf<String, MutableList<TaskEntity>>()

        for (task in sortedTasks) {
            val dateKey = getHeaderTitle(task.dueDate)
            if (!dateMap.containsKey(dateKey)) {
                dateMap[dateKey] = mutableListOf()
            }
            dateMap[dateKey]?.add(task)
        }

        for ((headerTitle, taskList) in dateMap) {
            val totalMinutes = taskList.sumOf { it.estimatedPomodoros } * 25
            resultList.add(WeekListItem.Header("$headerTitle • ${totalMinutes}ph"))
            resultList.addAll(taskList.map { WeekListItem.TaskItem(it) })
        }
        return resultList
    }

    private fun getHeaderTitle(timestamp: Long?): String {
        if (timestamp == null) return "Chưa đặt lịch"

        val taskDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            isSameDay(taskDate, today) -> "Hôm nay"
            isSameDay(taskDate, tomorrow) -> "Ngày mai"
            isSameDay(taskDate, yesterday) -> "Hôm qua"
            else -> {
                // Format: Thứ Sáu, 22 thg 11, 2025
                val sdf = SimpleDateFormat("EEEE, dd 'thg' MM, yyyy", Locale("vi", "VN"))
                sdf.format(taskDate.time)
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun toggleCompletedList() {
        isCompletedListVisible = !isCompletedListVisible
        binding.completedTasksList.visibility = if (isCompletedListVisible) View.VISIBLE else View.GONE
        val count = completedAdapter.currentList.count { it is WeekListItem.TaskItem }
        val arrow = if (isCompletedListVisible) "▼" else "▲"
        binding.completedTasksHeader.text = "Đã hoàn thành ($count) $arrow"
    }

    private fun updateStats(tasks: List<TaskEntity>) {
        val totalTasks = tasks.size
        val completed = tasks.count { it.status == TaskStatus.COMPLETED }
        val totalPomo = tasks.sumOf { it.estimatedPomodoros }
        val minutes = totalPomo * 25

        binding.taskNeedCompleteTV.text = "${totalTasks - completed}"
        binding.estimatedTimeTv.text = String.format("%02d:%02d", minutes / 60, minutes % 60)
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}