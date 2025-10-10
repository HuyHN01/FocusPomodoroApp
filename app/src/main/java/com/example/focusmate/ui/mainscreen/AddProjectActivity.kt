package com.example.focusmate.ui.mainscreen

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.focusmate.R

class AddProjectActivity : AppCompatActivity() {
    private lateinit var edtProjectName: EditText
    private lateinit var colorGrid: GridLayout
    private lateinit var tvDone: TextView
    private lateinit var btnBack: ImageButton

    private var selectedColor: Int? = null
    private lateinit var projectIcon: ImageView

    private val colors = listOf(
        0xFFE91E63.toInt(), 0xFF2196F3.toInt(), 0xFFFFC107.toInt(), 0xFF4CAF50.toInt(),
        0xFF9C27B0.toInt(), 0xFF009688.toInt(), 0xFFFF5722.toInt(), 0xFF795548.toInt(),
        0xFF607D8B.toInt(), 0xFF3F51B5.toInt(), 0xFFFFEB3B.toInt(), 0xFF673AB7.toInt(),
        0xFF8BC34A.toInt(), 0xFFFF9800.toInt(), 0xFF00BCD4.toInt(), 0xFF9E9E9E.toInt(),
        0xFF000000.toInt(), 0xFFCDDC39.toInt(), 0xFFFFB6C1.toInt(), 0xFF40E0D0.toInt()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_project)

        edtProjectName = findViewById(R.id.edtProjectName)
        colorGrid = findViewById(R.id.colorGrid)
        tvDone = findViewById(R.id.tvDone)
        projectIcon = findViewById(R.id.projectIcon)
        btnBack = findViewById(R.id.btnBack)
        renderColorOptions()

        tvDone.setOnClickListener {
            val name = edtProjectName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên dự án", Toast.LENGTH_SHORT).show()
            } else if (selectedColor == null) {
                Toast.makeText(this, "Vui lòng chọn màu", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent().apply {
                    putExtra("project_name", name)
                    putExtra("project_color", selectedColor!!)
                }
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
        btnBack.setOnClickListener {
            finish()
        }

    }

    private fun renderColorOptions() {
        colorGrid.removeAllViews()
        val size = (resources.displayMetrics.density * 70).toInt() // 48dp

        for (color in colors) {
            val btn = ImageButton(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(8, 8, 8, 8)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                }
            }

            btn.setOnClickListener {
                selectedColor = color
                projectIcon.setColorFilter(color)
                highlightSelected(btn)
            }

            colorGrid.addView(btn)
        }
    }


    private fun highlightSelected(selectedBtn: ImageButton) {
        for (i in 0 until colorGrid.childCount) {
            val child = colorGrid.getChildAt(i) as ImageButton
            child.foreground = null
        }
        selectedBtn.foreground = getDrawable(R.drawable.check_circle_24px)
    }
}