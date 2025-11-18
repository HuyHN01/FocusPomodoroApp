package com.example.focusmate.ui.todolist

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.focusmate.R
import com.example.focusmate.databinding.DialogProjectPickerBinding
import com.example.focusmate.data.local.entity.ProjectEntity // <-- THÊM IMPORT NÀY

class ProjectPickerDialogFragment : DialogFragment() {

    private var _binding: DialogProjectPickerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TaskViewModel by activityViewModels()
    private lateinit var projectAdapter: ProjectSelectionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogProjectPickerBinding.inflate(inflater, container, false)

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val currentProjectId = viewModel.currentTask.value?.projectId // Sửa: projectId

        projectAdapter = ProjectSelectionAdapter(currentProjectId) { selectedProject ->

            if (selectedProject.projectId == "inbox_id_placeholder") {
                viewModel.updateTaskProject(null)
            } else {
                viewModel.updateTaskProject(selectedProject.projectId)
            }
            dismiss()
        }
        binding.rvProjects.adapter = projectAdapter

        viewModel.allProjects.observe(viewLifecycleOwner) { projects ->
            val inboxProject = ProjectEntity(
                projectId = "inbox_id_placeholder",
                userId = "",
                name = "Nhiệm vụ",
                color = "#808080"
            )

            val listWithInbox = mutableListOf(inboxProject)
            listWithInbox.addAll(projects)

            projectAdapter.submitList(listWithInbox)
        }

        binding.btnAddProject.setOnClickListener {
            showAddProjectDialog()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun showAddProjectDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Tên dự án mới..."
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Tạo Dự Án Mới")
            .setView(editText)
            .setPositiveButton("Tạo") { dialog, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    // Gọi ViewModel để tạo dự án mới
                    viewModel.addNewProject(newName)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}