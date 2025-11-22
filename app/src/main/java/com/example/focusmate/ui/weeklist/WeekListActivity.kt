package com.example.focusmate.ui.weeklist

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
import com.example.focusmate.ui.pomodoro.PomodoroActivity
import com.example.focusmate.ui.todolist.AddTaskListener // Import Listener
import com.example.focusmate.ui.todolist.TaskDetailActivity
import com.example.focusmate.ui.todolist.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WeekListActivity : AppCompatActivity(), AddTaskListener {

    private lateinit var binding: ActivityTodolistBinding
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

        binding = ActivityTodolistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerViews()
        observeData()
    }

    private fun setupUI() {
        binding.headerTitle.text = "Tuần này"

        binding.backArrow.setOnClickListener { finish() }

        binding.completedTasksHeader.setOnClickListener {
            toggleCompletedList()
        }

        binding.addTaskEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                val title = binding.addTaskEditText.text.toString().trim()
                if (title.isNotEmpty()) {
                    viewModel.addNewTask(
                        title = title,
                        estimatedPomodoros = 1,
                        priority = TaskPriority.NONE,
                        projectId = "inbox_id_placeholder",
                        dueDate = System.currentTimeMillis() // Ngày hôm nay
                    )
                    binding.addTaskEditText.text.clear()
                    binding.addTaskEditText.clearFocus()
                }
                true
            } else {
                false
            }
        }

        binding.addTaskLayout.setOnClickListener {
            binding.addTaskFragment.visibility = View.VISIBLE
            viewModel.startAddTask()
        }

        binding.addTaskEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                viewModel.startAddTask()
                binding.addTaskFragment.visibility = View.VISIBLE
            } else {
                // Khi mất focus, ẩn Fragment đi
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
            onCompleteClick = { task ->
                viewModel.toggleTaskCompletion(task.taskId)
            },
            onPlayClick = { task ->
                val intent = Intent(this, PomodoroActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", task.taskId)
                }
                startActivity(intent)
            }
        )
        binding.tasksList.apply {
            layoutManager = LinearLayoutManager(this@WeekListActivity)
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
            onCompleteClick = { task ->
                viewModel.toggleTaskCompletion(task.taskId)
            },
            onPlayClick = {

            }
        )
        binding.completedTasksList.apply {
            layoutManager = LinearLayoutManager(this@WeekListActivity)
            adapter = completedAdapter
            isNestedScrollingEnabled = false
            visibility = View.GONE
        }
    }

    private fun observeData() {
        val (startOfWeek, endOfWeek) = getCurrentWeekRange()

        viewModel.allTasks.observe(this) { allTasks ->


            val weekTasks = allTasks.filter { task ->

                val isDueInWeek = task.dueDate != null && task.dueDate in startOfWeek..endOfWeek
                val isOverdueAndPending = task.dueDate != null && task.dueDate < startOfWeek && task.status == TaskStatus.PENDING

                isDueInWeek || isOverdueAndPending
            }

            val pendingTasks = weekTasks.filter { it.status == TaskStatus.PENDING }
            val completedTasks = weekTasks.filter { it.status == TaskStatus.COMPLETED }

            updateStats(weekTasks)

            val groupedPending = groupTasksByDate(pendingTasks)
            pendingAdapter.submitList(groupedPending)

            val groupedCompleted = groupTasksByDate(completedTasks)
            completedAdapter.submitList(groupedCompleted)

            val arrow = if (isCompletedListVisible) "▼" else "▲"
            binding.completedTasksHeader.text = "Đã hoàn thành (${completedTasks.size}) $arrow"
        }
    }

    private fun toggleCompletedList() {
        isCompletedListVisible = !isCompletedListVisible
        if (isCompletedListVisible) {
            binding.completedTasksList.visibility = View.VISIBLE
            val count = completedAdapter.currentList.count { it is WeekListItem.TaskItem }
            binding.completedTasksHeader.text = "Đã hoàn thành ($count) ▼"
        } else {
            binding.completedTasksList.visibility = View.GONE
            val count = completedAdapter.currentList.count { it is WeekListItem.TaskItem }
            binding.completedTasksHeader.text = "Đã hoàn thành ($count) ▲"
        }
    }

    // --- CÁC HÀM XỬ LÝ LOGIC NHÓM NGÀY ---

    private fun groupTasksByDate(tasks: List<TaskEntity>): List<WeekListItem> {
        if (tasks.isEmpty()) return emptyList()


        val sortedTasks = tasks.sortedBy { it.dueDate ?: Long.MAX_VALUE }
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
                val sdf = SimpleDateFormat("EEEE, dd 'thg' MM", Locale("vi", "VN"))
                sdf.format(taskDate.time)
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun updateStats(tasks: List<TaskEntity>) {
        val totalTasks = tasks.size
        val completedTasks = tasks.count { it.status == TaskStatus.COMPLETED }
        val totalPomo = tasks.sumOf { it.estimatedPomodoros }
        val estimatedMinutes = totalPomo * 25

        binding.taskNeedCompleteTV.text = "${totalTasks - completedTasks}"
        binding.taskCompleted.text = "$completedTasks"
        binding.tvTotalEstimatedTime.text = String.format("%02d:%02d", estimatedMinutes / 60, estimatedMinutes % 60)
    }
    private fun getCurrentWeekRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfWeek = calendar.timeInMillis

        return Pair(startOfWeek, endOfWeek)
    }

    // Hàm ẩn/hiện bàn phím (Nếu cần thiết)
    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}