package com.example.ui.screens

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChecklistHelper
import com.example.data.model.ChecklistItem
import com.example.data.model.NoteColorPalette
import com.example.data.model.NoteEntity
import com.example.data.model.NotePriority
import com.example.data.model.ReminderRepeat
import com.example.ui.components.ChecklistEditor
import com.example.ui.components.ColorPickerRow
import com.example.ui.components.PriorityPickerRow
import com.example.ui.components.ReminderBadge
import com.example.ui.components.ReminderBottomSheet
import com.example.ui.viewmodel.NotesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    noteId: Long?,
    isChecklistInitial: Boolean,
    openReminderInitial: Boolean,
    initialSpeechText: String?,
    viewModel: NotesViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    // State
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf(initialSpeechText ?: "") }
    var category by remember { mutableStateOf("Personal") }
    var colorHex by remember { mutableStateOf("#FFFFFF") }
    var isPinned by remember { mutableStateOf(false) }
    var isChecklist by remember { mutableStateOf(isChecklistInitial) }
    var checklistItems by remember { mutableStateOf<List<ChecklistItem>>(emptyList()) }
    var reminderTimeMillis by remember { mutableStateOf<Long?>(null) }
    var reminderRepeat by remember { mutableStateOf(ReminderRepeat.NONE.name) }
    var isReminderCompleted by remember { mutableStateOf(false) }
    var priority by remember { mutableStateOf(NotePriority.MEDIUM.name) }
    var createdAt by remember { mutableStateOf(System.currentTimeMillis()) }

    var showReminderSheet by remember { mutableStateOf(openReminderInitial) }
    var showColorPalette by remember { mutableStateOf(false) }
    var showPrioritySelector by remember { mutableStateOf(false) }
    var showCategoryEditor by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val reminderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Speech recognition launcher for note body
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spokenText.isNullOrBlank()) {
            if (isChecklist) {
                checklistItems = checklistItems + ChecklistItem(text = spokenText)
            } else {
                content = if (content.isBlank()) spokenText else "$content\n$spokenText"
            }
        }
    }

    // Load existing note if editing
    LaunchedEffect(noteId) {
        if (noteId != null && noteId > 0) {
            val noteFlow = viewModel.rawActiveNotes
            noteFlow.collect { list ->
                val existing = list.find { it.id == noteId }
                if (existing != null) {
                    title = existing.title
                    content = existing.content
                    category = existing.category
                    colorHex = existing.colorHex
                    isPinned = existing.isPinned
                    isChecklist = existing.isChecklist
                    checklistItems = ChecklistHelper.parseJson(existing.checklistJson)
                    reminderTimeMillis = existing.reminderTimeMillis
                    reminderRepeat = existing.reminderRepeat
                    isReminderCompleted = existing.isReminderCompleted
                    priority = existing.priority
                    createdAt = existing.createdAt
                }
            }
        }
    }

    val pageBgColor = NoteColorPalette.getColor(colorHex, isDark)

    fun saveCurrentNote() {
        val hasData = title.isNotBlank() || content.isNotBlank() || checklistItems.isNotEmpty() || reminderTimeMillis != null
        if (hasData) {
            val entity = NoteEntity(
                id = noteId ?: 0L,
                title = title.trim(),
                content = content.trim(),
                category = category.trim().ifBlank { "Personal" },
                colorHex = colorHex,
                isPinned = isPinned,
                isArchived = false,
                isTrashed = false,
                isChecklist = isChecklist,
                checklistJson = ChecklistHelper.toJson(checklistItems),
                reminderTimeMillis = reminderTimeMillis,
                reminderRepeat = reminderRepeat,
                isReminderCompleted = isReminderCompleted,
                priority = priority,
                createdAt = createdAt,
                updatedAt = System.currentTimeMillis()
            )
            viewModel.saveNote(entity)
        }
    }

    Scaffold(
        containerColor = pageBgColor,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = pageBgColor,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            saveCurrentNote()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("note_edit_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Save and go back"
                        )
                    }
                },
                title = {},
                actions = {
                    // Speech to text button
                    IconButton(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak...")
                            }
                            try {
                                speechLauncher.launch(intent)
                            } catch (e: Exception) {}
                        },
                        modifier = Modifier.testTag("edit_speech_button")
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice input")
                    }

                    // Checklist Toggle
                    IconButton(
                        onClick = {
                            if (!isChecklist && content.isNotBlank()) {
                                // Convert lines to checklists
                                val lines = content.lines().filter { it.isNotBlank() }
                                checklistItems = lines.map { ChecklistItem(text = it) }
                            } else if (isChecklist && checklistItems.isNotEmpty()) {
                                // Convert checklists to text
                                content = checklistItems.joinToString("\n") { it.text }
                            }
                            isChecklist = !isChecklist
                        },
                        modifier = Modifier.testTag("edit_checklist_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = "Toggle Checklist mode",
                            tint = if (isChecklist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Reminder Button
                    IconButton(
                        onClick = { showReminderSheet = true },
                        modifier = Modifier.testTag("edit_reminder_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Set reminder",
                            tint = if (reminderTimeMillis != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Pin Toggle
                    IconButton(
                        onClick = { isPinned = !isPinned },
                        modifier = Modifier.testTag("edit_pin_button")
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin Note",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Color Palette Toggle
                    IconButton(
                        onClick = { showColorPalette = !showColorPalette },
                        modifier = Modifier.testTag("edit_color_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Note Color"
                        )
                    }

                    // Overflow Menu (Share, Priority, Delete)
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("edit_more_menu_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Priority Level") },
                                onClick = {
                                    showMenu = false
                                    showPrioritySelector = !showPrioritySelector
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Share note") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    val shareText = buildString {
                                        if (title.isNotBlank()) appendLine(title)
                                        if (isChecklist) {
                                            checklistItems.forEach {
                                                appendLine("${if (it.isChecked) "[✓]" else "[ ]"} ${it.text}")
                                            }
                                        } else {
                                            appendLine(content)
                                        }
                                    }
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, title)
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share note via"))
                                }
                            )

                            if (noteId != null && noteId > 0) {
                                DropdownMenuItem(
                                    text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        val entity = NoteEntity(id = noteId)
                                        viewModel.moveToTrash(entity)
                                        onNavigateBack()
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Color Picker Expandable Row
            AnimatedVisibility(visible = showColorPalette) {
                ColorPickerRow(
                    selectedHex = colorHex,
                    onColorSelected = {
                        colorHex = it
                        showColorPalette = false
                    }
                )
            }

            // Priority Selector Expandable Row
            AnimatedVisibility(visible = showPrioritySelector) {
                PriorityPickerRow(
                    selectedPriority = priority,
                    onPrioritySelected = {
                        priority = it
                        showPrioritySelector = false
                    }
                )
            }

            // Active Reminder Banner Pill
            if (reminderTimeMillis != null) {
                Spacer(modifier = Modifier.height(6.dp))
                ReminderBadge(
                    timeMillis = reminderTimeMillis!!,
                    isCompleted = isReminderCompleted,
                    repeat = reminderRepeat,
                    onClick = { showReminderSheet = true }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Category tag chip and selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                if (showCategoryEditor) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        placeholder = { Text("Tag name") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tag_edit_input")
                    )
                    IconButton(
                        onClick = { showCategoryEditor = false },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Done", tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Surface(
                        onClick = { showCategoryEditor = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.testTag("edit_category_pill")
                    ) {
                        Text(
                            text = "🏷️ $category (Tap to edit)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Note Title Input
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text(
                        "Title",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                },
                textStyle = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_title_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Body Editor (Checklist OR Plain Text)
            if (isChecklist) {
                ChecklistEditor(
                    items = checklistItems,
                    onItemsChanged = { checklistItems = it },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = {
                        Text(
                            "Type your note here, tap mic for voice memo...",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .testTag("note_content_input")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer info: word count, character count, date
            val wordCount = remember(content, checklistItems, isChecklist) {
                if (isChecklist) {
                    checklistItems.sumOf { it.text.trim().split("\\s+".toRegex()).count { w -> w.isNotBlank() } }
                } else {
                    if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
                }
            }

            val charCount = remember(content, checklistItems, isChecklist) {
                if (isChecklist) checklistItems.sumOf { it.text.length } else content.length
            }

            val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edited ${dateFormat.format(Date(createdAt))}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Text(
                    text = "$wordCount words • $charCount characters",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Reminder Bottom Sheet
    if (showReminderSheet) {
        ReminderBottomSheet(
            sheetState = reminderSheetState,
            initialTimeMillis = reminderTimeMillis,
            initialRepeat = reminderRepeat,
            onDismiss = { showReminderSheet = false },
            onSaveReminder = { time, repeat ->
                reminderTimeMillis = time
                reminderRepeat = repeat
                isReminderCompleted = false
            }
        )
    }
}
