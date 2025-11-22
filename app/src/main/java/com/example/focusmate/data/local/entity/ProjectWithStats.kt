package com.example.focusmate.data.local.entity

import androidx.room.Embedded

data class ProjectWithStats(
    @Embedded val project: ProjectEntity,
    val taskCount: Int,
    val totalPomodoros: Int
)