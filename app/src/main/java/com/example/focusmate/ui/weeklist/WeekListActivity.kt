package com.example.focusmate.ui.weeklist

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.focusmate.data.local.entity.TaskEntity
import com.example.focusmate.data.local.entity.TaskStatus
import com.example.focusmate.databinding.ActivityTodolistBinding
import com.example.focusmate.ui.pomodoro.PomodoroActivity
import com.example.focusmate.ui.todolist.TaskDetailActivity
import com.example.focusmate.ui.todolist.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WeekListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTodolistBinding
    private val viewModel: TaskViewModel by viewModels()

    private lateinit var pendingAdapter: WeekTaskAdapter
    private lateinit var completedAdapter: WeekTaskAdapter

    private var isCompletedListVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Tái sử dụng layout cũ
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

        binding.addTaskLayout.setOnClickListener {

            binding.addTaskFragment.visibility = View.VISIBLE
            viewModel.startAddTask() // Reset dữ liệu thêm mới
        }
    }

    private fun setupRecyclerViews() {
        // --- LIST CHƯA HOÀN THÀNH (Pending) ---
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
            visibility = View.GONE // Mặc định ẩn
        }
    }

    private fun observeData() {
        val (startOfWeek, endOfWeek) = getCurrentWeekRange()

        viewModel.allTasks.observe(this) { allTasks ->

            // === LỌC TASK CHỈ TRONG TUẦN HIỆN TẠI ===
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

        // Sắp xếp theo ngày đến hạn (xa nhất xuống dưới)
        val sortedTasks = tasks.sortedBy { it.dueDate ?: Long.MAX_VALUE }
        val resultList = mutableListOf<WeekListItem>()
        val dateMap = mutableMapOf<String, MutableList<TaskEntity>>()

        // Gom nhóm
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
        if (timestamp == null) return "Chưa đặt lịch" // Hoặc "Sau này"

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
}