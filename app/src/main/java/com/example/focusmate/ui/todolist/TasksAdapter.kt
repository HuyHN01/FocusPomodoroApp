package com.example.focusmate.ui.todolist

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.focusmate.R
import com.example.focusmate.data.local.entity.TaskEntity
import com.example.focusmate.data.local.entity.TaskStatus // Import Enum mới
import com.example.focusmate.databinding.ItemTaskBinding


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
            binding.taskTitleTextView.text = task.title

            val pomodoroText = "${task.estimatedPomodoros} Pomodoro"
            binding.pomodoroCountTextView.text = pomodoroText

            if (task.status == TaskStatus.COMPLETED) {
                binding.taskCompleteIcon.setImageResource(R.drawable.green_checkmark_icon)
                binding.taskTitleTextView.paintFlags =
                    binding.taskTitleTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.taskPlayIcon.visibility = View.GONE
            } else {
                binding.taskCompleteIcon.setImageResource(R.drawable.ellipse_shape_line_icon)
                binding.taskTitleTextView.paintFlags =
                    binding.taskTitleTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.taskPlayIcon.visibility = View.VISIBLE
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<TaskEntity>() {
        override fun areItemsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean {
            // 3. Sửa 'id' thành 'taskId' để khớp Entity
            return oldItem.taskId == newItem.taskId
        }

        override fun areContentsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean {
            return oldItem == newItem
        }
    }
}