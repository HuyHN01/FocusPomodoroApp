package com.example.focusmate.ui.weeklist

import com.example.focusmate.data.local.entity.TaskEntity

sealed class WeekListItem {

    data class Header(val title: String) : WeekListItem()

    data class TaskItem(val task: TaskEntity) : WeekListItem()
}