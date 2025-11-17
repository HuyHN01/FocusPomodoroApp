package com.example.focusmate.ui.todolist

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.focusmate.databinding.DialogPomodoroCountPickerBinding // Đổi tên binding

class PomodoroCountPickerFragment(
    private val currentCount: Int, // Số Pomo hiện tại
    private val onSelected: (Int) -> Unit // Hàm trả về số Pomo mới
) : DialogFragment() {

    private lateinit var binding: DialogPomodoroCountPickerBinding // Đổi tên binding
    private val POMODORO_DURATION_MINUTES = 25 // 1 Pomo = 25 phút

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogPomodoroCountPickerBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- CÀI ĐẶT BÁNH XE QUAY SỐ ---
        // 1. Cài đặt giá trị
        binding.pickerPomodoroCount.minValue = 0 // Bắt đầu từ 0
        binding.pickerPomodoroCount.maxValue = 20 // Tối đa 20 Pomo (em tự đổi)
        binding.pickerPomodoroCount.value = currentCount // Set giá trị hiện tại

        // 2. Cập nhật text bên trên khi quay bánh xe
        updateEstimateText(currentCount) // Cập nhật lần đầu

        binding.pickerPomodoroCount.setOnValueChangedListener { _, _, newValue ->
            // Khi người dùng quay, cập nhật text
            updateEstimateText(newValue)
        }

        // --- XỬ LÝ NÚT BẤM ---
        binding.btnCancel.setOnClickListener {
            dismiss() // Nút Huỷ -> Đóng
        }

        binding.btnConfirm.setOnClickListener {
            // Nút Hoàn tất -> Lấy giá trị từ bánh xe
            val selectedCount = binding.pickerPomodoroCount.value
            // Trả giá trị về cho Activity
            onSelected(selectedCount)
            dismiss()
        }
    }

    // Hàm cập nhật text "Ước lượng: 0 x 25ph = 0ph"
    private fun updateEstimateText(count: Int) {
        val totalMinutes = count * POMODORO_DURATION_MINUTES
        val totalHours = totalMinutes / 60
        val remainingMinutes = totalMinutes % 60

        val timeString = if (totalHours > 0) {
            "${totalHours}h ${remainingMinutes}ph" // Ví dụ: 1h 15ph
        } else {
            "${remainingMinutes}ph" // Ví dụ: 25ph
        }

        binding.tvPomodoroEstimate.text =
            "Ước lượng: $count x ${POMODORO_DURATION_MINUTES}ph = $timeString"
    }
}