package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.DailyTask
import com.example.data.model.UserStats
import com.example.data.model.DateUtils
import com.example.data.model.VocabQuizSet
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    stats: UserStats?,
    categories: List<Category>,
    selectedDate: String,
    dailyTasks: List<DailyTask>,
    allDailyTasks: List<DailyTask>,
    vocabQuizSets: List<VocabQuizSet>,
    selectedConsistencyRatio: Double,
    overallConsistencyRatio: Double,
    onSelectDate: (String) -> Unit,
    onToggleCategory: (String, String, Boolean) -> Unit,
    onToggleTaskCompletion: (DailyTask) -> Unit,
    onOpenSettings: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Convert selectedDate for displaying in a human-readable format
    val readableDate = remember(selectedDate) {
        try {
            val parsed = DateUtils.parseDate(selectedDate)
            if (parsed != null) {
                SimpleDateFormat("MMMM d, yyyy (EEEE)", Locale.US).format(parsed)
            } else {
                selectedDate
            }
        } catch (e: Exception) {
            selectedDate
        }
    }

    // Modal state for date picking
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        // App / User Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .windowInsetsPadding(WindowInsets.statusBars),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Sayan's Initiative",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Exam Preparation Tracker",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            // Settings Action Icon
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .testTag("home_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Open Settings Screen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Date selection ribbon
        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Selected Prep Date",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = readableDate,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Quick date shift controls
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Yesterday Button
                TextButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        DateUtils.parseDate(selectedDate)?.let { cal.time = it }
                        cal.add(Calendar.DAY_OF_YEAR, -1)
                        onSelectDate(DateUtils.getFormatter().format(cal.time))
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Yesterday", style = MaterialTheme.typography.bodySmall)
                }

                // Today Button
                val isCurrentlyToday = DateUtils.isToday(selectedDate)
                FilledTonalButton(
                    onClick = { onSelectDate(DateUtils.getTodayString()) },
                    enabled = !isCurrentlyToday,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Today", style = MaterialTheme.typography.bodySmall)
                }

                // Custom Date Picker Dialog Trigger
                IconButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.size(32.dp).testTag("select_date_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.EditCalendar,
                        contentDescription = "Pick random date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Date Picker Modal
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = DateUtils.parseDate(selectedDate)?.time ?: System.currentTimeMillis()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val selectedDateFormatted = DateUtils.getFormatter().format(Date(millis))
                                onSelectDate(selectedDateFormatted)
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("Select")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Compact monthly heatmap calendar
        CompactMonthlyCalendar(
            selectedDate = selectedDate,
            allDailyTasks = allDailyTasks,
            vocabQuizSets = vocabQuizSets,
            onSelectDate = onSelectDate,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        // Prominent Progress & Consistency Metric Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .testTag("home_progress_card")
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DATE CONSISTENCY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f%%", selectedConsistencyRatio),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        CircularProgressIndicator(
                            progress = { (selectedConsistencyRatio / 100f).toFloat() },
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            strokeWidth = 6.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                        Icon(
                            imageVector = if (selectedConsistencyRatio >= 100 && dailyTasks.isNotEmpty()) Icons.Default.CheckCircle else Icons.Default.TrendingUp,
                            contentDescription = "Success Icon",
                            tint = if (selectedConsistencyRatio >= 100 && dailyTasks.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val completedCount = dailyTasks.count { it.isCompleted }
                    val totalCount = dailyTasks.size
                    Text(
                        text = "Completed: $completedCount of $totalCount focus areas",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.testTag("overall_consistency_badge")
                    ) {
                        Text(
                            text = String.format(Locale.US, "Avg: %.1f%%", overallConsistencyRatio),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Daily task categories selector section
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Select Prep Focus Areas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Choose categories you want to work on for this date. Tapping a category adds it to your checklist.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Categories list Row
        if (categories.isEmpty()) {
            Text(
                text = "No categories available. Add categories in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Red,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("categories_selection_row")
            ) {
                items(categories) { category ->
                    val isSelected = remember(dailyTasks, category.name) {
                        dailyTasks.any { it.categoryName == category.name }
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onToggleCategory(selectedDate, category.name, !isSelected)
                        },
                        label = {
                            Text(
                                text = category.name,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.testTag("category_chip_${category.name.replace(" ", "_")}")
                    )
                }
            }
        }

        // Daily Checklist Sections
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Prep Checklist",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            FilledTonalIconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Manage focus areas",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (dailyTasks.isEmpty()) {
            // Friendly Empty State card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .testTag("home_checklist_empty")
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📝",
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your Prep List is Empty",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Build muscle memory! Select prep categories from the scrollable row above to commit to your focus schedule for $readableDate.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .testTag("home_checklist_items"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isDark = MaterialTheme.colorScheme.background.red < 0.5f
                val greenText = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                val greenBg = if (isDark) Color(0xFF1B3D22) else Color(0xFFE8F5E9)

                dailyTasks.forEach { task ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (task.isCompleted) greenBg else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (task.isCompleted) greenText.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompleted) 1.dp else 3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleTaskCompletion(task) }
                            .testTag("daily_task_item_${task.categoryName.replace(" ", "_")}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { onToggleTaskCompletion(task) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = greenText,
                                        uncheckedColor = MaterialTheme.colorScheme.outline,
                                        checkmarkColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("daily_task_checkbox_${task.categoryName.replace(" ", "_")}")
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = task.categoryName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (task.isCompleted) greenText else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Completion Status Pill
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (task.isCompleted) greenText.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (task.isCompleted) "PASSED" else "PENDING",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (task.isCompleted) greenText else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun CompactMonthlyCalendar(
    selectedDate: String,
    allDailyTasks: List<DailyTask>,
    vocabQuizSets: List<VocabQuizSet>,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) } // 0-11

    // Synchronize current calendar view month/year with the selected date whenever it changes
    LaunchedEffect(selectedDate) {
        val cal = Calendar.getInstance()
        DateUtils.parseDate(selectedDate)?.let {
            cal.time = it
            currentYear = cal.get(Calendar.YEAR)
            currentMonth = cal.get(Calendar.MONTH)
        }
    }

    val tasksByDate = remember(allDailyTasks) {
        allDailyTasks.groupBy { it.date }
    }

    val quizSetsByDate = remember(vocabQuizSets) {
        vocabQuizSets.groupBy { it.date }
    }

    val calendarInstance = remember(currentMonth, currentYear) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val daysInMonth = calendarInstance.getActualMaximum(Calendar.DAY_OF_MONTH)
    val dayOfWeekOffset = calendarInstance.get(Calendar.DAY_OF_WEEK) - 1

    val monthName = remember(currentMonth, currentYear) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, currentMonth)
            set(Calendar.YEAR, currentYear)
        }
        SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_calendar_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Month Header Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentMonth == 0) {
                            currentMonth = 11
                            currentYear -= 1
                        } else {
                            currentMonth -= 1
                        }
                    },
                    modifier = Modifier.size(36.dp).testTag("home_prev_month")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Month",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = monthName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = {
                        if (currentMonth == 11) {
                            currentMonth = 0
                            currentYear += 1
                        } else {
                            currentMonth += 1
                        }
                    },
                    modifier = Modifier.size(36.dp).testTag("home_next_month")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Month",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Weekdays Row (S, M, T, W, T, F, S)
            val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weekdays.forEach { dayLetter ->
                    Text(
                        text = dayLetter,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Grid Layout of Cells
            val totalCells = daysInMonth + dayOfWeekOffset
            val rowsCount = (totalCells + 6) / 7

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (r in 0 until rowsCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (c in 0..6) {
                            val cellIndex = r * 7 + c
                            val dayNumber = cellIndex - dayOfWeekOffset + 1

                            if (cellIndex < dayOfWeekOffset || dayNumber > daysInMonth) {
                                Box(modifier = Modifier.weight(1f))
                            } else {
                                val cellDateString = remember(dayNumber, currentMonth, currentYear) {
                                    val cal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, currentYear)
                                        set(Calendar.MONTH, currentMonth)
                                        set(Calendar.DAY_OF_MONTH, dayNumber)
                                    }
                                    DateUtils.getFormatter().format(cal.time)
                                }

                                val isSelected = selectedDate == cellDateString
                                val cellTasks = tasksByDate[cellDateString] ?: emptyList()
                                val cellQuizSets = quizSetsByDate[cellDateString] ?: emptyList()

                                val totalCount = cellTasks.size
                                val completedCount = cellTasks.count { it.isCompleted }
                                val consistencyRatio = if (totalCount == 0) 0.0 else (completedCount.toDouble() / totalCount) * 100.0
                                val hasQuizActivity = cellQuizSets.isNotEmpty()

                                // Determine background color & text color
                                val isDark = MaterialTheme.colorScheme.background.red < 0.5f
                                
                                val containerColor = when {
                                    totalCount == 0 -> {
                                        if (isDark) Color(0xFF1E242B) else Color(0xFFF0F4F8)
                                    }
                                    consistencyRatio == 0.0 -> {
                                        if (isDark) Color(0xFF4C1D1D) else Color(0xFFFFEBEE)
                                    }
                                    consistencyRatio <= 33.0 -> {
                                        if (isDark) Color(0xFF073C3C) else Color(0xFFE0F2F1)
                                    }
                                    consistencyRatio <= 66.0 -> {
                                        if (isDark) Color(0xFF0D5E59) else Color(0xFF80CBC4)
                                    }
                                    consistencyRatio < 100.0 -> {
                                        if (isDark) Color(0xFF1B827A) else Color(0xFF26A69A)
                                    }
                                    else -> {
                                        if (isDark) Color(0xFF1B4D22) else Color(0xFFA5D6A7)
                                    }
                                }

                                val textColor = when {
                                    totalCount == 0 -> {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    }
                                    consistencyRatio == 0.0 -> {
                                        if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828)
                                    }
                                    consistencyRatio <= 33.0 -> {
                                        if (isDark) Color(0xFF80CBC4) else Color(0xFF004D40)
                                    }
                                    consistencyRatio <= 66.0 -> {
                                        if (isDark) Color(0xFFE0F2F1) else Color(0xFF004D40)
                                    }
                                    consistencyRatio < 100.0 -> {
                                        if (isDark) Color.White else Color(0xFF00332C)
                                    }
                                    else -> {
                                        if (isDark) Color(0xFFC8E6C9) else Color(0xFF1B5E20)
                                    }
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(containerColor)
                                        .border(
                                            width = if (isSelected) 2.dp else 0.5.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            onSelectDate(cellDateString)
                                        }
                                        .testTag("home_calendar_cell_$dayNumber")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = "$dayNumber",
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = textColor,
                                            fontSize = 13.sp
                                        )
                                        
                                        // Quiz Activity Indicator Dot
                                        if (hasQuizActivity) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Calendar Heatmap Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Less",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(6.dp))

                // Color tiles of legend
                val isDark = MaterialTheme.colorScheme.background.red < 0.5f
                val legendColors = listOf(
                    if (isDark) Color(0xFF1E242B) else Color(0xFFF0F4F8),
                    if (isDark) Color(0xFF4C1D1D) else Color(0xFFFFEBEE),
                    if (isDark) Color(0xFF073C3C) else Color(0xFFE0F2F1),
                    if (isDark) Color(0xFF0D5E59) else Color(0xFF80CBC4),
                    if (isDark) Color(0xFF1B827A) else Color(0xFF26A69A),
                    if (isDark) Color(0xFF1B4D22) else Color(0xFFA5D6A7)
                )

                legendColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "More",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Quiz indicator legend
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Quiz Set",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
