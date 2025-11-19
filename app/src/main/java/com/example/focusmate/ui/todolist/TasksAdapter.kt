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


        
        fun bind(task: TaskEntity) {
            val context = binding.root.context 
            binding.taskTitleTextView.text = task.title

            
            val pomoText = "${task.estimatedPomodoros} Pomodoro"

            
            val defaultColor = ContextCompat.getColor(context, R.color.black) 
            val overdueColor = ContextCompat.getColor(context, R.color.priority_high) 

            
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
                    TaskPriority.NONE -> ContextCompat.getColor(context, R.color.priority_none) 
                }

                
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