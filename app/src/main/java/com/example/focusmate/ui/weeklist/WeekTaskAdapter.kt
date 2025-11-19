package com.example.focusmate.ui.weeklist

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.focusmate.R
import com.example.focusmate.data.local.entity.TaskEntity
import com.example.focusmate.data.local.entity.TaskPriority
import com.example.focusmate.data.local.entity.TaskStatus
import com.example.focusmate.databinding.ItemTaskBinding
import com.example.focusmate.databinding.ItemWeekHeaderBinding // Đảm bảo em đã tạo file xml header này
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class WeekTaskAdapter(
    private val onTaskClick: (TaskEntity) -> Unit,
    private val onCompleteClick: (TaskEntity) -> Unit,
    private val onPlayClick: (TaskEntity) -> Unit
) : ListAdapter<WeekListItem, RecyclerView.ViewHolder>(WeekListDiffCallback()) {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_TASK = 1
    }

    // Xác định loại view dựa trên data
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is WeekListItem.Header -> TYPE_HEADER
            is WeekListItem.TaskItem -> TYPE_TASK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val binding = ItemWeekHeaderBinding.inflate(inflater, parent, false)
            HeaderViewHolder(binding)
        } else {
            // Inflate layout Task (Tái sử dụng layout cũ)
            val binding = ItemTaskBinding.inflate(inflater, parent, false)
            TaskViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is HeaderViewHolder -> holder.bind(item as WeekListItem.Header)
            is TaskViewHolder -> holder.bind((item as WeekListItem.TaskItem).task)
        }
    }

    // --- VIEWHOLDER CHO HEADER ---
    inner class HeaderViewHolder(private val binding: ItemWeekHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WeekListItem.Header) {
            // Giả sử trong item_week_header.xml em đặt id là tvHeaderTitle
            binding.tvHeaderTitle.text = item.title
        }
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val item = getItem(adapterPosition)
                    if (item is WeekListItem.TaskItem) onTaskClick(item.task)
                }
            }
            binding.taskPlayIcon.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val item = getItem(adapterPosition)
                    if (item is WeekListItem.TaskItem) onPlayClick(item.task)
                }
            }
            binding.taskCompleteIcon.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val item = getItem(adapterPosition)
                    if (item is WeekListItem.TaskItem) onCompleteClick(item.task)
                }
            }
        }

        fun bind(task: TaskEntity) {
            val context = binding.root.context
            binding.taskTitleTextView.text = task.title

            // 1. Chuẩn bị text Pomo
            val pomoText = "${task.estimatedPomodoros} Pomodoro"

            // 2. Màu sắc
            val defaultColor = ContextCompat.getColor(context, R.color.black) // Hoặc màu xám đậm tùy theme
            val overdueColor = ContextCompat.getColor(context, R.color.priority_high)

            // 3. Logic hiển thị ngày và quá hạn
            if (task.dueDate != null) {
                val dateText = formatTimestampToShortDate(task.dueDate!!)
                binding.pomodoroCountTextView.text = "$pomoText • $dateText"

                if (isOverdue(task.dueDate!!)) {
                    binding.pomodoroCountTextView.setTextColor(overdueColor)
                } else {
                    binding.pomodoroCountTextView.setTextColor(defaultColor)
                }
            } else {
                binding.pomodoroCountTextView.text = pomoText
                binding.pomodoroCountTextView.setTextColor(defaultColor)
            }

            // 4. Xử lý hoàn thành / chưa hoàn thành
            if (task.status == TaskStatus.COMPLETED) {
                binding.taskCompleteIcon.setImageResource(R.drawable.green_checkmark_icon)
                binding.taskCompleteIcon.clearColorFilter()
                binding.taskTitleTextView.paintFlags =
                    binding.taskTitleTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.taskPlayIcon.visibility = View.GONE
            } else {
                binding.taskCompleteIcon.setImageResource(R.drawable.ellipse_shape_line_icon)
                binding.taskTitleTextView.paintFlags =
                    binding.taskTitleTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.taskPlayIcon.visibility = View.VISIBLE

                // Tô màu cờ ưu tiên
                val priorityColor = when (task.priority) {
                    TaskPriority.HIGH -> ContextCompat.getColor(context, R.color.priority_high)
                    TaskPriority.MEDIUM -> ContextCompat.getColor(context, R.color.priority_medium)
                    TaskPriority.LOW -> ContextCompat.getColor(context, R.color.priority_low)
                    TaskPriority.NONE -> ContextCompat.getColor(context, R.color.priority_none)
                }
                binding.taskCompleteIcon.setColorFilter(priorityColor)
            }
        }
    }
}

// --- 3. DIFF CALLBACK (So sánh dữ liệu) ---
class WeekListDiffCallback : DiffUtil.ItemCallback<WeekListItem>() {
    override fun areItemsTheSame(oldItem: WeekListItem, newItem: WeekListItem): Boolean {
        return if (oldItem is WeekListItem.TaskItem && newItem is WeekListItem.TaskItem) {
            oldItem.task.taskId == newItem.task.taskId
        } else if (oldItem is WeekListItem.Header && newItem is WeekListItem.Header) {
            oldItem.title == newItem.title
        } else {
            false
        }
    }

    override fun areContentsTheSame(oldItem: WeekListItem, newItem: WeekListItem): Boolean {
        return oldItem == newItem
    }
}

// --- 4. CÁC HÀM TIỆN ÍCH (Helper Functions) ---
private fun getStartOfDay(calendar: Calendar): Calendar {
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar
}

private fun isOverdue(timestamp: Long): Boolean {
    val today = getStartOfDay(Calendar.getInstance())
    return timestamp < today.timeInMillis
}

private fun formatTimestampToShortDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEE, dd 'thg' MM", Locale("vi", "VN"))
    return sdf.format(Date(timestamp))
}