package com.example.focusmate.ui.todolist

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.focusmate.R
import com.example.focusmate.data.model.Task
import com.example.focusmate.databinding.ItemTaskBinding

class TasksAdapter(
    private val onTaskClick: (Task) -> Unit,           // Callback khi click vào task
    private val onCompleteClick: (Task) -> Unit        // Callback khi click vào nút hoàn thành
) : RecyclerView.Adapter<TasksAdapter.TaskViewHolder>() {

    private var tasks: List<Task> = emptyList()

    // Cập nhật danh sách tasks và thông báo cho RecyclerView
    fun submitList(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task)
    }

    override fun getItemCount(): Int = tasks.size

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            // Hiển thị tiêu đề task
            binding.taskTitleTextView.text = task.title

            // Hiển thị số Pomodoro ước tính
            val pomodoroText = "${task.pomodoroCount}g 15ph" // Giả sử 1g 15ph = 1 pomodoro
            binding.pomodoroCountTextView.text = pomodoroText

            // Xử lý trạng thái hoàn thành
            if (task.isCompleted) {
                binding.taskCompleteIcon.setImageResource(R.drawable.green_checkmark_icon)
                binding.taskTitleTextView.paintFlags =
                    binding.taskTitleTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.taskPlayIcon.visibility = View.GONE
            } else {
                binding.taskCompleteIcon.setImageResource(R.drawable.ellipse_shape_line_icon)
                binding.taskTitleTextView.paintFlags =
                    binding.taskTitleTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.taskPlayIcon.visibility = View.VISIBLE
                binding.taskPlayIcon.setOnClickListener { onTaskClick(task) }
            }

            // Xử lý sự kiện click
            binding.root.setOnClickListener { onTaskClick(task) }
            binding.taskCompleteIcon.setOnClickListener { onCompleteClick(task) }
        }
    }
}