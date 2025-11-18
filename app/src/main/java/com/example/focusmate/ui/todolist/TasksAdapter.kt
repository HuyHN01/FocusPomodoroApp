package com.example.focusmate.ui.todolist

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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// --- CÁC HÀM HỖ TRỢ (Em có thể để ở cuối file) ---

/**
 * Lấy mốc 00:00:00 của một ngày
 */
private fun getStartOfDay(calendar: Calendar): Calendar {
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar
}

/**
 * Kiểm tra xem ngày có bị quá hạn không (trước 00:00 sáng hôm nay)
 */
private fun isOverdue(timestamp: Long): Boolean {
    val today = getStartOfDay(Calendar.getInstance())
    return timestamp < today.timeInMillis
}

/**
 * Biến Long (1731465000000) thành "Th 5, 13 thg 11"
 */
private fun formatTimestampToShortDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEE, dd 'thg' MM", Locale("vi", "VN"))
    return sdf.format(Date(timestamp))
}



class TasksAdapter(
    private val onTaskClick: (TaskEntity) -> Unit,
    private val onCompleteClick: (TaskEntity) -> Unit,
    private val onPlayClick: (TaskEntity) -> Unit
) : ListAdapter<TaskEntity, TasksAdapter.TaskViewHolder>(TaskDiffCallback()) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }


    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task)
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onTaskClick(getItem(adapterPosition))
                }
            }

            binding.taskPlayIcon.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onPlayClick(getItem(adapterPosition))
                }
            }


            binding.taskCompleteIcon.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onCompleteClick(getItem(adapterPosition))
                }
            }
        }


        // --- SỬA LẠI HÀM BIND() ĐỂ ĐỔI MÀU ---
        fun bind(task: TaskEntity) {
            val context = binding.root.context // Lấy context
            binding.taskTitleTextView.text = task.title

            // 1. Chuẩn bị Pomo
            val pomoText = "${task.estimatedPomodoros} Pomodoro"

            // 2. Lấy màu mặc định (ví dụ: màu đen)
            val defaultColor = ContextCompat.getColor(context, R.color.black) // Em đổi thành màu chữ mặc định của em
            val overdueColor = ContextCompat.getColor(context, R.color.priority_high) // Màu đỏ

            // 3. Kiểm tra Ngày đến hạn
            if (task.dueDate != null) {
                // Nếu CÓ ngày -> Hiển thị cả Pomo và Ngày
                val dateText = formatTimestampToShortDate(task.dueDate!!)
                binding.pomodoroCountTextView.text = "$pomoText • $dateText"

                // KIỂM TRA QUÁ HẠN
                if (isOverdue(task.dueDate!!)) {
                    // Quá hạn -> Tô màu Đỏ
                    binding.pomodoroCountTextView.setTextColor(overdueColor)
                } else {
                    // Chưa quá hạn -> Màu mặc định
                    binding.pomodoroCountTextView.setTextColor(defaultColor)
                }

            } else {
                // Nếu KHÔNG có ngày -> Chỉ hiển thị Pomo
                binding.pomodoroCountTextView.text = pomoText
                // Màu mặc định
                binding.pomodoroCountTextView.setTextColor(defaultColor)
            }

            // 4. Xử lý trạng thái (Completed / Pending)
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

                val priorityColor = when (task.priority) {
                    TaskPriority.HIGH -> ContextCompat.getColor(context, R.color.priority_high)
                    TaskPriority.MEDIUM -> ContextCompat.getColor(context, R.color.priority_medium)
                    TaskPriority.LOW -> ContextCompat.getColor(context, R.color.priority_low)
                    TaskPriority.NONE -> ContextCompat.getColor(context, R.color.priority_none) // Màu xám
                }

                // TÔ MÀU cho vòng tròn
                binding.taskCompleteIcon.setColorFilter(priorityColor)
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<TaskEntity>() {
        override fun areItemsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean {
            return oldItem.taskId == newItem.taskId
        }

        override fun areContentsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean {
            return oldItem == newItem
        }
    }
}