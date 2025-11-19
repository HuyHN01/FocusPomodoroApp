package com.example.focusmate.ui.mainscreen

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.focusmate.databinding.MainScreenBinding
import com.example.focusmate.ui.MainScreenViewModel
import com.example.focusmate.ui.pomodoro.PomodoroActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.example.focusmate.R
import com.example.focusmate.data.model.MenuItem
import com.example.focusmate.ui.auth.AuthActivity
import com.example.focusmate.ui.todolist.TodoListTodayActivity
import com.example.focusmate.ui.weeklist.WeekListActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainScreenActivity : AppCompatActivity() {

    private lateinit var binding: MainScreenBinding
    private val viewModel: MainScreenViewModel by viewModels()


    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
            // Refresh lại dữ liệu sau khi đăng nhập
            viewModel.checkUserStatus()
        }
    }

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

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT, // Màu nền status bar
                Color.TRANSPARENT  // Màu scrim (mờ)
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )

        setContentView(binding.root)

        val mainScreenRoot = findViewById<View>(R.id.mainScreen)

        ViewCompat.setOnApplyWindowInsetsListener(mainScreenRoot) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // A. XỬ LÝ HEADER (Đẩy nội dung xuống)
            val headerLayout = findViewById<View>(R.id.headerLayout)
            // Layout gốc đang có padding="16dp".
            // Ta cần giữ 16dp đó và cộng thêm chiều cao Status Bar.
            val originalPaddingTop = 16.dpToPx()

            headerLayout.updatePadding(
                top = originalPaddingTop + bars.top
            )

            // B. XỬ LÝ NÚT TRÒN Ở DƯỚI (Đẩy lên tránh thanh điều hướng)
            val pomodoroCard = findViewById<View>(R.id.pomodoroCardView)
            val originalMarginBottom = 32.dpToPx() // Layout gốc margin bottom 32dp

            pomodoroCard.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = originalMarginBottom + bars.bottom
            }

            // C. (Tùy chọn) Xử lý RecyclerView để item cuối không bị che
            // Nếu danh sách dài, item cuối sẽ bị nút tròn che mất.
            // Ta thêm padding dưới cùng cho list bằng chiều cao nav bar + chiều cao nút tròn + khoảng cách
            val recyclerView = findViewById<View>(R.id.menuRecyclerView)
            recyclerView.updatePadding(
                bottom = bars.bottom + 100.dpToPx() // 100dp là khoảng trừ hao cho nút tròn
            )

            insets
        }

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        // Kiểm tra trạng thái ban đầu
        viewModel.checkUserStatus()
    }

    private fun setupRecyclerView() {
        val adapter = MenuAdapter(
            onItemClicked = { menuItem ->
                if (menuItem.title == "Thêm Dự Án") {
                    val intent = Intent(this, AddProjectActivity::class.java)
                    addProjectLauncher.launch(intent)
                } else {
                    if (menuItem.title == "Hôm nay") {
                        val intent = Intent(this, TodoListTodayActivity::class.java)
                        startActivity(intent)
                    } else {
                        if (menuItem.title == "Tuần này") {
                            val intent = Intent(this, WeekListActivity::class.java)
                            startActivity(intent)
                        }
                        else {
                            Toast.makeText(this, "Clicked on ${menuItem.title}", Toast.LENGTH_SHORT).show()
                        }
                        // Xử lý click vào project cụ thể
                    }
                }
            },
            onItemLongClicked = { menuItem ->
                showProjectOptionsDialog(menuItem)
            }
        )
        binding.menuRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.menuRecyclerView.adapter = adapter

        // Quan sát danh sách Project từ ViewModel
        viewModel.menuItems.observe(this) { items ->
            adapter.submitList(items)
        }
    }

    private fun setupClickListeners() {
        binding.pomodoroCardView.setOnClickListener {
            startActivity(Intent(this, PomodoroActivity::class.java))
        }

        // Xử lý Click vào tên User/Đăng nhập
        binding.loginText.setOnClickListener {
            // Kiểm tra thông qua ViewModel hoặc Text hiện tại
            if (binding.loginText.text == "Đăng Nhập | Đăng Ký") {
                authLauncher.launch(Intent(this, AuthActivity::class.java))
            } else {
                Toast.makeText(this, "Bạn đang đăng nhập với tên: ${binding.loginText.text}", Toast.LENGTH_SHORT).show()
            }
        }

        // Xử lý Click vào Avatar -> Hiện Menu Đăng xuất
        binding.profileImage.setOnClickListener { view ->
            if (binding.loginText.text != "Đăng Nhập | Đăng Ký") {
                showLogoutMenu(view)
            } else {
                Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        // Quan sát thông tin User để cập nhật UI (Thay thế cho hàm checkCurrentUser cũ)
        viewModel.currentUserInfo.observe(this) { name ->
            binding.loginText.text = name
        }
    }
    

    private fun showLogoutMenu(anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menu.add("Đăng xuất")

        // Thêm icon hoặc các option khác nếu cần (VD: Cài đặt tài khoản)

        popup.setOnMenuItemClickListener { menuItem ->
            if (menuItem.title == "Đăng xuất") {
                // --- LOGIC ĐĂNG XUẤT AN TOÀN ---
                // Gọi sang ViewModel để xử lý Data + Auth
                viewModel.signOut()

                Toast.makeText(this, "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show()
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

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}