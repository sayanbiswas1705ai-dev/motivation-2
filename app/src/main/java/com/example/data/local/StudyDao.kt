package com.example.data.local

import androidx.room.*
import com.example.data.model.Category
import com.example.data.model.DailyTask
import com.example.data.model.UserStats
import com.example.data.model.VocabQuizSet
import com.example.data.model.VocabQuizQuestion
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
    // Categories
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)

    @Delete
    suspend fun deleteCategory(category: Category)

    // Daily Tasks
    @Query("SELECT * FROM daily_tasks ORDER BY date ASC")
    fun getAllDailyTasks(): Flow<List<DailyTask>>

    @Query("SELECT * FROM daily_tasks WHERE date = :date")
    fun getDailyTasksForDate(date: String): Flow<List<DailyTask>>

    @Query("SELECT * FROM daily_tasks WHERE date = :date")
    suspend fun getDailyTasksForDateSync(date: String): List<DailyTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyTask(task: DailyTask)

    @Update
    suspend fun updateDailyTask(task: DailyTask)

    @Delete
    suspend fun deleteDailyTask(task: DailyTask)

    @Query("DELETE FROM daily_tasks WHERE date = :date AND categoryName = :categoryName")
    suspend fun deleteDailyTaskByDetails(date: String, categoryName: String)

    // User Stats
    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    fun getUserStats(): Flow<UserStats?>

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    suspend fun getUserStatsSync(): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserStats(stats: UserStats)

    // Reset operations
    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Query("DELETE FROM daily_tasks")
    suspend fun clearDailyTasks()

    @Query("DELETE FROM user_stats")
    suspend fun clearUserStats()

    // Vocab Quizzes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizSet(quizSet: VocabQuizSet): Long

    @Query("SELECT * FROM vocab_quiz_sets ORDER BY date DESC")
    fun getAllQuizSets(): Flow<List<VocabQuizSet>>

    @Query("SELECT * FROM vocab_quiz_sets WHERE date = :date LIMIT 1")
    suspend fun getQuizSetByDate(date: String): VocabQuizSet?

    @Query("SELECT * FROM vocab_quiz_sets WHERE id = :id LIMIT 1")
    suspend fun getQuizSetById(id: Int): VocabQuizSet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizQuestions(questions: List<VocabQuizQuestion>)

    @Query("SELECT * FROM vocab_quiz_questions WHERE quizSetId = :quizSetId ORDER BY id ASC")
    fun getQuestionsForQuizSet(quizSetId: Int): Flow<List<VocabQuizQuestion>>

    @Query("SELECT * FROM vocab_quiz_questions WHERE quizSetId = :quizSetId ORDER BY id ASC")
    suspend fun getQuestionsForQuizSetSync(quizSetId: Int): List<VocabQuizQuestion>

    @Update
    suspend fun updateQuizQuestion(question: VocabQuizQuestion)

    @Delete
    suspend fun deleteQuizSet(quizSet: VocabQuizSet)

    @Query("DELETE FROM vocab_quiz_questions WHERE quizSetId = :quizSetId")
    suspend fun deleteQuestionsForQuizSet(quizSetId: Int)

    @Query("DELETE FROM vocab_quiz_sets")
    suspend fun clearVocabQuizSets()

    @Query("DELETE FROM vocab_quiz_questions")
    suspend fun clearVocabQuizQuestions()
}
