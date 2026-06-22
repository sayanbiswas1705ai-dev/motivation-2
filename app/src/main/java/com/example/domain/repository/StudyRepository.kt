package com.example.domain.repository

import com.example.data.model.Category
import com.example.data.model.DailyTask
import com.example.data.model.UserStats
import kotlinx.coroutines.flow.Flow

interface StudyRepository {
    fun getAllCategories(): Flow<List<Category>>
    suspend fun insertCategory(category: Category)
    suspend fun deleteCategory(category: Category)

    fun getAllDailyTasks(): Flow<List<DailyTask>>
    fun getDailyTasksForDate(date: String): Flow<List<DailyTask>>
    suspend fun getDailyTasksForDateSync(date: String): List<DailyTask>
    suspend fun insertDailyTask(task: DailyTask)
    suspend fun updateDailyTask(task: DailyTask)
    suspend fun deleteDailyTask(task: DailyTask)
    suspend fun deleteDailyTaskByDetails(date: String, categoryName: String)

    fun getUserStats(): Flow<UserStats?>
    suspend fun saveUserStats(stats: UserStats)
    suspend fun getUserStatsSync(): UserStats?
    suspend fun getAllCategoriesSync(): List<Category>
    suspend fun getAllDailyTasksSync(): List<DailyTask>
    suspend fun restoreCloudData(
        userName: String,
        userDob: String,
        profilePictureUri: String?,
        categories: List<String>,
        dailyTasks: List<DailyTask>
    )
    suspend fun resetProgress()
}
