package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MemoFlowApp
import com.example.data.model.ChecklistHelper
import com.example.data.model.NoteEntity
import com.example.data.model.ReminderRepeat
import com.example.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ReminderFilterTab(val label: String) {
    TODAY("Today"),
    UPCOMING("Upcoming"),
    OVERDUE("Overdue"),
    COMPLETED("Completed"),
    ALL("All")
}

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MemoFlowApp
    private val repository = app.repository

    val rawActiveNotes = repository.activeNotes
    val rawReminders = repository.allReminders
    val rawArchivedNotes = repository.archivedNotes
    val rawTrashedNotes = repository.trashedNotes
    val categories = repository.categories

    // Search and filters
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null)
    val selectedPriority = MutableStateFlow<String?>(null)
    val isGridView = MutableStateFlow(true)
    val reminderFilterTab = MutableStateFlow(ReminderFilterTab.TODAY)

    // Filtered Active Notes
    val filteredNotes: StateFlow<List<NoteEntity>> = combine(
        rawActiveNotes,
        searchQuery,
        selectedCategory,
        selectedPriority
    ) { notes, query, category, priority ->
        notes.filter { note ->
            val matchesQuery = query.isBlank() ||
                note.title.contains(query, ignoreCase = true) ||
                note.content.contains(query, ignoreCase = true) ||
                note.category.contains(query, ignoreCase = true) ||
                (note.isChecklist && note.checklistJson.contains(query, ignoreCase = true))

            val matchesCategory = category == null || category == "All" || note.category.equals(category, ignoreCase = true)
            val matchesPriority = priority == null || priority == "All" || note.priority.equals(priority, ignoreCase = true)

            matchesQuery && matchesCategory && matchesPriority
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Reminders
    val filteredReminders: StateFlow<List<NoteEntity>> = combine(
        rawReminders,
        reminderFilterTab,
        searchQuery
    ) { reminders, tab, query ->
        val now = System.currentTimeMillis()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfToday = startOfToday + (24 * 3600 * 1000)

        reminders.filter { note ->
            val matchesQuery = query.isBlank() ||
                note.title.contains(query, ignoreCase = true) ||
                note.content.contains(query, ignoreCase = true)

            val matchesTab = when (tab) {
                ReminderFilterTab.TODAY -> {
                    !note.isReminderCompleted &&
                        (note.reminderTimeMillis ?: 0L) in startOfToday..endOfToday
                }
                ReminderFilterTab.UPCOMING -> {
                    !note.isReminderCompleted &&
                        (note.reminderTimeMillis ?: 0L) > endOfToday
                }
                ReminderFilterTab.OVERDUE -> {
                    !note.isReminderCompleted &&
                        (note.reminderTimeMillis ?: 0L) < now
                }
                ReminderFilterTab.COMPLETED -> {
                    note.isReminderCompleted
                }
                ReminderFilterTab.ALL -> true
            }

            matchesQuery && matchesTab
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotesList: StateFlow<List<NoteEntity>> = combine(
        rawArchivedNotes,
        searchQuery
    ) { notes, query ->
        if (query.isBlank()) notes
        else notes.filter { it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedNotesList: StateFlow<List<NoteEntity>> = rawTrashedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSelectedCategory(cat: String?) {
        selectedCategory.value = cat
    }

    fun setSelectedPriority(pri: String?) {
        selectedPriority.value = pri
    }

    fun toggleViewMode() {
        isGridView.value = !isGridView.value
    }

    fun setReminderFilterTab(tab: ReminderFilterTab) {
        reminderFilterTab.value = tab
    }

    fun saveNote(note: NoteEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val updatedTime = System.currentTimeMillis()
            val noteToSave = note.copy(updatedAt = updatedTime)
            val noteId: Long = if (note.id == 0L) {
                repository.insertNote(noteToSave)
            } else {
                repository.updateNote(noteToSave)
                note.id
            }

            val finalNote = noteToSave.copy(id = noteId)
            // Schedule or cancel reminder
            if (finalNote.reminderTimeMillis != null && !finalNote.isReminderCompleted && !finalNote.isTrashed) {
                ReminderScheduler.scheduleReminder(getApplication(), finalNote)
            } else {
                ReminderScheduler.cancelReminder(getApplication(), finalNote.id)
            }

            onSaved(noteId)
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.setPinned(note.id, !note.isPinned)
        }
    }

    fun toggleArchive(note: NoteEntity) {
        viewModelScope.launch {
            val newArchived = !note.isArchived
            repository.setArchived(note.id, newArchived)
            if (newArchived) {
                ReminderScheduler.cancelReminder(getApplication(), note.id)
            }
        }
    }

    fun moveToTrash(note: NoteEntity) {
        viewModelScope.launch {
            repository.setTrashed(note.id, true)
            ReminderScheduler.cancelReminder(getApplication(), note.id)
        }
    }

    fun restoreFromTrash(note: NoteEntity) {
        viewModelScope.launch {
            repository.setTrashed(note.id, false)
            if (note.reminderTimeMillis != null && !note.isReminderCompleted) {
                ReminderScheduler.scheduleReminder(getApplication(), note)
            }
        }
    }

    fun deletePermanently(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNotePermanently(note)
            ReminderScheduler.cancelReminder(getApplication(), note.id)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }

    fun toggleReminderCompleted(note: NoteEntity) {
        viewModelScope.launch {
            val newCompleted = !note.isReminderCompleted
            repository.setReminderCompleted(note.id, newCompleted)
            if (newCompleted) {
                ReminderScheduler.cancelReminder(getApplication(), note.id)
            } else {
                ReminderScheduler.scheduleReminder(getApplication(), note.copy(isReminderCompleted = false))
            }
        }
    }

    fun snoozeReminder(note: NoteEntity, minutes: Int) {
        viewModelScope.launch {
            val newTime = System.currentTimeMillis() + (minutes * 60 * 1000)
            val updated = note.copy(
                reminderTimeMillis = newTime,
                isReminderCompleted = false,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateNote(updated)
            ReminderScheduler.scheduleReminder(getApplication(), updated)
        }
    }

    fun quickToggleChecklistItem(note: NoteEntity, itemId: String) {
        viewModelScope.launch {
            val items = ChecklistHelper.parseJson(note.checklistJson).toMutableList()
            val index = items.indexOfFirst { it.id == itemId }
            if (index != -1) {
                val current = items[index]
                items[index] = current.copy(isChecked = !current.isChecked)
                val updatedJson = ChecklistHelper.toJson(items)
                val updatedNote = note.copy(checklistJson = updatedJson, updatedAt = System.currentTimeMillis())
                repository.updateNote(updatedNote)
            }
        }
    }

    fun duplicateNote(note: NoteEntity) {
        viewModelScope.launch {
            val duplicated = note.copy(
                id = 0,
                title = if (note.title.isNotBlank()) "${note.title} (Copy)" else "Copy of Note",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertNote(duplicated)
        }
    }
}
