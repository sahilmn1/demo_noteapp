package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NoteColorPalette
import com.example.data.model.NoteEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ReminderBadge
import com.example.ui.components.ReminderBottomSheet
import com.example.ui.components.formatReminderDate
import com.example.ui.viewmodel.NotesViewModel
import com.example.ui.viewmodel.ReminderFilterTab
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: NotesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onAddNewReminder: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val allReminders by viewModel.rawReminders.collectAsStateWithLifecycle(initialValue = emptyList())
    val filteredReminders by viewModel.filteredReminders.collectAsStateWithLifecycle()
    val currentTab by viewModel.reminderFilterTab.collectAsStateWithLifecycle()

    var noteToReschedule by remember { mutableStateOf<NoteEntity?>(null) }
    val rescheduleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val now = System.currentTimeMillis()
    val overdueCount = remember(allReminders, now) {
        allReminders.count { !it.isReminderCompleted && (it.reminderTimeMillis ?: 0L) < now }
    }
    val todayCount = remember(allReminders, now) {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfToday = startOfToday + (24 * 3600 * 1000)
        allReminders.count { !it.isReminderCompleted && (it.reminderTimeMillis ?: 0L) in startOfToday..endOfToday }
    }
    val completedCount = remember(allReminders) {
        allReminders.count { it.isReminderCompleted }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("reminders_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        text = "Reminders & Schedule",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(
                        onClick = onAddNewReminder,
                        modifier = Modifier.testTag("add_reminder_top_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add reminder note", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize().testTag("reminders_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Metrics Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricItem(
                        label = "Today",
                        count = todayCount,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { viewModel.setReminderFilterTab(ReminderFilterTab.TODAY) }
                    )
                    MetricItem(
                        label = "Overdue",
                        count = overdueCount,
                        color = MaterialTheme.colorScheme.error,
                        onClick = { viewModel.setReminderFilterTab(ReminderFilterTab.OVERDUE) }
                    )
                    MetricItem(
                        label = "Done",
                        count = completedCount,
                        color = Color(0xFF10B981),
                        onClick = { viewModel.setReminderFilterTab(ReminderFilterTab.COMPLETED) }
                    )
                    MetricItem(
                        label = "All",
                        count = allReminders.size,
                        color = MaterialTheme.colorScheme.onSurface,
                        onClick = { viewModel.setReminderFilterTab(ReminderFilterTab.ALL) }
                    )
                }
            }

            // Tabs Row
            ScrollableTabRow(
                selectedTabIndex = ReminderFilterTab.values().indexOf(currentTab),
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth().testTag("reminders_tab_row")
            ) {
                ReminderFilterTab.values().forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { viewModel.setReminderFilterTab(tab) },
                        text = {
                            Text(
                                text = tab.label,
                                fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.testTag("reminder_tab_${tab.name.lowercase()}")
                    )
                }
            }

            // Reminders List
            if (filteredReminders.isEmpty()) {
                EmptyStateView(
                    title = "No ${currentTab.label.lowercase()} reminders",
                    subtitle = when (currentTab) {
                        ReminderFilterTab.TODAY -> "You're all caught up for today! Plan ahead or set a new reminder."
                        ReminderFilterTab.OVERDUE -> "Great job! You have no overdue reminders."
                        ReminderFilterTab.COMPLETED -> "Completed reminders will appear here once marked as done."
                        else -> "Set reminders with specific times to stay on top of your priorities."
                    },
                    actionLabel = "Add Reminder Note",
                    onActionClick = onAddNewReminder,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize().testTag("reminders_lazy_list")
                ) {
                    items(filteredReminders, key = { it.id }) { note ->
                        ReminderItemCard(
                            note = note,
                            isDark = isDark,
                            onOpen = { onNavigateToEdit(note.id) },
                            onToggleComplete = { viewModel.toggleReminderCompleted(note) },
                            onSnooze15 = { viewModel.snoozeReminder(note, 15) },
                            onSnooze60 = { viewModel.snoozeReminder(note, 60) },
                            onReschedule = { noteToReschedule = note }
                        )
                    }
                }
            }
        }
    }

    // Reschedule Bottom Sheet
    noteToReschedule?.let { targetNote ->
        ReminderBottomSheet(
            sheetState = rescheduleSheetState,
            initialTimeMillis = targetNote.reminderTimeMillis,
            initialRepeat = targetNote.reminderRepeat,
            onDismiss = { noteToReschedule = null },
            onSaveReminder = { time, repeat ->
                viewModel.saveNote(
                    targetNote.copy(
                        reminderTimeMillis = time,
                        reminderRepeat = repeat,
                        isReminderCompleted = false
                    )
                )
                noteToReschedule = null
            }
        )
    }
}

@Composable
private fun MetricItem(
    label: String,
    count: Int,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("metric_${label.lowercase()}")
    ) {
        Text(
            text = count.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReminderItemCard(
    note: NoteEntity,
    isDark: Boolean,
    onOpen: () -> Unit,
    onToggleComplete: () -> Unit,
    onSnooze15: () -> Unit,
    onSnooze60: () -> Unit,
    onReschedule: () -> Unit
) {
    val cardBg = NoteColorPalette.getColor(note.colorHex, isDark)
    val now = System.currentTimeMillis()
    val isOverdue = !note.isReminderCompleted && (note.reminderTimeMillis ?: 0L) < now

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("reminder_item_${note.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Checkbox, Title & Category Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = note.isReminderCompleted,
                    onCheckedChange = { onToggleComplete() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("reminder_complete_checkbox_${note.id}")
                )

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.title.ifBlank { "Untitled Note" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (note.isReminderCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (note.content.isNotBlank()) {
                        Text(
                            text = note.content,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (note.category.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Text(
                            text = note.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reminder Time and Relative Countdown Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (note.reminderTimeMillis != null) {
                    ReminderBadge(
                        timeMillis = note.reminderTimeMillis,
                        isCompleted = note.isReminderCompleted,
                        repeat = note.reminderRepeat,
                        onClick = onReschedule
                    )
                }

                if (isOverdue) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Overdue", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Actions: Snooze +15m, +1h, Reschedule, Open Note
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!note.isReminderCompleted) {
                    FilledTonalButton(
                        onClick = onSnooze15,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).testTag("snooze_15_button_${note.id}")
                    ) {
                        Icon(Icons.Default.MoreTime, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+15m", fontSize = 11.sp)
                    }

                    FilledTonalButton(
                        onClick = onSnooze60,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).testTag("snooze_60_button_${note.id}")
                    ) {
                        Icon(Icons.Default.MoreTime, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+1h", fontSize = 11.sp)
                    }
                }

                OutlinedButton(
                    onClick = onReschedule,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("reschedule_button_${note.id}")
                ) {
                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Change", fontSize = 11.sp)
                }
            }
        }
    }
}
