package com.example.focusmate.ui.completedlist

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.focusmate.data.local.entity.TaskEntity
import com.example.focusmate.data.local.entity.TaskStatus
import com.example.focusmate.databinding.ActivityTodolisttomorrowBinding // Tái sử dụng layout Planned
import com.example.focusmate.ui.todolist.TaskDetailActivity
import com.example.focusmate.ui.todolist.TaskViewModel
import com.example.focusmate.ui.weeklist.WeekListItem
import com.example.focusmate.ui.weeklist.WeekTaskAdapter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CompletedHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTodolisttomorrowBinding
    private val viewModel: TaskViewModel by viewModels()
    private lateinit var adapter: WeekTaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTodolisttomorrowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        observeData()
    }

    private fun setupUI() {
        // 1. Đặt tiêu đề
        binding.headerTitle.text = "Đã hoàn thành"

        // 2. Nút Back
        binding.backArrow.setOnClickListener { finish() }


        binding.statsLayout.visibility = View.GONE

        binding.addTaskLayout.visibility = View.GONE

        binding.completedTasksHeader.visibility = View.GONE
        binding.completedTasksList.visibility = View.GONE

        // Đảm bảo container chính hiện lên
        binding.tasksContainer.visibility = View.VISIBLE
        binding.tasksList.visibility = View.VISIBLE
    }

    private fun setupRecyclerView() {
        adapter = WeekTaskAdapter(
            onTaskClick = { task ->
                // Mở xem chi tiết (nếu muốn)
                val intent = Intent(this, TaskDetailActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", task.taskId)
                }
                startActivity(intent)
            },
            onCompleteClick = { task ->
                // Nếu bỏ tích -> Nó sẽ biến mất khỏi list này (vì thành PENDING)
                viewModel.toggleTaskCompletion(task.taskId)
            },
            onPlayClick = {
                // Task đã xong thường không hiện nút play, adapter tự xử lý ẩn rồi
            }
        )

        binding.tasksList.apply {
            layoutManager = LinearLayoutManager(this@CompletedHistoryActivity)
            adapter = this@CompletedHistoryActivity.adapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observeData() {
        viewModel.allTasks.observe(this) { allTasks ->

            // === LOGIC LỌC DỮ LIỆU ===
            val historyTasks = allTasks.filter { task ->
                // 1. Phải là ĐÃ HOÀN THÀNH
                // 2. Phải có ngày giờ (dueDate != null)
                task.status == TaskStatus.COMPLETED && task.dueDate != null
            }


            val sortedTasks = historyTasks.sortedByDescending { it.dueDate }

            val groupedList = groupTasksByDate(sortedTasks)

            adapter.submitList(groupedList) {
                binding.tasksList.requestLayout()
            }

            if (groupedList.isEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.tasksList.visibility = View.GONE


            } else {
                binding.emptyStateLayout.visibility = View.GONE
                binding.tasksList.visibility = View.VISIBLE
            }
        }
    }

    private fun groupTasksByDate(tasks: List<TaskEntity>): List<WeekListItem> {
        if (tasks.isEmpty()) return emptyList()

        val resultList = mutableListOf<WeekListItem>()
        // Dùng LinkedHashMap để giữ thứ tự sắp xếp
        val dateMap = LinkedHashMap<String, MutableList<TaskEntity>>()

        for (task in tasks) {
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
        if (timestamp == null) return "Không xác định"
        val taskDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            isSameDay(taskDate, today) -> "Hôm nay"
            isSameDay(taskDate, yesterday) -> "Hôm qua"
            else -> {
                val sdf = SimpleDateFormat("EEEE, dd 'thg' MM, yyyy", Locale("vi", "VN"))
                sdf.format(taskDate.time)
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}