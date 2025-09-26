package com.example.focusmate.ui.todolist

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.focusmate.databinding.ActivityTodolistBinding
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.focusmate.ui.pomodoro.PomodoroViewModel
import com.example.focusmate.ui.pomodoro.TimerState


class TodoListTodayActivity : AppCompatActivity(){
    private lateinit var binding: ActivityTodolistBinding
    private lateinit var taskViewModel: TaskViewModel
    private lateinit var pomodoroViewModel: PomodoroViewModel
    private lateinit var tasksAdapter: TasksAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTodolistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo các ViewModel
        taskViewModel = ViewModelProvider(this).get(TaskViewModel::class.java)
        pomodoroViewModel = ViewModelProvider(this).get(PomodoroViewModel::class.java)

        // Khởi tạo Adapter với các callback
        tasksAdapter = TasksAdapter(
            onTaskClick = { task ->
                // TODO: Xử lý khi click vào task, ví dụ như bắt đầu Pomodoro cho task đó
                pomodoroViewModel.startTimer()
            },
            onCompleteClick = { task ->
                // Xử lý khi người dùng nhấn nút hoàn thành
                taskViewModel.toggleTaskCompletion(task.id)
            }
        )

        binding.tasksList.apply {
            layoutManager = LinearLayoutManager(this@TodoListTodayActivity)
            adapter = tasksAdapter
        }


        // Quan sát dữ liệu từ ViewModel và cập nhật Adapter
        taskViewModel.tasks.observe(this, Observer { tasks ->
            tasksAdapter.submitList(tasks)
        })

        // Quan sát LiveData từ PomodoroViewModel
        pomodoroViewModel.state.observe(this, Observer { state ->
            // TODO: Cập nhật giao diện dựa trên trạng thái (ví dụ: đổi icon play/pause)
            when (state) {
                TimerState.IDLE -> {}
                TimerState.RUNNING -> {}
                TimerState.PAUSED -> {}
                TimerState.BREAK_READY -> {}
                TimerState.BREAK_RUNNING -> {}
                TimerState.BREAK_PAUSED -> {}
            }
        })

        // Gắn sự kiện click cho các nút khác trong giao diện
        binding.addTaskLayout.setOnClickListener {
            // TODO: Mở dialog hoặc màn hình để thêm task mới
        }

        // ... Thêm các sự kiện click cho các nút khác
    }
}