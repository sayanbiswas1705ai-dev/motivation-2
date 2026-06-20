package com.example.data.local

import androidx.room.*
import com.example.data.model.StudyDay
import com.example.data.model.UserStats
import com.example.data.model.CustomTask
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
    @Query("SELECT * FROM study_days ORDER BY dayId ASC")
    fun getAllStudyDays(): Flow<List<StudyDay>>

    @Query("SELECT * FROM study_days WHERE month = :month ORDER BY dayIndex ASC")
    fun getDaysByMonth(month: Int): Flow<List<StudyDay>>

    @Query("SELECT * FROM study_days WHERE dayId = :dayId LIMIT 1")
    suspend fun getStudyDayById(dayId: Int): StudyDay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDays(days: List<StudyDay>)

    @Update
    suspend fun updateStudyDay(day: StudyDay)

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    fun getUserStats(): Flow<UserStats?>

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    suspend fun getUserStatsSync(): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserStats(stats: UserStats)

    @Update
    suspend fun updateUserStats(stats: UserStats)

    // Custom Task operations
    @Query("SELECT * FROM custom_tasks ORDER BY date ASC, startTime ASC")
    fun getAllCustomTasks(): Flow<List<CustomTask>>

    @Query("SELECT * FROM custom_tasks WHERE date = :date ORDER BY startTime ASC")
    fun getCustomTasksByDate(date: String): Flow<List<CustomTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomTask(task: CustomTask)

    @Update
    suspend fun updateCustomTask(task: CustomTask)

    @Delete
    suspend fun deleteCustomTask(task: CustomTask)
}
