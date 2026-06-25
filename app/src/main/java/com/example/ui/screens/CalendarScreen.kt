package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyTask
import com.example.data.model.DateUtils
import com.example.data.model.VocabQuizSet
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    allDailyTasks: List<DailyTask>,
    vocabQuizSets: List<VocabQuizSet>,
    selectedDate: String,
    onSelectDate: (String) -> Unit,
    onToggleTaskCompletion: (DailyTask) -> Unit
) {
    val scrollState = rememberScrollState()

    // Calendar view state
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) } // 0-11

    // Sync year and month with external selectedDate whenever it changes
    LaunchedEffect(selectedDate) {
        val cal = Calendar.getInstance()
        DateUtils.parseDate(selectedDate)?.let {
            cal.time = it
            selectedYear = cal.get(Calendar.YEAR)
            selectedMonth = cal.get(Calendar.MONTH)
        }
    }

    // Group daily tasks by date
    val tasksByDate = remember(allDailyTasks) {
        allDailyTasks.groupBy { it.date }
    }

    // Group vocab quiz sets by date
    val quizSetsByDate = remember(vocabQuizSets) {
        vocabQuizSets.groupBy { it.date }
    }

    // Days in current selected month
    val calendarInstance = remember(selectedMonth, selectedYear) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val daysInMonth = calendarInstance.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Day of week of the first day (1: Sunday, 2: Monday, ..., 7: Saturday)
    // Convert to 0-indexed offset (0: Sunday, 1: Monday, ..., 6: Saturday)
    val dayOfWeekOffset = calendarInstance.get(Calendar.DAY_OF_WEEK) - 1

    val monthName = remember(selectedMonth, selectedYear) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.YEAR, selectedYear)
        }
        SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Prep Calendar Map",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Inspect date-by-date consistency ratios and focus task checklists.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Month Selector Header Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (selectedMonth == 0) {
                            selectedMonth = 11
                            selectedYear -= 1
                        } else {
                            selectedMonth -= 1
                        }
                    },
                    modifier = Modifier.testTag("prev_month_button")
                ) {
                    Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                }

                Text(
                    text = monthName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = {
                        if (selectedMonth == 11) {
                            selectedMonth = 0
                            selectedYear += 1
                        } else {
                            selectedMonth += 1
                        }
                    },
                    modifier = Modifier.testTag("next_month_button")
                ) {
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Month")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Days of week header row
        val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            weekdays.forEach { dayName ->
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid Calendar Construction
        val totalCells = daysInMonth + dayOfWeekOffset
        val rowsCount = (totalCells + 6) / 7

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("calendar_grid_layout"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (r in 0 until rowsCount) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (c in 0..6) {
                        val cellIndex = r * 7 + c
                        val dayNumber = cellIndex - dayOfWeekOffset + 1

                        if (cellIndex < dayOfWeekOffset || dayNumber > daysInMonth) {
                            // Blank spacers
                            Box(modifier = Modifier.weight(1f))
                        } else {
                            // Formatted date string for compliance
                            val cellDateString = remember(dayNumber, selectedMonth, selectedYear) {
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, selectedYear)
                                    set(Calendar.MONTH, selectedMonth)
                                    set(Calendar.DAY_OF_MONTH, dayNumber)
                                }
                                DateUtils.getFormatter().format(cal.time)
                            }

                            val isSelected = selectedDate == cellDateString
                            val cellTasks = tasksByDate[cellDateString] ?: emptyList()
                            val cellQuizSets = quizSetsByDate[cellDateString] ?: emptyList()
                            val hasQuizActivity = cellQuizSets.isNotEmpty()

                            // Calculate cell metrics
                            val totalCount = cellTasks.size
                            val completedCount = cellTasks.count { it.isCompleted }
                            val consistencyRatio = if (totalCount == 0) 0.0 else (completedCount.toDouble() / totalCount) * 100.0

                            // Styling based on consistency ratio (heatmap style)
                            val isDark = MaterialTheme.colorScheme.background.red < 0.5f
                            val containerColor = when {
                                totalCount == 0 -> {
                                    if (isDark) Color(0xFF1E242B) else Color(0xFFEBF0F2)
                                }
                                consistencyRatio == 0.0 -> {
                                    if (isDark) Color(0xFF3F1D1D) else Color(0xFFFFECEB)
                                }
                                consistencyRatio <= 33.0 -> {
                                    if (isDark) Color(0xFF073838) else Color(0xFFE0F2F1)
                                }
                                consistencyRatio <= 66.0 -> {
                                    if (isDark) Color(0xFF0A5252) else Color(0xFFB2DFDB)
                                }
                                consistencyRatio < 100.0 -> {
                                    if (isDark) Color(0xFF0C6B6B) else Color(0xFF4DB6AC)
                                }
                                else -> {
                                    if (isDark) Color(0xFF1B3D22) else Color(0xFFA5D6A7)
                                }
                            }

                            val textColor = when {
                                totalCount == 0 -> {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                }
                                consistencyRatio == 0.0 -> {
                                    if (isDark) Color(0xFFFFB4AB) else Color(0xFFBA1A1A)
                                }
                                consistencyRatio <= 33.0 -> {
                                    if (isDark) Color(0xFF80CBC4) else Color(0xFF004D40)
                                }
                                consistencyRatio <= 66.0 -> {
                                    if (isDark) Color(0xFFB2DFDB) else Color(0xFF004D40)
                                }
                                consistencyRatio < 100.0 -> {
                                    if (isDark) Color(0xFFE0F2F1) else Color(0xFF00332C)
                                }
                                else -> {
                                    if (isDark) Color(0xFF81C784) else Color(0xFF1B5E20)
                                }
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(containerColor)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        onSelectDate(cellDateString)
                                    }
                                    .testTag("calendar_cell_$dayNumber")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$dayNumber",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textColor
                                    )
                                    if (totalCount > 0) {
                                        Text(
                                            text = "${completedCount}/${totalCount}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = textColor.copy(alpha = 0.8f)
                                        )
                                    }
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

        Spacer(modifier = Modifier.height(8.dp))

        // Quiz Indicator Legend on Calendar Screen
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Indicates generated Quiz Set exists for date",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Selected Date Summary Checklist Drawer
        val selectedDateReadable = remember(selectedDate) {
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

        val selectedDayTasks = tasksByDate[selectedDate] ?: emptyList()

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 100.dp)
                .testTag("calendar_day_drawer")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "FOCUS SUMMARY FOR $selectedDateReadable",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedDayTasks.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No syllabus categories were selected for prep on this date.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val completed = selectedDayTasks.count { it.isCompleted }
                    val total = selectedDayTasks.size
                    val percent = (completed.toFloat() / total * 100).toInt()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Completion: $percent%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$completed/$total focus areas passed",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
                    val greenText = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                    val greenBg = if (isDark) Color(0xFF1B3D22) else Color(0xFFE8F5E9)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        selectedDayTasks.forEach { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (task.isCompleted) greenBg
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(0.5.dp, if (task.isCompleted) greenText.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .clickable { onToggleTaskCompletion(task) }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = { onToggleTaskCompletion(task) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = greenText,
                                            uncheckedColor = MaterialTheme.colorScheme.outline,
                                            checkmarkColor = Color.White
                                        ),
                                        modifier = Modifier.size(24.dp).testTag("calendar_task_todo_${task.categoryName}")
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = task.categoryName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (task.isCompleted) greenText else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Text(
                                    text = if (task.isCompleted) "PASSED" else "PENDING",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = if (task.isCompleted) greenText else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
