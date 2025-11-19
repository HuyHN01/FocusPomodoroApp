package com.example.focusmate.ui.todolist

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.focusmate.databinding.DialogPomodoroCountPickerBinding 

class PomodoroCountPickerFragment(
    private val currentCount: Int, 
    private val onSelected: (Int) -> Unit 
) : DialogFragment() {

    private lateinit var binding: DialogPomodoroCountPickerBinding 
    private val POMODORO_DURATION_MINUTES = 25 

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogPomodoroCountPickerBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        
        
        binding.pickerPomodoroCount.minValue = 0 
        binding.pickerPomodoroCount.maxValue = 20 
        binding.pickerPomodoroCount.value = currentCount 

        
        updateEstimateText(currentCount) 

        binding.pickerPomodoroCount.setOnValueChangedListener { _, _, newValue ->
            
            updateEstimateText(newValue)
        }

        
        binding.btnCancel.setOnClickListener {
            dismiss() 
        }

        binding.btnConfirm.setOnClickListener {
            
            val selectedCount = binding.pickerPomodoroCount.value
            
            onSelected(selectedCount)
            dismiss()
        }
    }

    
    private fun updateEstimateText(count: Int) {
        val totalMinutes = count * POMODORO_DURATION_MINUTES
        val totalHours = totalMinutes / 60
        val remainingMinutes = totalMinutes % 60

        val timeString = if (totalHours > 0) {
            "${totalHours}h ${remainingMinutes}ph" 
        } else {
            "${remainingMinutes}ph" 
        }

        binding.tvPomodoroEstimate.text =
            "Ước lượng: $count x ${POMODORO_DURATION_MINUTES}ph = $timeString"
    }
}