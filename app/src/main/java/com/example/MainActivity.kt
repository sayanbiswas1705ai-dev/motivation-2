package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.Book
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VocabQuizScreen
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
                val stats by viewModel.stats.collectAsStateWithLifecycle()
                val categories by viewModel.categories.collectAsStateWithLifecycle()
                val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
                val allDailyTasks by viewModel.allDailyTasks.collectAsStateWithLifecycle()
                val dailyTasksForSelectedDate by viewModel.dailyTasksForSelectedDate.collectAsStateWithLifecycle()
                val selectedConsistencyRatio by viewModel.selectedDateConsistencyRatio.collectAsStateWithLifecycle()
                val overallConsistencyRatio by viewModel.overallConsistencyRatio.collectAsStateWithLifecycle()
                val vocabQuizSets by viewModel.vocabQuizSets.collectAsStateWithLifecycle()
                
                var currentTab by remember { mutableStateOf(0) }
                var showSettings by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showSettings) {
                        SettingsScreen(
                            categories = categories,
                            isDarkTheme = isDarkTheme,
                            onToggleDarkTheme = { enabled ->
                                viewModel.setDarkTheme(enabled)
                            },
                            onAddCategory = { newCategory ->
                                viewModel.addCategory(newCategory)
                            },
                            onDeleteCategory = { category ->
                                viewModel.deleteCategory(category)
                            },
                            onResetData = {
                                viewModel.forceReset()
                            },
                            onBack = {
                                showSettings = false
                            }
                        )
                    } else {
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
                                                imageVector = Icons.Default.Book,
                                                contentDescription = "Vocab Quiz tab icon"
                                            )
                                        },
                                        label = { Text("Vocab Quiz", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.testTag("nav_vocab_quiz_tab")
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
                                        categories = categories,
                                        selectedDate = selectedDate,
                                        dailyTasks = dailyTasksForSelectedDate,
                                        allDailyTasks = allDailyTasks,
                                        vocabQuizSets = vocabQuizSets,
                                        selectedConsistencyRatio = selectedConsistencyRatio,
                                        overallConsistencyRatio = overallConsistencyRatio,
                                        onSelectDate = { dateStr ->
                                            viewModel.selectDate(dateStr)
                                        },
                                        onToggleCategory = { date, categoryName, isSelected ->
                                            viewModel.toggleCategorySelectionForDate(date, categoryName, isSelected)
                                        },
                                        onToggleTaskCompletion = { task ->
                                            viewModel.toggleDailyTaskCompletion(task)
                                        },
                                        onOpenSettings = {
                                            showSettings = true
                                        }
                                    )
                                    1 -> CalendarScreen(
                                        allDailyTasks = allDailyTasks,
                                        vocabQuizSets = vocabQuizSets,
                                        selectedDate = selectedDate,
                                        onSelectDate = { dateStr ->
                                            viewModel.selectDate(dateStr)
                                        },
                                        onToggleTaskCompletion = { task ->
                                            viewModel.toggleDailyTaskCompletion(task)
                                        }
                                    )
                                    2 -> VocabQuizScreen(
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
