package com.example.focusmate.ui.todolist 

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.focusmate.data.local.entity.TaskPriority
import com.example.focusmate.databinding.DialogPriorityPickerBinding

class PriorityPickerDialogFragment : DialogFragment() {

    private var _binding: DialogPriorityPickerBinding? = null
    private val binding get() = _binding!!

    
    private val viewModel: TaskViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPriorityPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        
        binding.btnPriorityHigh.setOnClickListener {
            viewModel.setTempPriority(TaskPriority.HIGH)
            dismiss()
        }

        binding.btnPriorityMedium.setOnClickListener {
            viewModel.setTempPriority(TaskPriority.MEDIUM)
            dismiss()
        }

        binding.btnPriorityLow.setOnClickListener {
            viewModel.setTempPriority(TaskPriority.LOW)
            dismiss()
        }

        binding.btnPriorityNone.setOnClickListener {
            viewModel.setTempPriority(TaskPriority.NONE)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}