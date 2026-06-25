package com.example.data.repository

import com.example.data.local.DatabaseInitializer
import com.example.data.local.StudyDao
import com.example.data.model.Category
import com.example.data.model.DailyTask
import com.example.data.model.UserStats
import com.example.data.model.VocabQuizSet
import com.example.data.model.VocabQuizQuestion
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
        studyDao.clearVocabQuizSets()
        studyDao.clearVocabQuizQuestions()

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

    override fun getAllQuizSets(): Flow<List<VocabQuizSet>> {
        return studyDao.getAllQuizSets()
    }

    override suspend fun getQuizSetByDate(date: String): VocabQuizSet? {
        return studyDao.getQuizSetByDate(date)
    }

    override suspend fun getQuizSetById(id: Int): VocabQuizSet? {
        return studyDao.getQuizSetById(id)
    }

    override suspend fun createQuizSet(quizSet: VocabQuizSet, questions: List<VocabQuizQuestion>): Int {
        val quizSetId = studyDao.insertQuizSet(quizSet).toInt()
        val questionsWithId = questions.map { it.copy(quizSetId = quizSetId) }
        studyDao.insertQuizQuestions(questionsWithId)
        return quizSetId
    }

    override fun getQuestionsForQuizSet(quizSetId: Int): Flow<List<VocabQuizQuestion>> {
        return studyDao.getQuestionsForQuizSet(quizSetId)
    }

    override suspend fun updateQuizQuestion(question: VocabQuizQuestion) {
        studyDao.updateQuizQuestion(question)
    }

    override suspend fun resetQuizSet(quizSetId: Int) {
        val questions = studyDao.getQuestionsForQuizSetSync(quizSetId)
        val resetQuestions = questions.map {
            it.copy(
                userSelectedOptionIndex = -1,
                isAnswered = false,
                isAnsweredCorrectly = false
            )
        }
        studyDao.insertQuizQuestions(resetQuestions)
    }

    override suspend fun deleteQuizSet(quizSet: VocabQuizSet) {
        studyDao.deleteQuizSet(quizSet)
        studyDao.deleteQuestionsForQuizSet(quizSet.id)
    }
}
