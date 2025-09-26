package com.example.focusmate.ui.mainscreen

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.focusmate.databinding.MainScreenBinding
import com.example.focusmate.ui.MainScreenViewModel
import com.example.focusmate.ui.pomodoro.PomodoroActivity

class MainScreenActivity : AppCompatActivity() {

    private lateinit var binding: MainScreenBinding
    private val viewModel: MainScreenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = MenuAdapter { menuItem ->
            Toast.makeText(this, "Clicked on ${menuItem.title}", Toast.LENGTH_SHORT).show()
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