package com.example.data.repository

import com.example.data.local.DatabaseInitializer
import com.example.data.local.StudyDao
import com.example.data.model.StudyDay
import com.example.data.model.UserStats
import com.example.data.model.CustomTask
import com.example.domain.repository.StudyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StudyRepositoryImpl(private val studyDao: StudyDao) : StudyRepository {

    override fun getAllStudyDays(): Flow<List<StudyDay>> {
        return studyDao.getAllStudyDays().onStart {
            ensureDatabasePrepopulated()
        }
    }

    override fun getDaysByMonth(month: Int): Flow<List<StudyDay>> {
        return studyDao.getDaysByMonth(month).onStart {
            ensureDatabasePrepopulated()
        }
    }

    override fun getUserStats(): Flow<UserStats?> {
        return studyDao.getUserStats().onStart {
            ensureUserStatsInitialized()
        }
    }

    override suspend fun saveUserStats(stats: UserStats) {
        studyDao.insertUserStats(stats)
    }

    private suspend fun ensureDatabasePrepopulated() {
        // Run check sync
        val stats = studyDao.getUserStatsSync()
        // Simple heuristic: if we have zero study days, populate them
        // Let's first verify if we have empty days table
        // We can do this safely by getting first day or writing a count query or checking stats availability
        // If we don't have basic entries, we populate
        val studyDayCheck = studyDao.getStudyDayById(1)
        if (studyDayCheck == null) {
            val prebuiltDays = DatabaseInitializer.generateDays()
            studyDao.insertAllDays(prebuiltDays)
        }
    }

    private suspend fun ensureUserStatsInitialized() {
        val stats = studyDao.getUserStatsSync()
        if (stats == null) {
            studyDao.insertUserStats(
                UserStats(
                    id = 1,
                    streakCount = 0,
                    lastCompletionDate = null,
                    reminderHour = 9,
                    reminderMinute = 0,
                    isReminderEnabled = true
                )
            )
        }
    }

    override suspend fun completeDay(day: StudyDay, isCompleted: Boolean): Boolean {
        // Get existing day
        val existing = studyDao.getStudyDayById(day.dayId) ?: return false
        
        val updatedDay = existing.copy(
            isCompleted = isCompleted,
            completionTimestamp = if (isCompleted) System.currentTimeMillis() else null
        )
        studyDao.updateStudyDay(updatedDay)

        // Handle stats and streak logic
        val stats = studyDao.getUserStatsSync() ?: UserStats()
        if (isCompleted) {
            val todayStr = getCurrentDateString()
            val lastDateStr = stats.lastCompletionDate

            val newStreak = when {
                lastDateStr == null -> 1
                lastDateStr == todayStr -> stats.streakCount // Checked in today already, maintain
                isYesterday(lastDateStr) -> stats.streakCount + 1 // Consecutive, increment
                else -> 1 // Gap exists, reset to 1
            }

            studyDao.updateUserStats(
                stats.copy(
                    streakCount = newStreak,
                    lastCompletionDate = todayStr
                )
            )
        } else {
            // Decrementing or marking incomplete. We keep streak or if last date was today, we check if other days are completed today
            // Just for simplicity, we don't forcefully wipe streaks unless necessary
        }
        return true
    }

    override suspend fun resetProgress() {
        // Reset all days
        val prebuiltDays = DatabaseInitializer.generateDays()
        studyDao.insertAllDays(prebuiltDays)
        // Reset stats
        studyDao.insertUserStats(
            UserStats(
                id = 1,
                streakCount = 0,
                lastCompletionDate = null,
                reminderHour = 9,
                reminderMinute = 0,
                isReminderEnabled = true
            )
        )
    }

    // Helper functions for Streak calculation
    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    private fun isYesterday(dateStr: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsedDate = sdf.parse(dateStr) ?: return false
            
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            
            val yesterdayStr = sdf.format(calendar.time)
            yesterdayStr == dateStr
        } catch (e: Exception) {
            false
        }
    }

    override fun getAllCustomTasks(): Flow<List<CustomTask>> {
        return studyDao.getAllCustomTasks()
    }

    override fun getCustomTasksByDate(date: String): Flow<List<CustomTask>> {
        return studyDao.getCustomTasksByDate(date)
    }

    override suspend fun insertCustomTask(task: CustomTask) {
        studyDao.insertCustomTask(task)
    }

    override suspend fun updateCustomTask(task: CustomTask) {
        studyDao.updateCustomTask(task)
    }

    override suspend fun deleteCustomTask(task: CustomTask) {
        studyDao.deleteCustomTask(task)
    }
}
