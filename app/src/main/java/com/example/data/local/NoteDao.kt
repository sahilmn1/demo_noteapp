package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isTrashed = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE reminderTimeMillis IS NOT NULL AND isTrashed = 0 ORDER BY isReminderCompleted ASC, reminderTimeMillis ASC")
    fun getAllReminders(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 AND isTrashed = 0 ORDER BY updatedAt DESC")
    fun getArchivedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isTrashed = 1 ORDER BY updatedAt DESC")
    fun getTrashedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getNoteById(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteByIdDirect(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE reminderTimeMillis IS NOT NULL AND isReminderCompleted = 0 AND isTrashed = 0 AND reminderTimeMillis > :currentTime")
    suspend fun getPendingFutureReminders(currentTime: Long): List<NoteEntity>

    @Query("SELECT DISTINCT category FROM notes WHERE category IS NOT NULL AND category != '' AND isTrashed = 0")
    fun getAllCategories(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE isTrashed = 1")
    suspend fun emptyTrash()

    @Query("UPDATE notes SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinned(id: Long, isPinned: Boolean)

    @Query("UPDATE notes SET isArchived = :isArchived, isTrashed = 0 WHERE id = :id")
    suspend fun updateArchived(id: Long, isArchived: Boolean)

    @Query("UPDATE notes SET isTrashed = :isTrashed WHERE id = :id")
    suspend fun updateTrashed(id: Long, isTrashed: Boolean)

    @Query("UPDATE notes SET isReminderCompleted = :completed WHERE id = :id")
    suspend fun updateReminderCompleted(id: Long, completed: Boolean)

    @Query("UPDATE notes SET reminderTimeMillis = :timeMillis, isReminderCompleted = 0 WHERE id = :id")
    suspend fun updateReminderTime(id: Long, timeMillis: Long?)
}
