package com.example.domain.repository

import com.example.data.model.StudyDay
import com.example.data.model.UserStats
import com.example.data.model.CustomTask
import kotlinx.coroutines.flow.Flow

interface StudyRepository {
    fun getAllStudyDays(): Flow<List<StudyDay>>
    fun getDaysByMonth(month: Int): Flow<List<StudyDay>>
    fun getUserStats(): Flow<UserStats?>
    suspend fun completeDay(day: StudyDay, isCompleted: Boolean): Boolean
    suspend fun saveUserStats(stats: UserStats)
    suspend fun resetProgress()
    
    // Custom Task methods
    fun getAllCustomTasks(): Flow<List<CustomTask>>
    fun getCustomTasksByDate(date: String): Flow<List<CustomTask>>
    suspend fun insertCustomTask(task: CustomTask)
    suspend fun updateCustomTask(task: CustomTask)
    suspend fun deleteCustomTask(task: CustomTask)
}
