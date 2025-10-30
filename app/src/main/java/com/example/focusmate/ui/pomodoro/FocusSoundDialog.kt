package com.example.focusmate.ui.pomodoro

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.focusmate.R
import com.example.focusmate.data.model.FocusSound
import com.example.focusmate.data.model.FocusSoundList
import com.example.focusmate.databinding.DialogFocusSoundBinding
import com.example.focusmate.ui.pomodoro.FocusSoundAdapter
import com.example.focusmate.util.FocusSoundPlayer

class FocusSoundDialog (
    private val currentSoundId: Int,
    private val currentVolume: Float,
    private val onConfirm: (soundId: Int, volume: Float) -> Unit
) : DialogFragment(R.layout.dialog_focus_sound) {

    private var _binding: DialogFocusSoundBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: FocusSoundAdapter
    private lateinit var focusSoundPlayer: FocusSoundPlayer

    private var selectedSoundId: Int = currentSoundId
    private var selectedVolume: Float = currentVolume

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialog.window?.attributes?.windowAnimations = R.style.RoundedCornersDialog
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFocusSoundBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo tài nguyên
        focusSoundPlayer = FocusSoundPlayer(requireContext())

        setupViews()
        setupListeners()
        setupOnBackPressedCallback() // Đăng ký xử lý nút Back
    }

    private fun setupViews() {
        // Setup SeekBar
        binding.seekBarVolume.progress = (currentVolume * 100).toInt()

        // Setup RecyclerView
        adapter = FocusSoundAdapter(
            sounds = FocusSoundList.sounds,
            selectedSoundId = currentSoundId
        ) { sound ->
            selectedSoundId = sound.id
            // Phát demo khi chọn
            playDemo(sound)
        }

        binding.rvFocusSounds.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@FocusSoundDialog.adapter
        }
    }

    private fun setupListeners() {
        binding.seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedVolume = progress / 100f

                if (focusSoundPlayer.isPlaying()) {
                    focusSoundPlayer.setVolume(selectedVolume)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnConfirm.setOnClickListener {
            focusSoundPlayer.stopDemo()
            onConfirm(selectedSoundId, selectedVolume)
            dismiss()
        }
    }

    private fun setupOnBackPressedCallback() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                focusSoundPlayer.release()
                dismiss()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun playDemo(sound: FocusSound) {
        if (sound.resourceId != 0) {
            focusSoundPlayer.playDemo(sound.resourceId, selectedVolume)
        } else {
            focusSoundPlayer.stopDemo()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::focusSoundPlayer.isInitialized) {
            focusSoundPlayer.release()
        }
        _binding = null
    }
}