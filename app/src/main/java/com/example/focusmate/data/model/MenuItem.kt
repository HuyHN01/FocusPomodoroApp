package com.example.focusmate.data.model

data class MenuItem(
    val id: String? = null,
    val iconRes: Int,
    val title: String,
    val focusedTime: String,
    val taskCount: Int,
    val colorString: String? = null
)