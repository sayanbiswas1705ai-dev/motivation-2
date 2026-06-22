package com.example.data.repository

import com.example.data.local.DatabaseInitializer
import com.example.data.local.StudyDao
import com.example.data.model.Category
import com.example.data.model.DailyTask
import com.example.data.model.UserStats
import com.example.domain.repository.StudyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

class StudyRepositoryImpl(private val studyDao: StudyDao) : StudyRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return studyDao.getAllCategories().onStart {
            ensureUserStatsInitialized()
        }
    }

    override fun getAllDailyTasks(): Flow<List<DailyTask>> {
        return studyDao.getAllDailyTasks()
    }

    override fun getDailyTasksForDate(date: String): Flow<List<DailyTask>> {
        return studyDao.getDailyTasksForDate(date)
    }

    override suspend fun getDailyTasksForDateSync(date: String): List<DailyTask> {
        return studyDao.getDailyTasksForDateSync(date)
    }

    override suspend fun insertCategory(category: Category) {
        studyDao.insertCategory(category)
    }

    override suspend fun deleteCategory(category: Category) {
        studyDao.deleteCategory(category)
    }

    override suspend fun insertDailyTask(task: DailyTask) {
        studyDao.insertDailyTask(task)
    }

    override suspend fun updateDailyTask(task: DailyTask) {
        studyDao.updateDailyTask(task)
    }

    override suspend fun deleteDailyTask(task: DailyTask) {
        studyDao.deleteDailyTask(task)
    }

    override suspend fun deleteDailyTaskByDetails(date: String, categoryName: String) {
        studyDao.deleteDailyTaskByDetails(date, categoryName)
    }

    override fun getUserStats(): Flow<UserStats?> {
        return studyDao.getUserStats().onStart {
            ensureUserStatsInitialized()
        }
    }

    override suspend fun saveUserStats(stats: UserStats) {
        studyDao.insertUserStats(stats)
    }

    override suspend fun getUserStatsSync(): UserStats? {
        return studyDao.getUserStatsSync()
    }

    override suspend fun getAllCategoriesSync(): List<Category> {
        return studyDao.getAllCategoriesSync()
    }

    override suspend fun getAllDailyTasksSync(): List<DailyTask> {
        return studyDao.getAllDailyTasksSync()
    }

    override suspend fun restoreCloudData(
        userName: String,
        userDob: String,
        profilePictureUri: String?,
        categories: List<String>,
        dailyTasks: List<DailyTask>
    ) {
        // Clear existing tables
        studyDao.clearCategories()
        studyDao.clearDailyTasks()
        studyDao.clearUserStats()

        // Insert restored categories
        val restoredCategories = categories.map { Category(name = it) }
        studyDao.insertCategories(restoredCategories)

        // Insert restored tasks
        for (task in dailyTasks) {
            studyDao.insertDailyTask(task)
        }

        // Insert restored user profile stats
        studyDao.insertUserStats(
            UserStats(
                id = 1,
                userName = userName,
                userDob = userDob,
                profilePictureUri = profilePictureUri
            )
        )
    }

    private suspend fun ensureUserStatsInitialized() {
        val stats = studyDao.getUserStatsSync()
        if (stats == null) {
            // Prepopulate 7 default categories
            val defaults = DatabaseInitializer.generateDefaultCategories()
            studyDao.insertCategories(defaults)

            // Prepopulate UserStats
            studyDao.insertUserStats(
                UserStats(
                    id = 1,
                    userName = "",
                    userDob = "",
                    profilePictureUri = null
                )
            )
        }
    }

    override suspend fun resetProgress() {
        studyDao.clearCategories()
        studyDao.clearDailyTasks()
        studyDao.clearUserStats()

        // Re-inject defaults
        val defaults = DatabaseInitializer.generateDefaultCategories()
        studyDao.insertCategories(defaults)
        studyDao.insertUserStats(
            UserStats(
                id = 1,
                userName = "",
                userDob = "",
                profilePictureUri = null
            )
        )
    }
}
