package com.example.focusmate.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.focusmate.R
import com.example.focusmate.data.model.MenuItem

class MainScreenViewModel : ViewModel() {
    private val _menuItems = MutableLiveData<List<MenuItem>>()
    val menuItems: LiveData<List<MenuItem>> = _menuItems

    init {
        loadMenuItems()
    }

    private fun loadMenuItems() {
        _menuItems.value = listOf(
            MenuItem(R.drawable.wb_sunny_24px, "Hôm nay", "1h 15m", 5),
            MenuItem(R.drawable.wb_twilight_24px, "Ngày mai", "0m", 0), // Giả sử bạn có icon tên là ic_tomorrow
            MenuItem(R.drawable.calendar_month_24px, "Tuần này", "1h 15m", 5),
            MenuItem(R.drawable.event_available_24px, "Đã lên kế hoạch", "1h 15m", 5),
            MenuItem(R.drawable.event_24px, "Sự kiện", "0m", 0),
            MenuItem(R.drawable.check_circle_24px, "Đã hoàn thành", "0m", 0),
            MenuItem(R.drawable.task_24px, "Nhiệm vụ", "1h 15m", 5),
            MenuItem( iconRes = R.drawable.outline_add_24, title = "Thêm Dự Án",focusedTime = "",taskCount = -1)
        )
    }
    fun addProject(projectName: String, colorRes: Int) {
        val currentList = _menuItems.value?.toMutableList() ?: mutableListOf()

        val addButton = currentList.find { it.title == "Thêm Dự Án" }
        currentList.remove(addButton)

        val newProject = MenuItem(
            iconRes = R.drawable.ic_circle,
            title = projectName,
            focusedTime = "0m",
            taskCount = 0,
            colorRes = colorRes
        )
        currentList.add(newProject)

        if (addButton != null) {
            currentList.add(addButton)
        }

        _menuItems.value = currentList
    }

}