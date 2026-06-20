package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.StudyDatabase
import com.example.data.model.StudyDay
import com.example.data.model.UserStats
import com.example.data.model.CustomTask
import com.example.data.repository.StudyRepositoryImpl
import com.example.domain.repository.StudyRepository
import com.example.ui.receiver.ReminderReceiver
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import android.content.Context

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StudyRepository = StudyRepositoryImpl(StudyDatabase.getDatabase(application).studyDao())
    
    private val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
        prefs.edit().putBoolean("is_dark_theme", enabled).apply()
    }
    
    // UI trigger for celebration confetti
    private val _celebratedMonth = MutableStateFlow<Int?>(null)
    val celebratedMonth: StateFlow<Int?> = _celebratedMonth.asStateFlow()

    // Screen selection triggers or filterings
    private val _vibeTheme = MutableStateFlow("dark_slate") // Dark slate theme preference
    val vibeTheme: StateFlow<String> = _vibeTheme.asStateFlow()

    // Main States
    val allDays: StateFlow<List<StudyDay>> = repository.getAllStudyDays()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val stats: StateFlow<UserStats?> = repository.getUserStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val allCustomTasks: StateFlow<List<CustomTask>> = repository.getAllCustomTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Derived states
    val monthlyProgress: StateFlow<Map<Int, Double>> = allDays
        .map { days ->
            val progressMap = mutableMapOf<Int, Double>()
            for (m in 1..12) {
                val mDays = days.filter { it.month == m }
                if (mDays.isEmpty()) {
                    progressMap[m] = 0.0
                } else {
                    val completed = mDays.count { it.isCompleted }
                    progressMap[m] = completed.toDouble() / mDays.size
                }
            }
            progressMap
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val unlockedMonths: StateFlow<Set<Int>> = monthlyProgress
        .map { progress ->
            val unlocked = mutableSetOf(1) // Month 1 is unlocked by default
            for (m in 2..12) {
                val previousMonthProgress = progress[m - 1] ?: 0.0
                if (previousMonthProgress >= 1.0) {
                    unlocked.add(m)
                } else {
                    break // Remaining months stay locked
                }
            }
            unlocked
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = setOf(1)
        )

    val currentMonth: StateFlow<Int> = unlockedMonths
        .map { unlocked ->
            unlocked.maxOrNull() ?: 1
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 1
        )

    val todayTask: StateFlow<StudyDay?> = combine(allDays, currentMonth) { days, activeMonth ->
        // Today's task is the first incomplete day of the currently active unlocked month
        days.filter { it.month == activeMonth && !it.isCompleted }
            .minByOrNull { it.dayIndex }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Statistics Derived
    val completedDaysCount: StateFlow<Int> = allDays
        .map { days -> days.count { it.isCompleted } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val overallProgress: StateFlow<Double> = allDays
        .map { days ->
            if (days.isEmpty()) 0.0
            else days.count { it.isCompleted }.toDouble() / days.size
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    init {
        // Automatically sync custom tasks reminders with system alarms
        viewModelScope.launch {
            allCustomTasks.collect { tasks ->
                tasks.forEach { task ->
                    if (task.isReminderEnabled && !task.isCompleted) {
                        ReminderReceiver.scheduleTaskAlarm(
                            application,
                            task.id,
                            task.title,
                            task.category,
                            task.date,
                            task.startTime
                        )
                    } else {
                        ReminderReceiver.cancelTaskAlarm(application, task.id)
                    }
                }
            }
        }
    }

    // User actions
    fun toggleDayCompletion(day: StudyDay) {
        viewModelScope.launch {
            val wasIncomplete = !day.isCompleted
            val month = day.month

            // Before marking, check what the progress of this month is
            val allCurrentDays = allDays.value
            val monthDaysBefore = allCurrentDays.filter { it.month == month }
            val completedBefore = monthDaysBefore.count { it.isCompleted }

            // Save in DB
            repository.completeDay(day, wasIncomplete)

            // Check if this action completes the month 100%
            if (wasIncomplete && completedBefore == monthDaysBefore.size - 1) {
                // Newly completed month!
                _celebratedMonth.value = month
            }
        }
    }

    fun logFocusTime(minutes: Int) {
        viewModelScope.launch {
            val currentStats = stats.value ?: UserStats()
            val updated = currentStats.copy(
                totalFocusMinutes = currentStats.totalFocusMinutes + minutes
            )
            repository.saveUserStats(updated)
        }
    }

    fun dismissCelebration() {
        _celebratedMonth.value = null
    }

    fun updateReminderTime(hour: Int, minute: Int, enabled: Boolean) {
        viewModelScope.launch {
            val currentStats = stats.value ?: UserStats()
            val updated = currentStats.copy(
                reminderHour = hour,
                reminderMinute = minute,
                isReminderEnabled = enabled
            )
            repository.saveUserStats(updated)

            // Trigger actual system Alarm update
            val app = getApplication<Application>()
            if (enabled) {
                ReminderReceiver.scheduleAlarm(app, hour, minute)
            } else {
                ReminderReceiver.cancelAlarm(app)
            }
        }
    }

    fun forceReset() {
        viewModelScope.launch {
            repository.resetProgress()
            val app = getApplication<Application>()
            // Restart default reminder at 9:00 AM
            ReminderReceiver.scheduleAlarm(app, 9, 0)
        }
    }

    // Custom Task methods
    fun insertCustomTask(task: CustomTask) {
        viewModelScope.launch {
            repository.insertCustomTask(task)
            
            // If reminder is enabled, schedule a reminder notification
            if (task.isReminderEnabled) {
                // For simplicity, schedule an alarm or custom notification trigger.
                // We will reuse the existing ReminderReceiver or a simple reminder handler.
            }
        }
    }

    fun updateCustomTask(task: CustomTask) {
        viewModelScope.launch {
            repository.updateCustomTask(task)
        }
    }

    fun deleteCustomTask(task: CustomTask) {
        viewModelScope.launch {
            repository.deleteCustomTask(task)
        }
    }

    fun toggleCustomTaskCompletion(task: CustomTask) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            repository.updateCustomTask(updated)
        }
    }
}
