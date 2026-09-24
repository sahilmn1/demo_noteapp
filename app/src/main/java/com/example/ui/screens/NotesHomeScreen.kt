package com.example.ui.screens

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NoteEntity
import com.example.ui.components.CategoryChips
import com.example.ui.components.EmptyStateView
import com.example.ui.components.NoteCard
import com.example.ui.viewmodel.NotesViewModel
import java.util.Locale

@Composable
fun NotesHomeScreen(
    viewModel: NotesViewModel,
    onNavigateToEdit: (Long?, Boolean, Boolean, String?) -> Unit, // id, isChecklist, isReminder, initialSpeech
    onNavigateToReminders: () -> Unit,
    onNavigateToArchiveTrash: () -> Unit
) {
    val context = LocalContext.current
    val notes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val allReminders by viewModel.rawReminders.collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by viewModel.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()

    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }
    var isFabExpanded by remember { mutableStateOf(false) }

    // Speech recognition launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spokenText.isNullOrBlank()) {
            onNavigateToEdit(null, false, false, spokenText)
        }
    }

    val pinnedNotes = remember(notes) { notes.filter { it.isPinned } }
    val otherNotes = remember(notes) { notes.filter { !it.isPinned } }

    val activeRemindersCount = remember(allReminders) {
        allReminders.count { !it.isReminderCompleted && (it.reminderTimeMillis ?: 0L) >= System.currentTimeMillis() - (1000 * 3600 * 4) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("notes_home_screen"),
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Voice Note Action
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 2.dp
                            ) {
                                Text(
                                    text = "Voice Memo",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            SmallFloatingActionButton(
                                onClick = {
                                    isFabExpanded = false
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your note...")
                                    }
                                    try {
                                        speechLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        onNavigateToEdit(null, false, false, null)
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.testTag("voice_fab_button")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice note")
                            }
                        }

                        // Quick Reminder Action
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 2.dp
                            ) {
                                Text(
                                    text = "Reminder Note",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            SmallFloatingActionButton(
                                onClick = {
                                    isFabExpanded = false
                                    onNavigateToEdit(null, false, true, null)
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.testTag("reminder_fab_button")
                            ) {
                                Icon(Icons.Default.Alarm, contentDescription = "New Reminder")
                            }
                        }

                        // Checklist Action
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 2.dp
                            ) {
                                Text(
                                    text = "Checklist",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            SmallFloatingActionButton(
                                onClick = {
                                    isFabExpanded = false
                                    onNavigateToEdit(null, true, false, null)
                                },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.testTag("checklist_fab_button")
                            ) {
                                Icon(Icons.Default.Checklist, contentDescription = "New Checklist")
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = {
                        if (isFabExpanded) {
                            isFabExpanded = false
                        } else {
                            onNavigateToEdit(null, false, false, null)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("main_add_fab")
                ) {
                    Icon(
                        imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (isFabExpanded) "Close" else "Create Note"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top App Bar with Search Box and Navigation Icons
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Search bar container
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    placeholder = { Text("Search notes, checklists, tags...", fontSize = 14.sp) },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("search_notes_input")
                                )
                                if (searchQuery.isNotBlank()) {
                                    IconButton(
                                        onClick = { viewModel.setSearchQuery("") },
                                        modifier = Modifier.size(28.dp).testTag("clear_search_button")
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Grid / List View Toggle
                        IconButton(
                            onClick = { viewModel.toggleViewMode() },
                            modifier = Modifier.testTag("toggle_view_mode_button")
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = if (isGridView) "List View" else "Grid View",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Reminders Quick Access
                        IconButton(
                            onClick = onNavigateToReminders,
                            modifier = Modifier.testTag("nav_reminders_button")
                        ) {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = "Reminders",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                if (activeRemindersCount > 0) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier
                                            .size(8.dp)
                                            .align(Alignment.TopEnd)
                                    ) {}
                                }
                            }
                        }

                        // Archive & Trash Access
                        IconButton(
                            onClick = onNavigateToArchiveTrash,
                            modifier = Modifier.testTag("nav_archive_trash_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = "Archive & Trash",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Filter Chips
                    CategoryChips(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { viewModel.setSelectedCategory(it) },
                        onAddNewCategory = { showAddTagDialog = true }
                    )
                }
            }

            // Reminders Alert Banner if active reminders exist
            if (activeRemindersCount > 0 && searchQuery.isBlank() && selectedCategory == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { onNavigateToReminders() }
                        .testTag("reminders_summary_banner"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "$activeRemindersCount Active Reminder${if (activeRemindersCount > 1) "s" else ""}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Tap to view schedule and calendar",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Main Content Area
            if (notes.isEmpty()) {
                EmptyStateView(
                    title = if (searchQuery.isNotBlank() || selectedCategory != null) "No matching notes found" else "No notes here yet",
                    subtitle = if (searchQuery.isNotBlank() || selectedCategory != null) "Try clearing filters or search query" else "Capture your thoughts, to-do checklists, and set timely reminders!",
                    actionLabel = if (searchQuery.isBlank() && selectedCategory == null) "Create Note" else null,
                    onActionClick = { onNavigateToEdit(null, false, false, null) },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(if (isGridView) 2 else 1),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 10.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("notes_staggered_grid")
                ) {
                    // Pinned Notes Header & Items
                    if (pinnedNotes.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Text(
                                text = "📌 PINNED",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                            )
                        }

                        items(pinnedNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                onClick = { onNavigateToEdit(note.id, note.isChecklist, false, null) },
                                onTogglePin = { viewModel.togglePin(note) },
                                onToggleArchive = { viewModel.toggleArchive(note) },
                                onDelete = { viewModel.moveToTrash(note) },
                                onDuplicate = { viewModel.duplicateNote(note) },
                                onToggleChecklistItem = { itemId -> viewModel.quickToggleChecklistItem(note, itemId) },
                                onToggleReminderCompleted = { viewModel.toggleReminderCompleted(note) }
                            )
                        }
                    }

                    // Other Notes Header & Items
                    if (otherNotes.isNotEmpty()) {
                        if (pinnedNotes.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Text(
                                    text = "OTHERS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
                                )
                            }
                        }

                        items(otherNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                onClick = { onNavigateToEdit(note.id, note.isChecklist, false, null) },
                                onTogglePin = { viewModel.togglePin(note) },
                                onToggleArchive = { viewModel.toggleArchive(note) },
                                onDelete = { viewModel.moveToTrash(note) },
                                onDuplicate = { viewModel.duplicateNote(note) },
                                onToggleChecklistItem = { itemId -> viewModel.quickToggleChecklistItem(note, itemId) },
                                onToggleReminderCompleted = { viewModel.toggleReminderCompleted(note) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Tag Dialog
    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = { Text("Add Tag Filter") },
            text = {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    placeholder = { Text("e.g. Work, Shopping, Goals") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_tag_input_field")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTagInput.isNotBlank()) {
                            viewModel.setSelectedCategory(newTagInput.trim())
                            newTagInput = ""
                            showAddTagDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_tag_button")
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
