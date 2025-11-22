package com.example.focusmate.ui.mainscreen

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.focusmate.data.model.MenuItem
import com.example.focusmate.databinding.ItemMenuBinding
import android.graphics.Color

private const val TYPE_PROJECT = 0
private const val TYPE_ADD_BUTTON = 1

class MenuAdapter(private val onItemClicked: (MenuItem) -> Unit,
                  private val onItemLongClicked: (MenuItem) -> Unit
) :
    ListAdapter<MenuItem, MenuAdapter.MenuViewHolder>(MenuDiffCallback()) {

    class MenuViewHolder(private val binding: ItemMenuBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MenuItem) {
            binding.itemIcon.setImageResource(item.iconRes)
            binding.itemText.text = item.title
            binding.itemTime.text = item.focusedTime
            binding.itemTaskCount.text = item.taskCount.toString()
            item.colorString?.let { colorStr ->
                try {
                    binding.itemIcon.setColorFilter(Color.parseColor(colorStr))
                } catch (e: IllegalArgumentException) {

                    binding.itemIcon.clearColorFilter()
                }
            } ?: binding.itemIcon.clearColorFilter()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.itemView.setOnClickListener {
            onItemClicked(currentItem)
        }
        holder.itemView.setOnLongClickListener {
            if (currentItem.id != null) {
                onItemLongClicked(currentItem)
                true
            } else {
                false
            }
        }
        holder.bind(currentItem)
    }
}

class MenuDiffCallback : DiffUtil.ItemCallback<MenuItem>() {
    override fun areItemsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
        if (oldItem.id != null && newItem.id != null) {
            return oldItem.id == newItem.id
        }
        if (oldItem.id == null && newItem.id == null) {
            return oldItem.title == newItem.title
        }
        return false
    }

    override fun areContentsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
        return oldItem == newItem
    }
}