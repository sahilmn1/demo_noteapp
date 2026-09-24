package com.example.data.model

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val category: String = "Personal",
    val colorHex: String = "#FFFFFF",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val isChecklist: Boolean = false,
    val checklistJson: String = "[]",
    val reminderTimeMillis: Long? = null,
    val reminderRepeat: String = ReminderRepeat.NONE.name,
    val isReminderCompleted: Boolean = false,
    val priority: String = NotePriority.MEDIUM.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ChecklistItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String = "",
    val isChecked: Boolean = false
)

enum class NotePriority(val label: String, val color: Long) {
    LOW("Low", 0xFF4CAF50),
    MEDIUM("Medium", 0xFFFF9800),
    HIGH("High", 0xFFFF5722),
    URGENT("Urgent", 0xFFE91E63)
}

enum class ReminderRepeat(val label: String) {
    NONE("Does not repeat"),
    DAILY("Every day"),
    WEEKLY("Every week"),
    MONTHLY("Every month")
}

data class NoteColorOption(
    val hex: String,
    val name: String,
    val lightBg: Color,
    val darkBg: Color
)

object NoteColorPalette {
    val options = listOf(
        NoteColorOption("#FFFFFF", "Default", Color(0xFFFFFFFF), Color(0xFF25252A)),
        NoteColorOption("#FEF3C7", "Amber", Color(0xFFFEF3C7), Color(0xFF3E3316)),
        NoteColorOption("#DCFCE7", "Sage", Color(0xFFDCFCE7), Color(0xFF193826)),
        NoteColorOption("#DBEAFE", "Sky", Color(0xFFDBEAFE), Color(0xFF1E2E48)),
        NoteColorOption("#FCE7F3", "Rose", Color(0xFFFCE7F3), Color(0xFF451E33)),
        NoteColorOption("#F3E8FF", "Lavender", Color(0xFFF3E8FF), Color(0xFF37204D)),
        NoteColorOption("#FFEDD5", "Peach", Color(0xFFFFEDD5), Color(0xFF472C15)),
        NoteColorOption("#E0E7FF", "Indigo", Color(0xFFE0E7FF), Color(0xFF23254B))
    )

    fun getColor(hex: String, isDark: Boolean): Color {
        val found = options.find { it.hex.equals(hex, ignoreCase = true) }
        return if (found != null) {
            if (isDark) found.darkBg else found.lightBg
        } else {
            if (isDark) Color(0xFF25252A) else Color(0xFFFFFFFF)
        }
    }
}

object ChecklistHelper {
    fun parseJson(json: String): List<ChecklistItem> {
        if (json.isBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<ChecklistItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ChecklistItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        text = obj.optString("text", ""),
                        isChecked = obj.optBoolean("isChecked", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun toJson(items: List<ChecklistItem>): String {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("text", item.text)
            obj.put("isChecked", item.isChecked)
            array.put(obj)
        }
        return array.toString()
    }
}
