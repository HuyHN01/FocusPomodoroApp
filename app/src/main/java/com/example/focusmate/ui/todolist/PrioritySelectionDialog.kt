package com.example.focusmate.ui.todolist

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.focusmate.data.local.entity.TaskPriority
import com.example.focusmate.databinding.DialogPrioritySelectionBinding

class PrioritySelectionDialog(
    private val onPrioritySelected: (TaskPriority) -> Unit // Callback trả về kết quả
) : DialogFragment() {

    private lateinit var binding: DialogPrioritySelectionBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogPrioritySelectionBinding.inflate(inflater, container, false)

        // Làm nền dialog trong suốt để thấy bo góc của CardView
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Chọn Cao
        binding.itemPriorityHigh.setOnClickListener {
            onPrioritySelected(TaskPriority.HIGH)
            dismiss()
        }

        // 2. Chọn Trung Bình
        binding.itemPriorityMedium.setOnClickListener {
            onPrioritySelected(TaskPriority.MEDIUM)
            dismiss()
        }

        // 3. Chọn Thấp
        binding.itemPriorityLow.setOnClickListener {
            onPrioritySelected(TaskPriority.LOW)
            dismiss()
        }

        // 4. Chọn Không
        binding.itemPriorityNone.setOnClickListener {
            onPrioritySelected(TaskPriority.NONE)
            dismiss()
        }
    }
}