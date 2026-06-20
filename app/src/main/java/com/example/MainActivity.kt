package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.StudyViewModel

class MainActivity : ComponentActivity() {
    
    private val viewModel: StudyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = isDarkTheme) {
                // Collect states reactively with lifecycle awareness
                val allDays by viewModel.allDays.collectAsStateWithLifecycle()
                val stats by viewModel.stats.collectAsStateWithLifecycle()
                val monthlyProgressMap by viewModel.monthlyProgress.collectAsStateWithLifecycle()
                val unlockedMonths by viewModel.unlockedMonths.collectAsStateWithLifecycle()
                val currentActiveMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
                val todayTask by viewModel.todayTask.collectAsStateWithLifecycle()
                val completedDaysCount by viewModel.completedDaysCount.collectAsStateWithLifecycle()
                val overallProgress by viewModel.overallProgress.collectAsStateWithLifecycle()
                val celebratedMonth by viewModel.celebratedMonth.collectAsStateWithLifecycle()
                val customTasks by viewModel.allCustomTasks.collectAsStateWithLifecycle()

                var currentTab by remember { mutableStateOf(0) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            NavigationBar(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                    .testTag("app_navigation_bar"),
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == 0,
                                    onClick = { currentTab = 0 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = "Home tab icon"
                                        )
                                    },
                                    label = { Text("Home", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("nav_home_tab")
                                )
                                NavigationBarItem(
                                    selected = currentTab == 1,
                                    onClick = { currentTab = 1 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = "Calendar tab icon"
                                        )
                                    },
                                    label = { Text("Calendar", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("nav_calendar_tab")
                                )
                                NavigationBarItem(
                                    selected = currentTab == 2,
                                    onClick = { currentTab = 2 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = "Badges tab icon"
                                        )
                                    },
                                    label = { Text("Awards", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("nav_awards_tab")
                                )
                                NavigationBarItem(
                                    selected = currentTab == 3,
                                    onClick = { currentTab = 3 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Alarm,
                                            contentDescription = "Schedule tab icon"
                                        )
                                    },
                                    label = { Text("Remind", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("nav_settings_tab")
                                )
                            }
                        },
                        contentWindowInsets = WindowInsets.statusBars
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                0 -> HomeScreen(
                                    stats = stats,
                                    todayTask = todayTask,
                                    currentActiveMonth = currentActiveMonth,
                                    completedDays = completedDaysCount,
                                    overallProgress = overallProgress,
                                    monthlyProgressMap = monthlyProgressMap,
                                    customTasks = customTasks,
                                    onCompleteTask = { day ->
                                        viewModel.toggleDayCompletion(day)
                                    },
                                    onSaveReminder = { hour, minute, enabled ->
                                        viewModel.updateReminderTime(hour, minute, enabled)
                                    },
                                    onLogFocusTime = { minutes ->
                                        viewModel.logFocusTime(minutes)
                                    },
                                    onAddCustomTask = { task ->
                                        viewModel.insertCustomTask(task)
                                    },
                                    onUpdateCustomTask = { task ->
                                        viewModel.updateCustomTask(task)
                                    },
                                    onDeleteCustomTask = { task ->
                                        viewModel.deleteCustomTask(task)
                                    },
                                    onToggleCustomTask = { task ->
                                        viewModel.toggleCustomTaskCompletion(task)
                                    }
                                )
                                1 -> CalendarScreen(
                                    currentMonth = currentActiveMonth,
                                    allDays = allDays,
                                    customTasks = customTasks,
                                    onCompleteTask = { day ->
                                        viewModel.toggleDayCompletion(day)
                                    }
                                )
                                2 -> AchievementsScreen(
                                    monthlyProgressMap = monthlyProgressMap,
                                    allDays = allDays
                                )
                                3 -> SettingsScreen(
                                    stats = stats,
                                    isDarkTheme = isDarkTheme,
                                    onToggleDarkTheme = { enabled ->
                                        viewModel.setDarkTheme(enabled)
                                    },
                                    onSaveReminder = { hour, minute, enabled ->
                                        viewModel.updateReminderTime(hour, minute, enabled)
                                    },
                                    onResetData = {
                                        viewModel.forceReset()
                                    }
                                )
                            }

                            // Dynamic celebrations overlays over screen scaffold
                            if (celebratedMonth != null) {
                                CelebrationOverlay(
                                    monthId = celebratedMonth!!,
                                    onDismiss = {
                                        viewModel.dismissCelebration()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
