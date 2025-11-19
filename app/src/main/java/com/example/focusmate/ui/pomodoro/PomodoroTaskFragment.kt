
package com.example.focusmate.ui.pomodoro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.focusmate.databinding.FragmentPomodoroTaskBinding
import com.example.focusmate.ui.todolist.TaskViewModel


class PomodoroTaskFragment : Fragment() {

    private lateinit var taskViewModel: TaskViewModel
    private var _binding: FragmentPomodoroTaskBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPomodoroTaskBinding.inflate(inflater, container, false)

        taskViewModel = ViewModelProvider(requireActivity()).get(TaskViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        
        val taskTitle = arguments?.getString("TASK_TITLE")
        binding.taskTitleInFragment.text = taskTitle

        
        binding.closeTaskButton.setOnClickListener {
            taskViewModel.clearCurrentTask()
        }
    }

    
    companion object {
        fun newInstance(title: String): PomodoroTaskFragment {
            val fragment = PomodoroTaskFragment()
            val args = Bundle()
            args.putString("TASK_TITLE", title)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}