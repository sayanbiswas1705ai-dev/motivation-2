package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.StudyDatabase
import com.example.data.model.Category
import com.example.data.model.DailyTask
import com.example.data.model.UserStats
import com.example.data.model.DateUtils
import com.example.data.repository.StudyRepositoryImpl
import com.example.domain.repository.StudyRepository
import com.example.data.remote.FirestoreManager
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

    init {
        FirestoreManager.initialize(application)
    }

    // Cloud Synchronizer stats
    val cloudSyncId = MutableStateFlow(FirestoreManager.getUserId(application))
    val cloudSyncLastTime = MutableStateFlow(FirestoreManager.getLastSyncTime(application))
    val cloudSyncStatus = MutableStateFlow<String>("IDLE")

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", true))
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

    // Cloud Synchronizer Actions
    fun triggerCloudBackup() {
        viewModelScope.launch {
            try {
                val currentStats = repository.getUserStatsSync()
                val currentCategories = repository.getAllCategoriesSync()
                val currentTasks = repository.getAllDailyTasksSync()
                FirestoreManager.uploadData(
                    context = getApplication(),
                    stats = currentStats,
                    categories = currentCategories,
                    tasks = currentTasks
                )
                cloudSyncLastTime.value = FirestoreManager.getLastSyncTime(getApplication())
            } catch (e: Exception) {
                android.util.Log.e("StudyViewModel", "Auto-backup to cloud failed: ${e.message}")
            }
        }
    }

    fun manualCloudBackup() {
        cloudSyncStatus.value = "SYNCING"
        viewModelScope.launch {
            try {
                val currentStats = repository.getUserStatsSync()
                val currentCategories = repository.getAllCategoriesSync()
                val currentTasks = repository.getAllDailyTasksSync()
                val success = FirestoreManager.uploadData(
                    context = getApplication(),
                    stats = currentStats,
                    categories = currentCategories,
                    tasks = currentTasks
                )
                if (success) {
                    cloudSyncStatus.value = "SUCCESS_BACKUP"
                    cloudSyncLastTime.value = FirestoreManager.getLastSyncTime(getApplication())
                } else {
                    cloudSyncStatus.value = "ERROR"
                }
            } catch (e: Exception) {
                cloudSyncStatus.value = "ERROR"
            }
        }
    }

    fun manualCloudRestore(customId: String? = null) {
        val targetId = customId?.trim() ?: cloudSyncId.value.trim()
        if (targetId.length < 3) {
            cloudSyncStatus.value = "ERROR"
            return
        }
        cloudSyncStatus.value = "SYNCING"
        viewModelScope.launch {
            try {
                val data = FirestoreManager.downloadData(getApplication(), targetId)
                if (data != null) {
                    repository.restoreCloudData(
                        userName = data.userName,
                        userDob = data.userDob,
                        profilePictureUri = data.profilePictureUri,
                        categories = data.categories,
                        dailyTasks = data.dailyTasks
                    )
                    FirestoreManager.updateUserId(getApplication(), targetId)
                    cloudSyncId.value = targetId
                    cloudSyncLastTime.value = FirestoreManager.getLastSyncTime(getApplication())
                    cloudSyncStatus.value = "SUCCESS_RESTORE"
                } else {
                    cloudSyncStatus.value = "ERROR"
                }
            } catch (e: Exception) {
                cloudSyncStatus.value = "ERROR"
            }
        }
    }

    fun updateSyncUserId(newId: String) {
        val trimmed = newId.trim()
        if (trimmed.length >= 3) {
            FirestoreManager.updateUserId(getApplication(), trimmed)
            cloudSyncId.value = trimmed
        }
    }

    fun resetSyncStatus() {
        cloudSyncStatus.value = "IDLE"
    }

    // Category Management Actions
    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repository.insertCategory(Category(name = trimmed))
            triggerCloudBackup()
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            triggerCloudBackup()
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
            triggerCloudBackup()
        }
    }

    // Toggle daily task's checkbox completion status
    fun toggleDailyTaskCompletion(task: DailyTask) {
        viewModelScope.launch {
            repository.updateDailyTask(task.copy(isCompleted = !task.isCompleted))
            triggerCloudBackup()
        }
    }

    // Update user profile info
    fun updateUserProfile(name: String, dob: String, picUri: String?) {
        viewModelScope.launch {
            val currentStats = stats.value ?: UserStats()
            val updated = currentStats.copy(
                userName = name,
                userDob = dob,
                profilePictureUri = picUri
            )
            repository.saveUserStats(updated)
            triggerCloudBackup()
        }
    }

    // Hard reset of database to default categories, clearing all tasks and profile details
    fun forceReset() {
        viewModelScope.launch {
            repository.resetProgress()
            _selectedDate.value = DateUtils.getTodayString()
            triggerCloudBackup()
        }
    }
}
