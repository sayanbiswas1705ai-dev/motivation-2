package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.os.Build
import com.example.data.model.StudyDay
import com.example.data.model.UserStats
import com.example.data.model.CustomTask
import com.example.data.api.GeminiClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

@Composable
fun HomeScreen(
    stats: UserStats?,
    todayTask: StudyDay?,
    currentActiveMonth: Int,
    completedDays: Int,
    overallProgress: Double,
    monthlyProgressMap: Map<Int, Double>,
    customTasks: List<CustomTask> = emptyList(),
    onCompleteTask: (StudyDay) -> Unit,
    onSaveReminder: (Int, Int, Boolean) -> Unit,
    onLogFocusTime: (Int) -> Unit,
    onAddCustomTask: (CustomTask) -> Unit = {},
    onUpdateCustomTask: (CustomTask) -> Unit = {},
    onDeleteCustomTask: (CustomTask) -> Unit = {},
    onToggleCustomTask: (CustomTask) -> Unit = {}
) {
    val dateString = remember {
        val sdf = SimpleDateFormat("EEEE, MMMM dd", Locale.US)
        sdf.format(Date())
    }

    var completedSubTasks by remember(todayTask?.dayId) {
        mutableStateOf(mapOf(1 to false, 2 to false, 3 to false))
    }

    var showResetConfirmDialog by remember {
        mutableStateOf(false)
    }

    val initialHour = stats?.reminderHour ?: 9
    val initialMinute = stats?.reminderMinute ?: 0
    val isEnabled = stats?.isReminderEnabled ?: true

    var selectedHour by remember(stats) { mutableStateOf(initialHour) }
    var selectedMinute by remember(stats) { mutableStateOf(initialMinute) }
    var reminderEnabled by remember(stats) { mutableStateOf(isEnabled) }

    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            reminderEnabled = true
            onSaveReminder(selectedHour, selectedMinute, true)
        } else {
            reminderEnabled = false
            onSaveReminder(selectedHour, selectedMinute, false)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    var studyTipState by remember(todayTask?.dayId) {
        mutableStateOf<String?>(null)
    }
    var isFetchingTip by remember(todayTask?.dayId) {
        mutableStateOf(false)
    }

    val moduleName = remember(currentActiveMonth) { getThemeName(currentActiveMonth) }
    val progressPercent = remember(monthlyProgressMap, currentActiveMonth) {
        val progressVal = (monthlyProgressMap[currentActiveMonth] ?: 0.0)
        (progressVal * 100).toInt()
    }

    val totalModulesCompleted = remember(monthlyProgressMap) {
        monthlyProgressMap.values.count { it >= 1.0 }
    }
    val unlockedDaysCount = remember(currentActiveMonth) {
        currentActiveMonth * 30
    }
    val consistencyPercent = remember(completedDays, unlockedDaysCount) {
        if (unlockedDaysCount > 0) {
            ((completedDays * 100) / unlockedDaysCount).coerceIn(0, 100)
        } else {
            0
        }
    }

    var showAddEditTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<CustomTask?>(null) }

    val todayDateStr = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.format(Date())
    }

    var searchQuery by remember { mutableStateOf("") }

    val sortedCustomTasks = remember(customTasks, todayDateStr) {
        customTasks.filter { it.date == todayDateStr }.sortedBy { it.startTime }
    }

    val filteredCustomTasks = remember(sortedCustomTasks, searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            sortedCustomTasks
        } else {
            val q = searchQuery.trim().lowercase()
            sortedCustomTasks.filter {
                it.title.lowercase().contains(q) || it.category.lowercase().contains(q)
            }
        }
    }

    val pendingTasks = remember(filteredCustomTasks) {
        filteredCustomTasks.filter { !it.isCompleted }
    }

    val completedTasks = remember(filteredCustomTasks) {
        filteredCustomTasks.filter { it.isCompleted }
    }

    val customProgress = remember(sortedCustomTasks) {
        val total = sortedCustomTasks.size
        val done = sortedCustomTasks.count { it.isCompleted }
        if (total == 0) 0f else done.toFloat() / total.toFloat()
    }

    val customProgressPercent = remember(customProgress) {
        (customProgress * 100).toInt()
    }

    LaunchedEffect(todayTask?.dayId) {
        if (todayTask != null && studyTipState == null) {
            isFetchingTip = true
            studyTipState = GeminiClient.getStudyTip(
                moduleName = moduleName,
                progressPercent = progressPercent,
                todayTopic = todayTask.title,
                todayDescription = todayTask.description
            )
            isFetchingTip = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Welcome Header Group (exactly matches HTML design)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dateString.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sayan's Initiative",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Streak Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stats?.streakCount ?: 0}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // User Initials Badge
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = "A",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // Beautiful Search Bar (Material 3 style) to filter tasks by title or category
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { 
                    Text(
                        text = "Search tasks by title or category...", 
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ) 
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_search_bar"),
                singleLine = true
            )
        }

        // Active Milestone Block (Tailwind bg-[#3F4759] Rounded 28px card vibe)
        item {
            val progressVal = (monthlyProgressMap[currentActiveMonth] ?: 0.0)
            val progressPercent = (progressVal * 100).toInt()
            val themeTitle = moduleName
            val completedDaysInMonth = (progressVal * 30).toInt()

            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("current_month_card")
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Decorative glowing background circle
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 24.dp, y = 24.dp)
                            .size(128.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "CURRENT PROGRESS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Month ${String.format("%02d", currentActiveMonth)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = themeTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Circular Progress Indicator corresponding to the Tailwind view
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(64.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { progressVal.toFloat() },
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Text(
                                    text = "$progressPercent%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "STATUS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$completedDaysInMonth / 30 Days",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // Summary Performance Dashboard Widget
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    text = "PERFORMANCE METRICS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("summary_dashboard_widget")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Metric 1: Total Modules Completed
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_modules_completed"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Modules Completed Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$totalModulesCompleted / 12",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Modules Done",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                fontSize = 10.sp
                            )
                        }

                        // Custom Vertical Divider 1
                        Spacer(
                            modifier = Modifier
                                .width(1.dp)
                                .height(36.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        )

                        // Metric 2: Current Streak
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_current_streak"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = "Streak Icon",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${stats?.streakCount ?: 0} Days",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Study Streak",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                fontSize = 10.sp
                            )
                        }

                        // Custom Vertical Divider 2
                        Spacer(
                            modifier = Modifier
                                .width(1.dp)
                                .height(36.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        )

                        // Metric 3: Total Study Focus Minutes (saved from Pomodoro)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_total_focus_minutes"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = "Total Focus Time Icon",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${stats?.totalFocusMinutes ?: 0} Min",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Focused Time",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                fontSize = 10.sp
                            )
                        }

                        // Custom Vertical Divider 3
                        Spacer(
                            modifier = Modifier
                                .width(1.dp)
                                .height(36.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        )

                        // Metric 4: Study Consistency
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_consistency"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "Consistency Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$consistencyPercent%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Consistency",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // AI Study Coach Tip & Goal Card
        if (todayTask != null) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        text = "AI STUDY COACH",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_study_coach_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Coach Icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Daily Coach Tip & Goal",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (!isFetchingTip) {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                isFetchingTip = true
                                                studyTipState = GeminiClient.getStudyTip(
                                                    moduleName = moduleName,
                                                    progressPercent = progressPercent,
                                                    todayTopic = todayTask.title,
                                                    todayDescription = todayTask.description
                                                )
                                                isFetchingTip = false
                                            }
                                        },
                                        modifier = Modifier.size(24.dp).testTag("ai_regenerate_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Regenerate tip",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (isFetchingTip) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp).testTag("ai_loading_indicator"),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "AI is polishing your coaching insights...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Text(
                                    text = studyTipState ?: "Starting up your AI coach advice...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            if (todayTask != null) {
                // Pomodoro Study Focus Timer Section Component
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "FOCUS ASSISTANT (POMODORO)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                PomodoroTimerCard(
                    todayTaskTitle = todayTask.title,
                    onLogFocusTime = onLogFocusTime
                )
            } else {
                // If todayTask is null, all days in current month are completed
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🏆",
                            fontSize = 44.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Month $currentActiveMonth Complete!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Amazing! You've successfully finished all day tasks for this study month. Review your progress in the Calendar and Awards dashboards as you unlock the future modules.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // TODAY'S CUSTOM TASKS SECTION
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TODAY'S CUSTOM TASKS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.2.sp
                    )
                    
                    if (sortedCustomTasks.isNotEmpty()) {
                        Text(
                            text = "$customProgressPercent% Done",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (customProgressPercent == 100) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().testTag("custom_tasks_container")
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        if (sortedCustomTasks.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "🎯",
                                    fontSize = 32.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No custom tasks for today",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap the '+' floating button at the bottom right to schedule custom study, revision, practice sessions, or exam prep!",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            if (filteredCustomTasks.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "🔍",
                                        fontSize = 32.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No matching tasks found",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Try searching for a different title or category keyword.",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LinearProgressIndicator(
                                    progress = { customProgress },
                                    color = if (customProgressPercent == 100) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .testTag("custom_tasks_progress_bar")
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // 1. Pending Tasks Section
                                if (pendingTasks.isNotEmpty()) {
                                    Text(
                                        text = "PENDING",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    pendingTasks.forEach { task ->
                                        CustomTaskItemRow(
                                            task = task,
                                            onToggle = onToggleCustomTask,
                                            onEdit = { taskToEdit = task; showAddEditTaskDialog = true },
                                            onDelete = onDeleteCustomTask
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }

                                if (pendingTasks.isNotEmpty() && completedTasks.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                // 2. Completed Tasks Section
                                if (completedTasks.isNotEmpty()) {
                                    Text(
                                        text = "COMPLETED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    completedTasks.forEach { task ->
                                        CustomTaskItemRow(
                                            task = task,
                                            onToggle = onToggleCustomTask,
                                            onEdit = { taskToEdit = task; showAddEditTaskDialog = true },
                                            onDelete = onDeleteCustomTask
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Interactive Study Reminder Setup Card
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "STUDY CONSISTENCY REMINDER",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("study_reminder_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Clock Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Daily Study Alert",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            reminderEnabled = true
                                            onSaveReminder(selectedHour, selectedMinute, true)
                                        }
                                    } else {
                                        reminderEnabled = false
                                        onSaveReminder(selectedHour, selectedMinute, false)
                                    }
                                },
                                modifier = Modifier.testTag("home_reminder_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        AnimatedVisibility(
                            visible = reminderEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Alert scheduled daily at ${String.format("%02d:%02d", selectedHour, selectedMinute)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Hour Selector
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "HOUR",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    selectedHour = if (selectedHour == 0) 23 else selectedHour - 1
                                                    onSaveReminder(selectedHour, selectedMinute, reminderEnabled)
                                                },
                                                modifier = Modifier.size(36.dp).testTag("home_dec_hour")
                                            ) {
                                                Icon(Icons.Default.Remove, "Decrease hour", tint = MaterialTheme.colorScheme.secondary)
                                            }
                                            Text(
                                                text = String.format("%02d", selectedHour),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )
                                            IconButton(
                                                onClick = {
                                                    selectedHour = if (selectedHour == 23) 0 else selectedHour + 1
                                                    onSaveReminder(selectedHour, selectedMinute, reminderEnabled)
                                                },
                                                modifier = Modifier.size(36.dp).testTag("home_inc_hour")
                                            ) {
                                                Icon(Icons.Default.Add, "Increase hour", tint = MaterialTheme.colorScheme.secondary)
                                            }
                                        }
                                    }

                                    Text(
                                        text = ":",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Minute Selector
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "MINUTE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    selectedMinute = if (selectedMinute == 0) 55 else (selectedMinute - 5) / 5 * 5
                                                    onSaveReminder(selectedHour, selectedMinute, reminderEnabled)
                                                },
                                                modifier = Modifier.size(36.dp).testTag("home_dec_minute")
                                            ) {
                                                Icon(Icons.Default.Remove, "Decrease minute", tint = MaterialTheme.colorScheme.secondary)
                                            }
                                            Text(
                                                text = String.format("%02d", selectedMinute),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )
                                            IconButton(
                                                onClick = {
                                                    selectedMinute = if (selectedMinute == 55) 0 else (selectedMinute + 5) / 5 * 5
                                                    onSaveReminder(selectedHour, selectedMinute, reminderEnabled)
                                                },
                                                modifier = Modifier.size(36.dp).testTag("home_inc_minute")
                                            ) {
                                                Icon(Icons.Default.Add, "Increase minute", tint = MaterialTheme.colorScheme.secondary)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (!reminderEnabled) {
                            Text(
                                text = "Keep reminders active to trigger alerts and complete daily challenges consistently.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Overall Global Progress Tracker Block (styled beautifully like the grid summaries)
        item {
            Text(
                text = "OVERALL METRICS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Stat Box: Overall Progress
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("overall_progress_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "OVERALL PROGRESS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f%%", overallProgress * 100),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { overallProgress.toFloat() },
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }

                // Right Stat Box: Remaining
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "REMAINING",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${360 - completedDays} Days",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "to reach Month 12",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Floating Action Button (FAB) for custom task entry (Requirement 1)
        FloatingActionButton(
            onClick = {
                taskToEdit = null
                showAddEditTaskDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 80.dp)
                .testTag("add_custom_task_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add custom task"
            )
        }
    }

    if (showAddEditTaskDialog) {
        AddEditTaskDialog(
            taskToEdit = taskToEdit,
            onDismiss = { showAddEditTaskDialog = false; taskToEdit = null },
            onSave = { task ->
                if (task.id == 0) {
                    onAddCustomTask(task)
                } else {
                    onUpdateCustomTask(task)
                }
                showAddEditTaskDialog = false
                taskToEdit = null
            }
        )
    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTaskItemRow(
    task: CustomTask,
    onToggle: (CustomTask) -> Unit,
    onEdit: () -> Unit,
    onDelete: (CustomTask) -> Unit
) {
    val categoryEmoji = when (task.category) {
        "Study" -> "📚"
        "Revision" -> "🔄"
        "Practice" -> "✍️"
        "Exam" -> "📝"
        "Personal" -> "👤"
        else -> "⚙️"
    }

    val priorityColor = when (task.priority) {
        "High" -> MaterialTheme.colorScheme.error
        "Medium" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    val priorityContainerColor = when (task.priority) {
        "High" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        "Medium" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
    }

    val isCompleted = task.isCompleted

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete(task)
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val color = MaterialTheme.colorScheme.error
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Task",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) 
                                 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
            modifier = Modifier.fillMaxWidth().testTag("custom_task_item_${task.id}")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val priorityIndicatorColor = when (task.priority) {
                    "High" -> Color(0xFFEF4444)
                    "Medium" -> Color(0xFFFBBF24)
                    "Low" -> Color(0xFF3B82F6)
                    else -> Color(0xFF3B82F6)
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(5.dp)
                        .background(priorityIndicatorColor)
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onToggle(task) },
                        modifier = Modifier.size(28.dp).testTag("custom_task_check_${task.id}")
                    ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isCompleted) MaterialTheme.colorScheme.secondary else Color.Transparent)
                            .border(
                                2.dp,
                                if (isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                CircleShape
                            )
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "$categoryEmoji ${task.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(priorityContainerColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.priority,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = priorityColor,
                                fontSize = 9.sp
                            )
                        }
                    }

                    if (task.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isCompleted) 0.5f else 1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Time icon",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = if (isCompleted) 0.5f else 1f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${task.startTime} - ${task.endTime} (${task.durationMinutes} mins)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isCompleted) 0.5f else 1f),
                        )
                        
                        if (task.isReminderEnabled) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Reminder Active",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = if (isCompleted) 0.5f else 1f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp).testTag("custom_task_edit_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit task",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { onDelete(task) },
                        modifier = Modifier.size(32.dp).testTag("custom_task_delete_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete task",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
fun AddEditTaskDialog(
    taskToEdit: CustomTask? = null,
    onDismiss: () -> Unit,
    onSave: (CustomTask) -> Unit
) {
    val context = LocalContext.current
    val todayDateStr = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.format(Date())
    }

    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var desc by remember { mutableStateOf(taskToEdit?.description ?: "") }
    var date by remember { mutableStateOf(taskToEdit?.date ?: todayDateStr) }
    var startTime by remember { mutableStateOf(taskToEdit?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(taskToEdit?.endTime ?: "10:00") }
    var durationMinutes by remember { mutableStateOf(taskToEdit?.durationMinutes?.toString() ?: "60") }
    var category by remember { mutableStateOf(taskToEdit?.category ?: "Study") }
    var priority by remember { mutableStateOf(taskToEdit?.priority ?: "Medium") }
    var isReminderEnabled by remember { mutableStateOf(taskToEdit?.isReminderEnabled ?: false) }

    fun calculateDuration(start: String, end: String): Int {
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.US)
            val startTimeVal = sdf.parse(start)!!
            val endTimeVal = sdf.parse(end)!!
            var diff = endTimeVal.time - startTimeVal.time
            if (diff < 0) {
                diff += 24 * 60 * 60 * 1000
            }
            (diff / (60 * 1000)).toInt()
        } catch (e: Exception) {
            60
        }
    }

    LaunchedEffect(startTime, endTime) {
        val calculated = calculateDuration(startTime, endTime)
        durationMinutes = calculated.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (taskToEdit == null) "Add Custom Task" else "Edit Task",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    placeholder = { Text("Enter task title...") },
                    modifier = Modifier.fillMaxWidth().testTag("add_task_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Enter short description...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (yyyy-MM-dd)") },
                    trailingIcon = {
                        IconButton(onClick = {
                            val cal = Calendar.getInstance()
                            try {
                                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
                                if (parsed != null) cal.time = parsed
                            } catch (e: Exception) {}
                            
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    date = String.format("%04d-%02d-%02d", year, month + 1, day)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Starts") },
                        trailingIcon = {
                            IconButton(onClick = {
                                val cal = Calendar.getInstance()
                                try {
                                    val parts = startTime.split(":")
                                    cal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                                    cal.set(Calendar.MINUTE, parts[1].toInt())
                                } catch (e: Exception) {}

                                android.app.TimePickerDialog(
                                    context,
                                    { _, hr, min ->
                                        startTime = String.format("%02d:%02d", hr, min)
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    true
                                ).show()
                            }) {
                                Icon(Icons.Default.Schedule, contentDescription = "Pick Start Time")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        readOnly = true
                    )

                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("Ends") },
                        trailingIcon = {
                            IconButton(onClick = {
                                val cal = Calendar.getInstance()
                                try {
                                    val parts = endTime.split(":")
                                    cal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                                    cal.set(Calendar.MINUTE, parts[1].toInt())
                                } catch (e: Exception) {}

                                android.app.TimePickerDialog(
                                    context,
                                    { _, hr, min ->
                                        endTime = String.format("%02d:%02d", hr, min)
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    true
                                ).show()
                            }) {
                                Icon(Icons.Default.Schedule, contentDescription = "Pick End Time")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        readOnly = true
                    )
                }

                OutlinedTextField(
                    value = durationMinutes,
                    onValueChange = { durationMinutes = it },
                    label = { Text("Duration (minutes)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_task_duration_input"),
                    singleLine = true
                )

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                val categories = listOf("Study", "Revision", "Practice", "Exam", "Personal", "Other")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSel = category == cat
                        val chipColor = if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent
                        val chipBorder = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        val textColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipColor)
                                .border(1.dp, chipBorder, RoundedCornerShape(8.dp))
                                .clickable { category = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = textColor
                            )
                        }
                    }
                }

                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                val priorities = listOf("Low", "Medium", "High")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    priorities.forEach { prio ->
                        val isSel = priority == prio
                        val color = when (prio) {
                            "High" -> MaterialTheme.colorScheme.errorContainer
                            "Medium" -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }
                        val chipColor = if (isSel) color else Color.Transparent
                        val chipBorder = if (isSel) color else MaterialTheme.colorScheme.outline
                        val textColor = if (isSel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipColor)
                                .border(1.dp, chipBorder, RoundedCornerShape(8.dp))
                                .clickable { priority = prio }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = prio,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = textColor
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Reminder Notification",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Notify when the task starts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isReminderEnabled,
                        onCheckedChange = { isReminderEnabled = it },
                        modifier = Modifier.testTag("reminder_active_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    val durationVal = durationMinutes.toIntOrNull() ?: 60
                    val task = CustomTask(
                        id = taskToEdit?.id ?: 0,
                        title = title,
                        description = desc,
                        date = date,
                        startTime = startTime,
                        endTime = endTime,
                        durationMinutes = durationVal,
                        category = category,
                        priority = priority,
                        isReminderEnabled = isReminderEnabled,
                        isCompleted = taskToEdit?.isCompleted ?: false
                    )
                    onSave(task)
                },
                enabled = title.isNotBlank()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helpers
fun getThemeName(monthId: Int): String {
    return when (monthId) {
        1 -> "Foundations of Programming"
        2 -> "Data Structures & Complexity"
        3 -> "Object-Oriented Design"
        4 -> "Relational & NoSQL Databases"
        5 -> "UI/UX & Mobile Engineering"
        6 -> "Client-Server Networking"
        7 -> "Asynchronous Coroutines & State"
        8 -> "Backend & API Engineering"
        9 -> "Software Testing & QA"
        10 -> "DevOps & Cloud Systems"
        11 -> "AI & Machine Learning Basics"
        12 -> "Advanced System Architecture"
        else -> "Professional Study Theme"
    }
}

@Composable
fun PomodoroTimerCard(
    todayTaskTitle: String,
    onLogFocusTime: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Timer states
    var selectedDurationMinutes by remember { mutableStateOf(25) }
    var remainingSeconds by remember { mutableStateOf(25 * 60) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var totalElapsedSecondsThisSession by remember { mutableStateOf(0) }
    
    // Ticker logic
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds -= 1
                totalElapsedSecondsThisSession += 1
            }
            if (remainingSeconds == 0) {
                // Timer fully completed!
                val minutesToLog = selectedDurationMinutes
                onLogFocusTime(minutesToLog)
                android.widget.Toast.makeText(
                    context,
                    "🎉 Focus Session Complete! Logged $minutesToLog minutes to dashboard.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                
                // Reset State
                isTimerRunning = false
                isPaused = false
                remainingSeconds = selectedDurationMinutes * 60
                totalElapsedSecondsThisSession = 0
            }
        }
    }
    
    // Handle duration selection/changing
    fun selectDuration(minutes: Int) {
        isTimerRunning = false
        isPaused = false
        selectedDurationMinutes = minutes
        remainingSeconds = minutes * 60
        totalElapsedSecondsThisSession = 0
    }
    
    // Progress calculation
    val progressRatio = remember(remainingSeconds, selectedDurationMinutes) {
        val totalSecs = selectedDurationMinutes * 60
        if (totalSecs > 0) remainingSeconds.toFloat() / totalSecs else 0f
    }
    
    val timeLabel = remember(remainingSeconds) {
        val mins = remainingSeconds / 60
        val secs = remainingSeconds % 60
        String.format("%02d:%02d", mins, secs)
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("pomodoro_timer_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Pomodoro Timer Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "FOCUS POMODORO TIMER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.1.sp
                        )
                        Text(
                            text = "Studying: $todayTaskTitle",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Preset badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Focus Mode",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Circular progress countdown visualization
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Background Circle Track
                CircularProgressIndicator(
                    progress = { 1f },
                    strokeWidth = 6.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Colored Circle Fill
                CircularProgressIndicator(
                    progress = { progressRatio },
                    strokeWidth = 8.dp,
                    color = MaterialTheme.colorScheme.primary,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                    modifier = Modifier.fillMaxSize()
                )

                // Time Indicator Text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("pomodoro_char_countdown")
                    )
                    Text(
                        text = if (isTimerRunning) "STAY FOCUSED" else "READY",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Presets row to toggle session length
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val presets = listOf(
                    Triple(10, "10m", "pomodoro_preset_10"),
                    Triple(25, "25m", "pomodoro_preset_25"),
                    Triple(45, "45m", "pomodoro_preset_45"),
                    Triple(1, "1m", "pomodoro_preset_1")
                )

                presets.forEach { (mins, label, tag) ->
                    val isSelected = selectedDurationMinutes == mins
                    OutlinedButton(
                        onClick = { selectDuration(mins) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag(tag)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pause / Start
                Button(
                    onClick = {
                        isTimerRunning = !isTimerRunning
                        isPaused = !isTimerRunning
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTimerRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1.2f)
                        .testTag("pomodoro_toggle")
                ) {
                    Icon(
                        imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle Timer State"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isTimerRunning) "Pause" else "Start Timer",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Reset
                OutlinedButton(
                    onClick = {
                        isTimerRunning = false
                        isPaused = false
                        remainingSeconds = selectedDurationMinutes * 60
                        totalElapsedSecondsThisSession = 0
                    },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .height(48.dp)
                        .weight(0.8f)
                        .testTag("pomodoro_reset")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart Timer icon"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(14.dp))

            // Custom "Instant Save/Log Focus" shortcut
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Forgot to track study?",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Log extra focus minutes directly to dashboard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            onLogFocusTime(5)
                            android.widget.Toast.makeText(context, "Logged +5 Min Focus!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("pomodoro_add_5m")
                    ) {
                        Text("+5m", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = {
                            onLogFocusTime(25)
                            android.widget.Toast.makeText(context, "Logged +25 Min Focus!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("pomodoro_add_25m")
                    ) {
                        Text("+25m", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
