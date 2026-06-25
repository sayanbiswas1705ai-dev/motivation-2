package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VocabQuizQuestion
import com.example.data.model.VocabQuizSet
import com.example.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabQuizScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vocabQuizSets by viewModel.vocabQuizSets.collectAsState()
    val activeQuizSet by viewModel.activeQuizSet.collectAsState()
    val activeQuestions by viewModel.activeQuestions.collectAsState()
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()
    val uploadStatus by viewModel.uploadStatus.collectAsState()
    val showFeedback by viewModel.showFeedback.collectAsState()
    val lastSelectedOption by viewModel.lastSelectedOption.collectAsState()

    // Flag to determine if we are in "Review Mode" (viewing past completed quiz)
    // vs "Taking Active Quiz" mode. If active quiz has all answered questions, it's review mode.
    val isAllAnswered = activeQuestions.isNotEmpty() && activeQuestions.all { it.isAnswered }

    var pendingUploadUri by remember { mutableStateOf<Uri?>(null) }
    var pendingUploadFileName by remember { mutableStateOf("") }
    var showPdfConfigDialog by remember { mutableStateOf(false) }
    var showManualTextDialog by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }

    var pdfStartPage by remember { mutableStateOf("") }
    var pdfEndPage by remember { mutableStateOf("") }

    var manualTextContent by remember { mutableStateOf("") }
    var manualTextTitle by remember { mutableStateOf("") }

    var pastedUrl by remember { mutableStateOf("") }
    var urlStartPage by remember { mutableStateOf("") }
    var urlEndPage by remember { mutableStateOf("") }

    var quizToDelete by remember { mutableStateOf<VocabQuizSet?>(null) }

    // PDF selection launcher
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileName(context, uri) ?: "vocabulary.pdf"
            if (fileName.lowercase().endsWith(".pdf")) {
                pendingUploadUri = uri
                pendingUploadFileName = fileName
                pdfStartPage = ""
                pdfEndPage = ""
                showPdfConfigDialog = true
            } else {
                viewModel.uploadAndGenerateQuiz(uri, fileName)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (activeQuizSet == null) {
            // 1. DASHBOARD MODE (No active quiz selected)
            DashboardView(
                vocabQuizSets = vocabQuizSets,
                uploadStatus = uploadStatus,
                onSelectQuiz = { quizSet -> viewModel.selectQuizSet(quizSet) },
                onUploadClick = { pdfLauncher.launch("*/*") },
                onManualTextClick = { showManualTextDialog = true },
                onUrlClick = { showUrlDialog = true },
                onDeleteQuizSet = { quizToDelete = it },
                onDismissError = { viewModel.resetUploadStatus() }
            )
        } else if (isAllAnswered) {
            // 2. REVIEW MODE (Viewing a past completed quiz)
            ReviewQuizView(
                quizSet = activeQuizSet!!,
                questions = activeQuestions,
                onBackToDashboard = { viewModel.selectQuizSet(null) },
                onReattemptClick = { viewModel.reattemptQuiz(activeQuizSet!!.id) }
            )
        } else {
            // 3. ACTIVE QUIZ TAKING MODE
            ActiveQuizTakingView(
                quizSet = activeQuizSet!!,
                questions = activeQuestions,
                currentIndex = currentQuestionIndex,
                showFeedback = showFeedback,
                lastSelectedOption = lastSelectedOption,
                onOptionSelect = { q, idx -> viewModel.submitAnswer(q, idx) },
                onNextQuestion = { viewModel.nextQuestion() },
                onBackToDashboard = { viewModel.selectQuizSet(null) }
            )
        }

        // PDF Configuration Dialog
        if (showPdfConfigDialog) {
            AlertDialog(
                onDismissRequest = { showPdfConfigDialog = false },
                title = {
                    Text(
                        text = "PDF Extraction Settings",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "File: $pendingUploadFileName\n\nSpecify the manual page range to extract (leave empty to extract the entire document).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = pdfStartPage,
                                onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) pdfStartPage = it },
                                label = { Text("Start Page") },
                                placeholder = { Text("e.g. 1") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = pdfEndPage,
                                onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) pdfEndPage = it },
                                label = { Text("End Page") },
                                placeholder = { Text("e.g. 5") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val start = pdfStartPage.toIntOrNull()
                            val end = pdfEndPage.toIntOrNull()
                            pendingUploadUri?.let { uri ->
                                viewModel.uploadAndGenerateQuiz(uri, pendingUploadFileName, start, end)
                            }
                            showPdfConfigDialog = false
                        }
                    ) {
                        Text("Extract & Generate")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPdfConfigDialog = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Manual Text Entry Dialog
        if (showManualTextDialog) {
            AlertDialog(
                onDismissRequest = { showManualTextDialog = false },
                title = {
                    Text(
                        text = "Paste Vocabulary Manually",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Type or paste your vocabulary words, definitions, notes, or list here. Gemini will analyze the text to generate multiple-choice questions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = manualTextTitle,
                            onValueChange = { manualTextTitle = it },
                            label = { Text("Source Title (e.g. Lesson 5)") },
                            placeholder = { Text("Manual Study Guide") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = manualTextContent,
                            onValueChange = { manualTextContent = it },
                            label = { Text("Vocabulary / Text Content") },
                            placeholder = { Text("Enter or paste vocabulary text...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            maxLines = 15
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (manualTextContent.isNotBlank()) {
                                viewModel.generateQuizFromManualText(manualTextContent, manualTextTitle)
                                showManualTextDialog = false
                                manualTextContent = ""
                                manualTextTitle = ""
                            }
                        },
                        enabled = manualTextContent.isNotBlank()
                    ) {
                        Text("Generate Quiz")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showManualTextDialog = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Paste URL/Telegram Link Dialog
        if (showUrlDialog) {
            AlertDialog(
                onDismissRequest = { showUrlDialog = false },
                title = {
                    Text(
                        text = "Paste Telegram / Web PDF Link",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Paste a public Telegram channel post link, or a direct link to any PDF or text file on the web. The app will download and extract the vocabulary to auto-generate a Multiple-Choice quiz using Gemini API.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = pastedUrl,
                            onValueChange = { pastedUrl = it },
                            label = { Text("Telegram Post or PDF/TXT Link") },
                            placeholder = { Text("https://t.me/channelname/123 or https://example.com/vocab.pdf") },
                            modifier = Modifier.fillMaxWidth().testTag("url_link_input"),
                            singleLine = true
                        )
                        
                        Text(
                            text = "Optional: If the link is a PDF, specify the page range to extract (leave blank for entire PDF):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = urlStartPage,
                                onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) urlStartPage = it },
                                label = { Text("Start Page") },
                                placeholder = { Text("e.g. 1") },
                                modifier = Modifier.weight(1f).testTag("url_start_page"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = urlEndPage,
                                onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) urlEndPage = it },
                                label = { Text("End Page") },
                                placeholder = { Text("e.g. 5") },
                                modifier = Modifier.weight(1f).testTag("url_end_page"),
                                singleLine = true
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (pastedUrl.isNotBlank()) {
                                val start = urlStartPage.toIntOrNull()
                                val end = urlEndPage.toIntOrNull()
                                viewModel.generateQuizFromUrl(pastedUrl, start, end)
                                showUrlDialog = false
                                pastedUrl = ""
                                urlStartPage = ""
                                urlEndPage = ""
                            }
                        },
                        enabled = pastedUrl.isNotBlank(),
                        modifier = Modifier.testTag("url_generate_button")
                    ) {
                        Text("Extract & Generate")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUrlDialog = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Delete Quiz Confirmation Dialog
        if (quizToDelete != null) {
            AlertDialog(
                onDismissRequest = { quizToDelete = null },
                title = {
                    Text(
                        text = "Delete Quiz Set?",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete this quiz set from '${quizToDelete?.sourcePdfName}'? All its questions and your answers will be permanently deleted.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            quizToDelete?.let { viewModel.deleteQuizSet(it) }
                            quizToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_delete_quiz_button")
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.onError)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { quizToDelete = null }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardView(
    vocabQuizSets: List<VocabQuizSet>,
    uploadStatus: StudyViewModel.UploadStatus,
    onSelectQuiz: (VocabQuizSet) -> Unit,
    onUploadClick: () -> Unit,
    onManualTextClick: () -> Unit,
    onUrlClick: () -> Unit,
    onDeleteQuizSet: (VocabQuizSet) -> Unit,
    onDismissError: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Vocabulary Quiz",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("vocab_top_app_bar")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
        ) {
            // Upload card / Active status
            item {
                when (uploadStatus) {
                    is StudyViewModel.UploadStatus.Idle,
                    is StudyViewModel.UploadStatus.Success -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUploadClick() }
                                    .testTag("pdf_upload_card")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(24.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.UploadFile,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Text(
                                        text = "Upload Vocabulary PDF or TXT",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Select a daily vocabulary sheet, wordlist, syllabus PDF, or plain text file to auto-generate a Multiple-Choice quiz using Gemini API.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            // Manual Text / Paste Button
                            OutlinedButton(
                                onClick = onManualTextClick,
                                modifier = Modifier.fillMaxWidth().testTag("manual_text_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Or Paste Vocabulary Text Manually")
                            }

                            // URL / Telegram PDF Link Button
                            OutlinedButton(
                                onClick = onUrlClick,
                                modifier = Modifier.fillMaxWidth().testTag("url_link_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Or Paste PDF / Telegram Channel Link")
                            }
                        }
                    }

                    is StudyViewModel.UploadStatus.Extracting -> {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Extracting text content...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Reading raw content from the uploaded document locally.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    is StudyViewModel.UploadStatus.Generating -> {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                                Text(
                                    text = "Gemini is building your quiz...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Identifying vocabulary words, definitions, context, and generating plausible options under strict guidelines.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    is StudyViewModel.UploadStatus.Error -> {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Quiz Generation Failed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = uploadStatus.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = onDismissError,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }
                }
            }

            // History Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quiz & Revision History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // History list
            if (vocabQuizSets.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(32.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Quiz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No quizzes yet",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "Upload a vocabulary PDF or text file above to create your first review quiz.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(vocabQuizSets, key = { it.id }) { quiz ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectQuiz(quiz) }
                            .testTag("historical_quiz_${quiz.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Quiz - ${quiz.date}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = quiz.sourcePdfName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { onDeleteQuizSet(quiz) },
                                    modifier = Modifier.testTag("delete_quiz_${quiz.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete quiz",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open quiz",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveQuizTakingView(
    quizSet: VocabQuizSet,
    questions: List<VocabQuizQuestion>,
    currentIndex: Int,
    showFeedback: Boolean,
    lastSelectedOption: Int?,
    onOptionSelect: (VocabQuizQuestion, Int) -> Unit,
    onNextQuestion: () -> Unit,
    onBackToDashboard: () -> Unit
) {
    val totalQuestions = questions.size
    val currentQuestion = questions.getOrNull(currentIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Vocab Quiz - ${quizSet.date}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToDashboard,
                        modifier = Modifier.testTag("vocab_quiz_back")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        if (currentQuestion == null) {
            // End of quiz summary
            val correctCount = questions.count { it.isAnsweredCorrectly }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Text(
                            text = "Quiz Completed!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "$correctCount / $totalQuestions Correct",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Excellent revision! This quiz attempt has been persisted to your local study archive for easy retrieval.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = onBackToDashboard,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("vocab_finish_close")
                        ) {
                            Text("Back to Dashboard", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Quiz taking questions
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Progress Indicator
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Question ${currentIndex + 1} of $totalQuestions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        val correctSofar = questions.take(currentIndex).count { it.isAnsweredCorrectly }
                        Text(
                            text = "Score: $correctSofar/${currentIndex}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (currentIndex + 1).toFloat() / totalQuestions },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // The Question Word Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "What is the meaning of:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentQuestion.word,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Options list
                val options = listOf(
                    currentQuestion.optionA,
                    currentQuestion.optionB,
                    currentQuestion.optionC,
                    currentQuestion.optionD
                )

                val isDark = MaterialTheme.colorScheme.background.red < 0.5f
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    options.forEachIndexed { idx, optionText ->
                        val isSelected = lastSelectedOption == idx || currentQuestion.userSelectedOptionIndex == idx
                        val isCorrect = idx == currentQuestion.correctOptionIndex
                        val isAnswered = currentQuestion.isAnswered

                        val containerColor = when {
                            isAnswered && isCorrect -> if (isDark) Color(0xFF1B3D22) else Color(0xFFE2F4E3)
                            isAnswered && isSelected && !isCorrect -> if (isDark) Color(0xFF3F1D1D) else Color(0xFFFBEBEB)
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        }

                        val strokeColor = when {
                            isAnswered && isCorrect -> if (isDark) Color(0xFF81C784) else Color(0xFF4CAF50)
                            isAnswered && isSelected && !isCorrect -> if (isDark) Color(0xFFE57373) else Color(0xFFE53935)
                            isSelected -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, strokeColor),
                            colors = CardDefaults.cardColors(containerColor = containerColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isAnswered) {
                                    onOptionSelect(currentQuestion, idx)
                                }
                                .testTag("option_$idx")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isAnswered && isCorrect -> if (isDark) Color(0xFF81C784) else Color(0xFF4CAF50)
                                                isAnswered && isSelected && !isCorrect -> if (isDark) Color(0xFFE57373) else Color(0xFFE53935)
                                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (idx) {
                                            0 -> "A"
                                            1 -> "B"
                                            2 -> "C"
                                            else -> "D"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isAnswered && (isCorrect || isSelected)) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = optionText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Feedback popup / Action area
                AnimatedVisibility(
                    visible = showFeedback,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    val wasCorrect = lastSelectedOption == currentQuestion.correctOptionIndex
                    Card(
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (wasCorrect) {
                                if (isDark) Color(0xFF1B3D22) else Color(0xFFE8F5E9)
                            } else {
                                if (isDark) Color(0xFF3F1D1D) else Color(0xFFFFEBEE)
                            }
                        ),
                        border = BorderStroke(1.dp, if (wasCorrect) {
                            if (isDark) Color(0xFF81C784) else Color(0xFF81C784)
                        } else {
                            if (isDark) Color(0xFFE57373) else Color(0xFFE57373)
                        }),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (wasCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (wasCorrect) {
                                        if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                                    } else {
                                        if (isDark) Color(0xFFE57373) else Color(0xFFC62828)
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (wasCorrect) "Absolutely Correct!" else "Incorrect",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (wasCorrect) {
                                        if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                                    } else {
                                        if (isDark) Color(0xFFE57373) else Color(0xFFC62828)
                                    }
                                )
                            }

                            Text(
                                text = currentQuestion.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = onNextQuestion,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("vocab_next_question_button")
                            ) {
                                Text(
                                    text = if (currentIndex == totalQuestions - 1) "View Summary" else "Next Question",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewQuizView(
    quizSet: VocabQuizSet,
    questions: List<VocabQuizQuestion>,
    onBackToDashboard: () -> Unit,
    onReattemptClick: () -> Unit
) {
    val correctCount = questions.count { it.isAnsweredCorrectly }
    val totalCount = questions.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Revision - ${quizSet.date}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            quizSet.sourcePdfName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToDashboard,
                        modifier = Modifier.testTag("review_mode_back")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
        ) {
            // Summary banner
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Quiz Score",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$correctCount out of $totalCount correct",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${((correctCount.toFloat() / totalCount) * 100).toInt()}%",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        Button(
                            onClick = onReattemptClick,
                            modifier = Modifier.fillMaxWidth().testTag("reattempt_quiz_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reattempt"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reattempt Quiz")
                        }
                    }
                }
            }

            // Questions revision list
            items(questions) { q ->
                val isDark = MaterialTheme.colorScheme.background.red < 0.5f
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = q.word,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (q.isAnsweredCorrectly) {
                                    if (isDark) Color(0xFF1B3D22) else Color(0xFFE8F5E9)
                                } else {
                                    if (isDark) Color(0xFF3F1D1D) else Color(0xFFFFEBEE)
                                },
                                modifier = Modifier.height(26.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (q.isAnsweredCorrectly) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (q.isAnsweredCorrectly) {
                                            if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                                        } else {
                                            if (isDark) Color(0xFFE57373) else Color(0xFFC62828)
                                        },
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (q.isAnsweredCorrectly) "Right" else "Wrong",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (q.isAnsweredCorrectly) {
                                            if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                                        } else {
                                            if (isDark) Color(0xFFE57373) else Color(0xFFC62828)
                                        }
                                    )
                                }
                            }
                        }

                        // Display selected vs correct option
                        val options = listOf(q.optionA, q.optionB, q.optionC, q.optionD)
                        val correctText = options.getOrNull(q.correctOptionIndex) ?: ""
                        val selectedText = options.getOrNull(q.userSelectedOptionIndex) ?: "Unanswered"

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (!q.isAnsweredCorrectly) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Your Answer: ",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFE57373) else Color(0xFFC62828)
                                    )
                                    Text(
                                        text = selectedText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Correct Answer: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                                )
                                Text(
                                    text = correctText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Explanation card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Explanation: ${q.explanation}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper to extract file name from local content URI
private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result.substring(cut + 1)
        }
    }
    return result
}
