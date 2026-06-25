package com.example.domain.repository

import com.example.data.model.Category
import com.example.data.model.DailyTask
import com.example.data.model.UserStats
import com.example.data.model.VocabQuizSet
import com.example.data.model.VocabQuizQuestion
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
    suspend fun resetProgress()

    // Vocab Quizzes
    fun getAllQuizSets(): Flow<List<VocabQuizSet>>
    suspend fun getQuizSetByDate(date: String): VocabQuizSet?
    suspend fun getQuizSetById(id: Int): VocabQuizSet?
    suspend fun createQuizSet(quizSet: VocabQuizSet, questions: List<VocabQuizQuestion>): Int
    fun getQuestionsForQuizSet(quizSetId: Int): Flow<List<VocabQuizQuestion>>
    suspend fun updateQuizQuestion(question: VocabQuizQuestion)
    suspend fun resetQuizSet(quizSetId: Int)
    suspend fun deleteQuizSet(quizSet: VocabQuizSet)
}
