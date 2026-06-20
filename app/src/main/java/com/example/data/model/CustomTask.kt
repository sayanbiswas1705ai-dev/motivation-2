package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_tasks")
data class CustomTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val date: String,             // Format: "yyyy-MM-dd"
    val startTime: String,        // Format: "HH:mm"
    val endTime: String,          // Format: "HH:mm"
    val durationMinutes: Int,
    val category: String,         // "Study", "Revision", "Practice", "Exam", "Personal", "Other"
    val priority: String,         // "Low", "Medium", "High"
    val isReminderEnabled: Boolean = false,
    val isCompleted: Boolean = false
)
