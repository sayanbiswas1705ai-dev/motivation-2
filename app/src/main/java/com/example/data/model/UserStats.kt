package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val streakCount: Int = 0,
    val lastCompletionDate: String? = null, // "yyyy-MM-dd"
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val isReminderEnabled: Boolean = true,
    val totalFocusMinutes: Int = 0
)
