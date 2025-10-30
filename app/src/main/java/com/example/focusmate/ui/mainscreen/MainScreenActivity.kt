package com.example.focusmate.ui.mainscreen

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.focusmate.databinding.MainScreenBinding
import com.example.focusmate.ui.MainScreenViewModel
import com.example.focusmate.ui.pomodoro.PomodoroActivity
import com.example.focusmate.R
class MainScreenActivity : AppCompatActivity() {

    private lateinit var binding: MainScreenBinding
    private val viewModel: MainScreenViewModel by viewModels()

    private val addProjectLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val projectName = data?.getStringExtra("project_name") ?: return@registerForActivityResult
                val projectColor = data.getIntExtra("project_color", 0) ?:0

                viewModel.addProject(projectName, projectColor)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = MenuAdapter { menuItem ->
            if (menuItem.title == "Thêm Dự Án") {
                val intent = Intent(this, AddProjectActivity::class.java)
                addProjectLauncher.launch(intent)
            } else {
                Toast.makeText(this, "Clicked on ${menuItem.title}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.menuRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.menuRecyclerView.adapter = adapter


        viewModel.menuItems.observe(this) { items ->
            adapter.submitList(items)
        }
        binding.pomodoroCardView.setOnClickListener {
            val intent = Intent(this, PomodoroActivity::class.java)

            startActivity(intent)
        }
    }
}