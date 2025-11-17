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
import androidx.appcompat.app.AlertDialog
import com.example.focusmate.R
import com.example.focusmate.data.model.MenuItem
import com.example.focusmate.ui.auth.AuthActivity
import com.example.focusmate.ui.todolist.TodoListTodayActivity

class MainScreenActivity : AppCompatActivity() {

    private lateinit var binding: MainScreenBinding
    private val viewModel: MainScreenViewModel by viewModels()

    private val editProjectLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val projectId = data?.getStringExtra("project_id_string")
                val projectName = data?.getStringExtra("project_name")
                val projectColorString = data?.getStringExtra("project_color_string")

                if (projectId != null && projectName != null && projectColorString != null) {
                    viewModel.updateProject(projectId, projectName, projectColorString)
                }
            }
        }
    private val addProjectLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val projectName = data?.getStringExtra("project_name") ?: return@registerForActivityResult
                val projectColorString = data?.getStringExtra("project_color_string") ?: return@registerForActivityResult

                viewModel.addProject(projectName, projectColorString)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = MenuAdapter(
            onItemClicked = { menuItem ->
            if (menuItem.title == "Thêm Dự Án") {
                val intent = Intent(this, AddProjectActivity::class.java)
                addProjectLauncher.launch(intent)
            } else {
                if (menuItem.title == "Hôm nay") {
                    val intent = Intent(this, TodoListTodayActivity::class.java)
                    startActivity(intent)

                }
                Toast.makeText(this, "Clicked on ${menuItem.title}", Toast.LENGTH_SHORT).show()
            }
        },
            onItemLongClicked = { menuItem ->
                showProjectOptionsDialog(menuItem)
            }
        )

        binding.menuRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.menuRecyclerView.adapter = adapter


        viewModel.menuItems.observe(this) { items ->
            adapter.submitList(items)
        }
        binding.pomodoroCardView.setOnClickListener {
            val intent = Intent(this, PomodoroActivity::class.java)

            startActivity(intent)
        }

        binding.loginText.setOnClickListener {
            val intent = Intent(this, AuthActivity::class.java)
            authLauncher.launch(intent)
        }
    }

    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()

            updateUiForLoggedInUser()
        } else {
            Toast.makeText(this, "Đăng nhập đã bị hủy.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUiForLoggedInUser() {
        // Ví dụ:
        // buttonLogin.text = "Đã đăng nhập"
        // buttonSync.isEnabled = true
    }
    private fun showProjectOptionsDialog(menuItem: MenuItem) {
        val options = arrayOf("Chỉnh sửa", "Xóa")

        AlertDialog.Builder(this)
            .setTitle("Tùy chọn cho: ${menuItem.title}")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, AddProjectActivity::class.java).apply {
                            putExtra("project_id_string", menuItem.id)
                            putExtra("project_name", menuItem.title)
                            putExtra("project_color_string", menuItem.colorString)
                        }
                        editProjectLauncher.launch(intent)
                    }
                    1 -> {
                        viewModel.deleteProject(menuItem)
                        Toast.makeText(this, "Đã xóa ${menuItem.title}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}