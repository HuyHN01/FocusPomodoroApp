package com.example.focusmate.ui.todolist

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.focusmate.data.local.entity.ProjectEntity
import com.example.focusmate.databinding.ItemProjectSelectionBinding

class ProjectSelectionAdapter(
    private val currentProjectId: String?,
    private val onProjectClicked: (ProjectEntity) -> Unit
) : ListAdapter<ProjectEntity, ProjectSelectionAdapter.ProjectViewHolder>(ProjectDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = ItemProjectSelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = getItem(position)
        holder.bind(project)
    }

    inner class ProjectViewHolder(private val binding: ItemProjectSelectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.projectItemRoot.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onProjectClicked(getItem(adapterPosition))
                }
            }
        }

        fun bind(project: ProjectEntity) {
            binding.projectName.text = project.name

            try {
                binding.projectIcon.imageTintList = ColorStateList.valueOf(Color.parseColor(project.color))
            } catch (e: Exception) {
                binding.projectIcon.imageTintList = ColorStateList.valueOf(Color.GRAY)
            }

            if (project.projectId == currentProjectId) {
                binding.projectCheckmark.visibility = View.VISIBLE
            } else {
                binding.projectCheckmark.visibility = View.GONE
            }
        }
    }

    class ProjectDiffCallback : DiffUtil.ItemCallback<ProjectEntity>() {
        override fun areItemsTheSame(oldItem: ProjectEntity, newItem: ProjectEntity): Boolean {
            return oldItem.projectId == newItem.projectId
        }

        override fun areContentsTheSame(oldItem: ProjectEntity, newItem: ProjectEntity): Boolean {
            return oldItem == newItem
        }
    }
}