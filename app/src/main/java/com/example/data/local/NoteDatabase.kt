package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ChecklistHelper
import com.example.data.model.ChecklistItem
import com.example.data.model.NoteEntity
import com.example.data.model.NotePriority
import com.example.data.model.ReminderRepeat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [NoteEntity::class], version = 1, exportSchema = false)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "memoflow_notes_db"
                )
                    .addCallback(NoteDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class NoteDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialNotes(database.noteDao())
                    }
                }
            }

            private suspend fun populateInitialNotes(dao: NoteDao) {
                val now = System.currentTimeMillis()
                val todayEvening = now + (3 * 3600 * 1000) // 3 hours from now
                val tomorrowMorning = now + (18 * 3600 * 1000) // tomorrow

                val welcomeChecklist = listOf(
                    ChecklistItem(text = "Try tapping any checklist item to mark it done", isChecked = true),
                    ChecklistItem(text = "Set a smart reminder with notification", isChecked = false),
                    ChecklistItem(text = "Assign color themes and priority levels", isChecked = false),
                    ChecklistItem(text = "Search and filter notes by custom tags", isChecked = false)
                )

                val groceryChecklist = listOf(
                    ChecklistItem(text = "Almond milk & Greek yogurt", isChecked = true),
                    ChecklistItem(text = "Avocados and fresh spinach", isChecked = false),
                    ChecklistItem(text = "Whole grain sourdough bread", isChecked = false),
                    ChecklistItem(text = "Organic dark roast coffee beans", isChecked = false)
                )

                val note1 = NoteEntity(
                    title = "✨ Welcome to MemoFlow Notes",
                    content = "MemoFlow is your all-in-one companion for structured thoughts, to-do lists, and timely reminders.\n\n• Tap the '+' button below to quickly write a note, checklist, or set a reminder\n• Pin important thoughts to the top\n• Color-code by category (Work, Personal, Ideas, Study)\n• Get notified on time so you never miss a beat!",
                    category = "Ideas",
                    colorHex = "#FEF3C7", // Amber
                    isPinned = true,
                    isArchived = false,
                    isTrashed = false,
                    isChecklist = false,
                    priority = NotePriority.HIGH.name,
                    createdAt = now - 100000,
                    updatedAt = now - 100000
                )

                val note2 = NoteEntity(
                    title = "🚀 Feature Checklist",
                    content = "",
                    category = "Tasks",
                    colorHex = "#DCFCE7", // Sage Green
                    isPinned = true,
                    isArchived = false,
                    isTrashed = false,
                    isChecklist = true,
                    checklistJson = ChecklistHelper.toJson(welcomeChecklist),
                    reminderTimeMillis = todayEvening,
                    reminderRepeat = ReminderRepeat.NONE.name,
                    priority = NotePriority.URGENT.name,
                    createdAt = now - 80000,
                    updatedAt = now - 80000
                )

                val note3 = NoteEntity(
                    title = "🛒 Weekend Market Grocery",
                    content = "",
                    category = "Personal",
                    colorHex = "#DBEAFE", // Sky Blue
                    isPinned = false,
                    isArchived = false,
                    isTrashed = false,
                    isChecklist = true,
                    checklistJson = ChecklistHelper.toJson(groceryChecklist),
                    reminderTimeMillis = tomorrowMorning,
                    reminderRepeat = ReminderRepeat.WEEKLY.name,
                    priority = NotePriority.MEDIUM.name,
                    createdAt = now - 60000,
                    updatedAt = now - 60000
                )

                val note4 = NoteEntity(
                    title = "💡 Project Brainstorming & Architecture",
                    content = "Key takeaways for the upcoming sprint:\n\n1. Clean separation between UI and Data layers using Repository Pattern\n2. Reactive Room Flow updates for zero lag\n3. Instant search across all notes, tags, and checklist contents\n4. Foreground & background notification management with Snooze & Done actions.",
                    category = "Work",
                    colorHex = "#F3E8FF", // Lavender
                    isPinned = false,
                    isArchived = false,
                    isTrashed = false,
                    isChecklist = false,
                    priority = NotePriority.MEDIUM.name,
                    createdAt = now - 40000,
                    updatedAt = now - 40000
                )

                dao.insertNote(note1)
                dao.insertNote(note2)
                dao.insertNote(note3)
                dao.insertNote(note4)
            }
        }
    }
}
