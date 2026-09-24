package com.example.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.NoteEntity

object ReminderScheduler {

    const val ACTION_REMINDER_ALARM = "com.aistudio.memoflow.ACTION_REMINDER_ALARM"
    const val EXTRA_NOTE_ID = "extra_note_id"
    const val EXTRA_NOTE_TITLE = "extra_note_title"
    const val EXTRA_NOTE_CONTENT = "extra_note_content"
    const val EXTRA_NOTE_REPEAT = "extra_note_repeat"

    fun scheduleReminder(context: Context, note: NoteEntity) {
        val reminderTime = note.reminderTimeMillis ?: return
        if (note.isReminderCompleted || note.isTrashed) {
            cancelReminder(context, note.id)
            return
        }

        // If time is in the past, don't schedule an alarm unless repeating
        val now = System.currentTimeMillis()
        if (reminderTime <= now) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_REMINDER_ALARM
            putExtra(EXTRA_NOTE_ID, note.id)
            putExtra(EXTRA_NOTE_TITLE, note.title.ifBlank { "Note Reminder" })
            putExtra(EXTRA_NOTE_CONTENT, note.content)
            putExtra(EXTRA_NOTE_REPEAT, note.reminderRepeat)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            note.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            }
            Log.d("ReminderScheduler", "Scheduled reminder for note #${note.id} at $reminderTime")
        } catch (e: SecurityException) {
            Log.e("ReminderScheduler", "Failed to schedule exact alarm: ${e.message}")
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            } catch (ex: Exception) {
                Log.e("ReminderScheduler", "Fallback alarm failed: ${ex.message}")
            }
        }
    }

    fun cancelReminder(context: Context, noteId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_REMINDER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            noteId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("ReminderScheduler", "Cancelled reminder for note #$noteId")
        }
    }
}
