package com.example.data.repository

import com.example.data.local.NoteDao
import com.example.data.model.NoteEntity
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    val activeNotes: Flow<List<NoteEntity>> = noteDao.getActiveNotes()
    val allReminders: Flow<List<NoteEntity>> = noteDao.getAllReminders()
    val archivedNotes: Flow<List<NoteEntity>> = noteDao.getArchivedNotes()
    val trashedNotes: Flow<List<NoteEntity>> = noteDao.getTrashedNotes()
    val categories: Flow<List<String>> = noteDao.getAllCategories()

    fun getNoteById(id: Long): Flow<NoteEntity?> = noteDao.getNoteById(id)

    suspend fun getNoteByIdDirect(id: Long): NoteEntity? = noteDao.getNoteByIdDirect(id)

    suspend fun getPendingFutureReminders(currentTime: Long): List<NoteEntity> =
        noteDao.getPendingFutureReminders(currentTime)

    suspend fun insertNote(note: NoteEntity): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: NoteEntity) = noteDao.updateNote(note)

    suspend fun deleteNotePermanently(note: NoteEntity) = noteDao.deleteNote(note)

    suspend fun emptyTrash() = noteDao.emptyTrash()

    suspend fun setPinned(id: Long, isPinned: Boolean) = noteDao.updatePinned(id, isPinned)

    suspend fun setArchived(id: Long, isArchived: Boolean) = noteDao.updateArchived(id, isArchived)

    suspend fun setTrashed(id: Long, isTrashed: Boolean) = noteDao.updateTrashed(id, isTrashed)

    suspend fun setReminderCompleted(id: Long, completed: Boolean) =
        noteDao.updateReminderCompleted(id, completed)

    suspend fun updateReminderTime(id: Long, timeMillis: Long?) =
        noteDao.updateReminderTime(id, timeMillis)
}
