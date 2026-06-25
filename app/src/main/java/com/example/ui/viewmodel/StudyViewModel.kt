package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.StudyDatabase
import com.example.data.model.Category
import com.example.data.model.DailyTask
import com.example.data.model.UserStats
import com.example.data.model.DateUtils
import com.example.data.model.VocabQuizSet
import com.example.data.model.VocabQuizQuestion
import com.example.data.util.PdfExtractor
import com.example.data.remote.GeminiClient
import com.example.data.repository.StudyRepositoryImpl
import com.example.domain.repository.StudyRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.Context

@OptIn(ExperimentalCoroutinesApi::class)
class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StudyRepository = StudyRepositoryImpl(
        StudyDatabase.getDatabase(application).studyDao()
    )

    private val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
        prefs.edit().putBoolean("is_dark_theme", enabled).apply()
    }

    // Categories List
    val categories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current selected date for view/handling tasks (defaults to today)
    private val _selectedDate = MutableStateFlow(DateUtils.getTodayString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    fun selectDate(dateStr: String) {
        _selectedDate.value = dateStr
    }

    // All daily tasks across all dates (for calendar visualizer & stats)
    val allDailyTasks: StateFlow<List<DailyTask>> = repository.getAllDailyTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Daily tasks filtered for the selected date
    val dailyTasksForSelectedDate: StateFlow<List<DailyTask>> = _selectedDate
        .flatMapLatest { date ->
            repository.getDailyTasksForDate(date)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // User Profile Stats
    val stats: StateFlow<UserStats?> = repository.getUserStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Derived: Selected Date Consistency Ratio
    val selectedDateConsistencyRatio: StateFlow<Double> = dailyTasksForSelectedDate
        .map { tasks ->
            if (tasks.isEmpty()) return@map 0.0
            val completed = tasks.count { it.isCompleted }
            (completed.toDouble() / tasks.size) * 100.0
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    // Derived: Overall consistency ratio across all days that have at least one task selected
    val overallConsistencyRatio: StateFlow<Double> = repository.getAllDailyTasks()
        .map { allTasks ->
            val grouped = allTasks.groupBy { it.date }
            if (grouped.isEmpty()) return@map 0.0

            var totalRatiosSum = 0.0
            var validDaysCount = 0

            for ((_, dayTasks) in grouped) {
                if (dayTasks.isNotEmpty()) {
                    val completed = dayTasks.count { it.isCompleted }
                    val ratio = (completed.toDouble() / dayTasks.size) * 100.0
                    totalRatiosSum += ratio
                    validDaysCount++
                }
            }
            if (validDaysCount == 0) 0.0 else totalRatiosSum / validDaysCount
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    // Category Management Actions
    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repository.insertCategory(Category(name = trimmed))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // Toggle selected state of category for the given date
    fun toggleCategorySelectionForDate(date: String, categoryName: String, isSelected: Boolean) {
        viewModelScope.launch {
            if (isSelected) {
                val currentTasks = repository.getDailyTasksForDateSync(date)
                if (currentTasks.none { it.categoryName == categoryName }) {
                    repository.insertDailyTask(
                        DailyTask(
                            date = date,
                            categoryName = categoryName,
                            isCompleted = false
                        )
                    )
                }
            } else {
                repository.deleteDailyTaskByDetails(date, categoryName)
            }
        }
    }

    // Toggle daily task's checkbox completion status
    fun toggleDailyTaskCompletion(task: DailyTask) {
        viewModelScope.launch {
            repository.updateDailyTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    // Hard reset of database to default categories, clearing all tasks and profile details
    fun forceReset() {
        viewModelScope.launch {
            repository.resetProgress()
            _selectedDate.value = DateUtils.getTodayString()
        }
    }

    // Vocab Quiz Features
    sealed interface UploadStatus {
        object Idle : UploadStatus
        object Extracting : UploadStatus
        object Generating : UploadStatus
        object Success : UploadStatus
        data class Error(val message: String) : UploadStatus
    }

    val vocabQuizSets: StateFlow<List<VocabQuizSet>> = repository.getAllQuizSets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeQuizSet = MutableStateFlow<VocabQuizSet?>(null)
    val activeQuizSet: StateFlow<VocabQuizSet?> = _activeQuizSet.asStateFlow()

    val activeQuestions: StateFlow<List<VocabQuizQuestion>> = _activeQuizSet
        .flatMapLatest { quizSet ->
            if (quizSet == null) flowOf(emptyList())
            else repository.getQuestionsForQuizSet(quizSet.id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _uploadStatus = MutableStateFlow<UploadStatus>(UploadStatus.Idle)
    val uploadStatus: StateFlow<UploadStatus> = _uploadStatus.asStateFlow()

    private val _showFeedback = MutableStateFlow(false)
    val showFeedback: StateFlow<Boolean> = _showFeedback.asStateFlow()

    private val _lastSelectedOption = MutableStateFlow<Int?>(null)
    val lastSelectedOption: StateFlow<Int?> = _lastSelectedOption.asStateFlow()

    fun selectQuizSet(quizSet: VocabQuizSet?) {
        _activeQuizSet.value = quizSet
        _currentQuestionIndex.value = 0
        _showFeedback.value = false
        _lastSelectedOption.value = null
    }

    fun resetUploadStatus() {
        _uploadStatus.value = UploadStatus.Idle
    }

    fun uploadAndGenerateQuiz(uri: Uri, fileName: String, startPage: Int? = null, endPage: Int? = null) {
        viewModelScope.launch {
            _uploadStatus.value = UploadStatus.Extracting
            try {
                val context = getApplication<Application>()
                val extractedText = PdfExtractor.extractText(context, uri, fileName, startPage, endPage)
                if (extractedText.isBlank()) {
                    _uploadStatus.value = UploadStatus.Error("The uploaded file did not contain any extractable text.")
                    return@launch
                }

                _uploadStatus.value = UploadStatus.Generating
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    _uploadStatus.value = UploadStatus.Error("Gemini API key is not configured. Please enter your API key in the Secrets Panel.")
                    return@launch
                }

                val questions = GeminiClient.generateVocabularyQuiz(apiKey, extractedText)
                if (questions.isEmpty()) {
                    _uploadStatus.value = UploadStatus.Error("Gemini did not find any vocabulary words or generate questions from the text.")
                    return@launch
                }

                val today = DateUtils.getTodayString()
                val displaySource = if (startPage != null && endPage != null) {
                    "$fileName (Pages $startPage-$endPage)"
                } else if (startPage != null) {
                    "$fileName (Page $startPage+)"
                } else if (endPage != null) {
                    "$fileName (Up to Page $endPage)"
                } else {
                    fileName
                }
                val quizSet = VocabQuizSet(date = today, sourcePdfName = displaySource)
                
                val vocabQuestions = questions.map { raw ->
                    VocabQuizQuestion(
                        quizSetId = 0, // will be replaced in repository
                        word = raw.word,
                        optionA = raw.optionA,
                        optionB = raw.optionB,
                        optionC = raw.optionC,
                        optionD = raw.optionD,
                        correctOptionIndex = raw.correctOptionIndex,
                        explanation = raw.explanation
                    )
                }

                repository.createQuizSet(quizSet, vocabQuestions)
                
                // Fetch the created quiz set to set as active
                val createdQuizSet = repository.getQuizSetByDate(today)
                if (createdQuizSet != null) {
                    selectQuizSet(createdQuizSet)
                }
                
                _uploadStatus.value = UploadStatus.Success
            } catch (e: Exception) {
                android.util.Log.e("StudyViewModel", "Vocabulary quiz generation failed: ${e.message}", e)
                _uploadStatus.value = UploadStatus.Error(e.message ?: "An unexpected error occurred.")
            }
        }
    }

    fun generateQuizFromManualText(text: String, title: String) {
        viewModelScope.launch {
            if (text.isBlank()) {
                _uploadStatus.value = UploadStatus.Error("Please enter some text first.")
                return@launch
            }
            _uploadStatus.value = UploadStatus.Generating
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    _uploadStatus.value = UploadStatus.Error("Gemini API key is not configured. Please enter your API key in the Secrets Panel.")
                    return@launch
                }

                val questions = GeminiClient.generateVocabularyQuiz(apiKey, text)
                if (questions.isEmpty()) {
                    _uploadStatus.value = UploadStatus.Error("Gemini did not find any vocabulary words or generate questions from the text.")
                    return@launch
                }

                val today = DateUtils.getTodayString()
                val displayTitle = if (title.isBlank()) "Manually Pasted Text" else title
                val quizSet = VocabQuizSet(date = today, sourcePdfName = displayTitle)
                
                val vocabQuestions = questions.map { raw ->
                    VocabQuizQuestion(
                        quizSetId = 0,
                        word = raw.word,
                        optionA = raw.optionA,
                        optionB = raw.optionB,
                        optionC = raw.optionC,
                        optionD = raw.optionD,
                        correctOptionIndex = raw.correctOptionIndex,
                        explanation = raw.explanation
                    )
                }

                repository.createQuizSet(quizSet, vocabQuestions)
                
                // Fetch the created quiz set to set as active
                val createdQuizSet = repository.getQuizSetByDate(today)
                if (createdQuizSet != null) {
                    selectQuizSet(createdQuizSet)
                }
                
                _uploadStatus.value = UploadStatus.Success
            } catch (e: Exception) {
                android.util.Log.e("StudyViewModel", "Vocabulary quiz manual generation failed: ${e.message}", e)
                _uploadStatus.value = UploadStatus.Error(e.message ?: "An unexpected error occurred.")
            }
        }
    }

    fun submitAnswer(question: VocabQuizQuestion, selectedOptionIndex: Int) {
        if (question.isAnswered) return
        _lastSelectedOption.value = selectedOptionIndex
        _showFeedback.value = true
        viewModelScope.launch {
            val isCorrect = selectedOptionIndex == question.correctOptionIndex
            val updated = question.copy(
                userSelectedOptionIndex = selectedOptionIndex,
                isAnswered = true,
                isAnsweredCorrectly = isCorrect
            )
            repository.updateQuizQuestion(updated)
        }
    }

    fun nextQuestion() {
        _showFeedback.value = false
        _lastSelectedOption.value = null
        _currentQuestionIndex.value = _currentQuestionIndex.value + 1
    }

    fun reattemptQuiz(quizSetId: Int) {
        viewModelScope.launch {
            repository.resetQuizSet(quizSetId)
            _currentQuestionIndex.value = 0
            _showFeedback.value = false
            _lastSelectedOption.value = null
        }
    }

    fun generateQuizFromUrl(urlString: String, startPage: Int? = null, endPage: Int? = null) {
        viewModelScope.launch {
            val trimmedUrl = urlString.trim()
            if (trimmedUrl.isBlank()) {
                _uploadStatus.value = UploadStatus.Error("Please enter a valid URL.")
                return@launch
            }
            
            // Check basic URL format
            if (!trimmedUrl.startsWith("http://", ignoreCase = true) && !trimmedUrl.startsWith("https://", ignoreCase = true)) {
                _uploadStatus.value = UploadStatus.Error("Invalid URL. It must start with http:// or https://")
                return@launch
            }

            _uploadStatus.value = UploadStatus.Extracting
            try {
                val context = getApplication<Application>()
                val (extractedText, sourceName) = PdfExtractor.downloadAndExtract(context, trimmedUrl, startPage, endPage)
                
                if (extractedText.isBlank()) {
                    _uploadStatus.value = UploadStatus.Error("The downloaded document did not contain any extractable text.")
                    return@launch
                }

                _uploadStatus.value = UploadStatus.Generating
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    _uploadStatus.value = UploadStatus.Error("Gemini API key is not configured. Please enter your API key in the Secrets Panel.")
                    return@launch
                }

                val questions = GeminiClient.generateVocabularyQuiz(apiKey, extractedText)
                if (questions.isEmpty()) {
                    _uploadStatus.value = UploadStatus.Error("Gemini did not find any vocabulary words or generate questions from the text.")
                    return@launch
                }

                val today = DateUtils.getTodayString()
                val displaySource = if (startPage != null && endPage != null) {
                    "$sourceName (Pages $startPage-$endPage)"
                } else if (startPage != null) {
                    "$sourceName (Page $startPage+)"
                } else if (endPage != null) {
                    "$sourceName (Up to Page $endPage)"
                } else {
                    sourceName
                }
                
                val quizSet = VocabQuizSet(date = today, sourcePdfName = displaySource)
                
                val vocabQuestions = questions.map { raw ->
                    VocabQuizQuestion(
                        quizSetId = 0,
                        word = raw.word,
                        optionA = raw.optionA,
                        optionB = raw.optionB,
                        optionC = raw.optionC,
                        optionD = raw.optionD,
                        correctOptionIndex = raw.correctOptionIndex,
                        explanation = raw.explanation
                    )
                }

                repository.createQuizSet(quizSet, vocabQuestions)
                
                val createdQuizSet = repository.getQuizSetByDate(today)
                if (createdQuizSet != null) {
                    selectQuizSet(createdQuizSet)
                }
                
                _uploadStatus.value = UploadStatus.Success
            } catch (e: Exception) {
                android.util.Log.e("StudyViewModel", "Quiz generation from URL failed: ${e.message}", e)
                _uploadStatus.value = UploadStatus.Error(e.message ?: "An unexpected error occurred while processing the link.")
            }
        }
    }

    fun deleteQuizSet(quizSet: VocabQuizSet) {
        viewModelScope.launch {
            try {
                // If the deleted quiz is the currently active/selected quiz, clear it from selected state
                val currentSelected = _activeQuizSet.value
                if (currentSelected?.id == quizSet.id) {
                    selectQuizSet(null)
                }
                repository.deleteQuizSet(quizSet)
            } catch (e: Exception) {
                android.util.Log.e("StudyViewModel", "Failed to delete quiz set: ${e.message}", e)
            }
        }
    }
}
