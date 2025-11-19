package com.example.focusmate.ui.todolist

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.focusmate.databinding.DialogDatePickerBinding
import java.text.SimpleDateFormat
import java.util.*

class DatePickerDialogFragment(
    private val initialDate: Long?,
    private val onDateSelected: (Long) -> Unit

) : DialogFragment() {

    private lateinit var binding: DialogDatePickerBinding
    private var selectedTimestamp: Long = initialDate ?: System.currentTimeMillis()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogDatePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.let { window ->
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT  
            )
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.calendarView.date = selectedTimestamp
        updateDisplayDate(selectedTimestamp)
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedTimestamp = calendar.timeInMillis
            updateDisplayDate(selectedTimestamp)
        }

        binding.btnToday.setOnClickListener {
            val calendar = Calendar.getInstance()
            binding.calendarView.date = calendar.timeInMillis
            selectedTimestamp = calendar.timeInMillis
            updateDisplayDate(selectedTimestamp)
        }

        binding.btnTomorrow.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            binding.calendarView.date = calendar.timeInMillis
            selectedTimestamp = calendar.timeInMillis
            updateDisplayDate(selectedTimestamp)
        }

        binding.btnNextWeek.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, 7)
            binding.calendarView.date = calendar.timeInMillis
            selectedTimestamp = calendar.timeInMillis
            updateDisplayDate(selectedTimestamp)
        }

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnConfirm.setOnClickListener {
            onDateSelected(selectedTimestamp)
            dismiss()
        }
    }

    private fun updateDisplayDate(timestamp: Long) {
        val sdf = SimpleDateFormat("EEE, dd 'thg' MM yyyy", Locale("vi", "VN"))
        binding.tvSelectedDateDisplay.text = sdf.format(Date(timestamp))
    }
}