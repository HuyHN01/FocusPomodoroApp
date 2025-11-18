package com.example.focusmate.ui.mainscreen

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.focusmate.databinding.MainScreenBinding
import com.example.focusmate.ui.MainScreenViewModel
import com.example.focusmate.ui.pomodoro.PomodoroActivity
import androidx.appcompat.app.AlertDialog
import com.example.focusmate.R
import com.example.focusmate.data.model.MenuItem
import com.example.focusmate.ui.auth.AuthActivity
import com.example.focusmate.ui.todolist.TodoListTodayActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainScreenActivity : AppCompatActivity() {

    private lateinit var binding: MainScreenBinding
    private val viewModel: MainScreenViewModel by viewModels()
    private lateinit var firebaseAuth: FirebaseAuth
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
        firebaseAuth = FirebaseAuth.getInstance()
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
            if (firebaseAuth.currentUser == null) {
                val intent = Intent(this, AuthActivity::class.java)
                authLauncher.launch(intent)
            } else {
                Toast.makeText(this, "Bạn đã đăng nhập", Toast.LENGTH_SHORT).show()
            }
        }
        binding.profileImage.setOnClickListener { view ->
            if (firebaseAuth.currentUser != null) {
                showLogoutMenu(view)
            } else {
                Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show()
            }
        }
        checkCurrentUser()
    }

    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()

            checkCurrentUser()
        } else {
            Toast.makeText(this, "Đăng nhập đã bị hủy.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkCurrentUser() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            val displayName = user.displayName
            val email = user.email

            val greetingName = if (!displayName.isNullOrBlank()) {
                displayName
            } else if (!email.isNullOrBlank()) {
                email
            } else {
                "Người dùng"
            }
            binding.loginText.text = greetingName

        } else {
            binding.loginText.text = "Đăng Nhập | Đăng Ký"
        }
    }
    private fun showLogoutMenu(anchorView: View) {
        val popup = PopupMenu(this, anchorView)

        popup.menu.add("Đăng xuất")

        popup.setOnMenuItemClickListener { menuItem ->
            if (menuItem.title == "Đăng xuất") {

                // --- PHẦN LOGIC ĐĂNG XUẤT CỦA BẠN EM ĐỂ Ở ĐÂY ---

                Toast.makeText(this, "Đã nhấn đăng xuất!", Toast.LENGTH_SHORT).show()
                true
            } else {
                false
            }
        }
        popup.show()
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