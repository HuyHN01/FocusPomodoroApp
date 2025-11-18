package com.example.focusmate.ui.todolist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.focusmate.R
import com.example.focusmate.data.local.entity.TaskPriority
import com.example.focusmate.databinding.FragmentAddTaskBinding
import java.util.Calendar

class AddTaskFragment : Fragment() {

    private var _binding: FragmentAddTaskBinding? = null
    private val binding get() = _binding!!
    private var selectedDate: Long? = System.currentTimeMillis()
    private var selectedPomodoros: Int = 0
    private lateinit var pomoIcons: List<ImageView>
    private val viewModel: TaskViewModel by activityViewModels()

    // Xóa biến 'selectedPriority' cũ, vì ViewModel sẽ quản lý

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.tempSelectedPriority.observe(viewLifecycleOwner) { priority ->
            // Cập nhật màu của icon cờ
            val flagColorRes = when (priority) {
                TaskPriority.HIGH -> R.color.priority_high
                TaskPriority.MEDIUM -> R.color.priority_medium
                TaskPriority.LOW -> R.color.priority_low
                TaskPriority.NONE -> R.color.priority_none
            }
            binding.iconPriorityFlag.setColorFilter(
                ContextCompat.getColor(requireContext(), flagColorRes)
            )
        }

        pomoIcons = listOf(
            binding.pomo1,
            binding.pomo2,
            binding.pomo3,
            binding.pomo4,
            binding.pomo5,
            binding.pomo6
        )

        pomoIcons.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                selectedPomodoros = index + 1 // index (0-4) + 1 = số Pomo (1-5)
                updatePomoIcons(selectedPomodoros)
            }
        }
        binding.iconPriorityFlag.setOnClickListener {
            PriorityPickerDialogFragment().show(parentFragmentManager, "PriorityPicker")
        }

        binding.iconDate.setOnClickListener {
            val dateDialog = DatePickerDialogFragment(selectedDate) { timestamp ->
                selectedDate = timestamp
                updateDateIcon(timestamp)
                Toast.makeText(requireContext(), "Đã chọn ngày!", Toast.LENGTH_SHORT).show()
            }
            dateDialog.show(parentFragmentManager, "DatePickerDialog")
        }

        binding.completeText.setOnClickListener {
            val taskTitle = requireActivity()
                .findViewById<EditText>(R.id.add_task_edit_text)
                .text.toString()
                .trim()

            if (taskTitle.isNotEmpty()) {
                // Lấy priority hiện tại từ ViewModel
                val currentPriority = viewModel.tempSelectedPriority.value ?: TaskPriority.NONE

                viewModel.addNewTask(
                    title = taskTitle,
                    estimatedPomodoros = selectedPomodoros,
                    priority = currentPriority,
                    dueDate = selectedDate
                )

                Toast.makeText(requireContext(), "Đã thêm: $taskTitle", Toast.LENGTH_SHORT).show()

                // 4. RESET LẠI PRIORITY VỀ NONE CHO LẦN SAU
                viewModel.setTempPriority(TaskPriority.NONE)
                selectedDate = System.currentTimeMillis()
                selectedPomodoros = 0
                updatePomoIcons(selectedPomodoros)
                binding.iconDate.setImageResource(R.drawable.sunny_24dp_1f1f1f_fill0_wght400_grad0_opsz24)
            }

            // ... (code ẩn fragment và xóa text vẫn đúng) ...
            requireActivity()
                .findViewById<View>(R.id.addTaskFragment)
                .visibility = View.GONE
            requireActivity()
                .findViewById<EditText>(R.id.add_task_edit_text)
                .text.clear()
            requireActivity()
                .findViewById<EditText>(R.id.add_task_edit_text)
                .clearFocus()
        }
    }
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    // 2. Hàm cập nhật Icon dựa trên ngày
    private fun updateDateIcon(timestamp: Long) {
        val selectedCal = Calendar.getInstance()
        selectedCal.timeInMillis = timestamp

        val todayCal = Calendar.getInstance() // Thời gian hiện tại

        val tomorrowCal = Calendar.getInstance()
        tomorrowCal.add(Calendar.DAY_OF_YEAR, 1) // Thời gian ngày mai

        if (isSameDay(selectedCal, todayCal)) {
            binding.iconDate.setImageResource(R.drawable.wb_sunny_24px)
            binding.iconDate.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green))
        }
        else if (isSameDay(selectedCal, tomorrowCal)) {
            binding.iconDate.setImageResource(R.drawable.weather_sunny_low_svgrepo_com)
            binding.iconDate.setColorFilter(ContextCompat.getColor(requireContext(), R.color.orange))
        }
        else {
            binding.iconDate.setImageResource(R.drawable.event_available_24px)
            binding.iconDate.setColorFilter(ContextCompat.getColor(requireContext(), R.color.blue))
        }


    }

    private fun updatePomoIcons(count: Int) {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.pink_pomo)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.gray_pomo)

        pomoIcons.forEachIndexed { index, imageView ->
            if (index < count) {
                imageView.setColorFilter(activeColor)
            } else {
                imageView.setColorFilter(inactiveColor)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // Reset lần nữa phòng khi user thoát mà không lưu
        viewModel.setTempPriority(TaskPriority.NONE)
    }
}