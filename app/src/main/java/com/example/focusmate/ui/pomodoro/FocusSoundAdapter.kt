package com.example.focusmate.ui.pomodoro

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.focusmate.data.model.FocusSound
import com.example.focusmate.databinding.ItemFocusSoundBinding

class FocusSoundAdapter(
    private val sounds: List<FocusSound>,
    private var selectedSoundId: Int,
    private val onSoundSelected: (FocusSound) -> Unit
) : RecyclerView.Adapter<FocusSoundAdapter.SoundViewHolder>(){

    inner class SoundViewHolder(private val binding: ItemFocusSoundBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(sound: FocusSound) {
            binding.tvSoundName.text = sound.name
            binding.rbSelect.isChecked = sound.id == selectedSoundId

            binding.root.setOnClickListener {
                val previousSelected = selectedSoundId
                selectedSoundId = sound.id

                
                notifyItemChanged(sounds.indexOfFirst { it.id == previousSelected })
                notifyItemChanged(sounds.indexOfFirst { it.id == selectedSoundId })

                
                onSoundSelected(sound)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SoundViewHolder {
        val binding = ItemFocusSoundBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SoundViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: SoundViewHolder,
        position: Int
    ) {
        holder.bind(sounds[position])
    }

    override fun getItemCount(): Int {
        return sounds.size
    }

    fun updateSelection(soundId: Int) {
        val previousSelected = selectedSoundId
        selectedSoundId = soundId
        notifyItemChanged(sounds.indexOfFirst { it.id == previousSelected })
        notifyItemChanged(sounds.indexOfFirst { it.id == selectedSoundId })
    }
}