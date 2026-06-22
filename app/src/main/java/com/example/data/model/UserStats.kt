package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val userName: String = "",
    val userDob: String = "",
    val profilePictureUri: String? = null
)
