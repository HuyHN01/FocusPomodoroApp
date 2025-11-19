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
    private val onPrioritySelected: (TaskPriority) -> Unit 
) : DialogFragment() {

    private lateinit var binding: DialogPrioritySelectionBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogPrioritySelectionBinding.inflate(inflater, container, false)

        
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        
        binding.itemPriorityHigh.setOnClickListener {
            onPrioritySelected(TaskPriority.HIGH)
            dismiss()
        }

        
        binding.itemPriorityMedium.setOnClickListener {
            onPrioritySelected(TaskPriority.MEDIUM)
            dismiss()
        }

        
        binding.itemPriorityLow.setOnClickListener {
            onPrioritySelected(TaskPriority.LOW)
            dismiss()
        }

        
        binding.itemPriorityNone.setOnClickListener {
            onPrioritySelected(TaskPriority.NONE)
            dismiss()
        }
    }
}