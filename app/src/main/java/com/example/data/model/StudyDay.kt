package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_days")
data class StudyDay(
    @PrimaryKey val dayId: Int, // 1 to 360
    val month: Int,            // 1 to 12
    val dayIndex: Int,         // 1 to 30
    val title: String,
    val description: String,
    val durationMinutes: Int,
    val isCompleted: Boolean = false,
    val completionTimestamp: Long? = null
)
