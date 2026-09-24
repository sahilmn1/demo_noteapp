package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ChecklistHelper
import com.example.data.model.ChecklistItem
import com.example.data.model.NoteEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MemoFlow", appName)
    }

    @Test
    fun testChecklistSerialization() {
        val items = listOf(
            ChecklistItem(id = "1", text = "Buy milk", isChecked = false),
            ChecklistItem(id = "2", text = "Finish project", isChecked = true)
        )
        val json = ChecklistHelper.toJson(items)
        val parsed = ChecklistHelper.parseJson(json)
        assertEquals(2, parsed.size)
        assertEquals("Buy milk", parsed[0].text)
        assertEquals(false, parsed[0].isChecked)
        assertEquals("Finish project", parsed[1].text)
        assertEquals(true, parsed[1].isChecked)
    }

    @Test
    fun testNoteEntityDefaultValues() {
        val note = NoteEntity(title = "Test Note", content = "Test Content")
        assertEquals("Test Note", note.title)
        assertEquals("Test Content", note.content)
        assertEquals("Personal", note.category)
        assertEquals(false, note.isPinned)
        assertEquals(false, note.isArchived)
        assertEquals(false, note.isTrashed)
    }
}
