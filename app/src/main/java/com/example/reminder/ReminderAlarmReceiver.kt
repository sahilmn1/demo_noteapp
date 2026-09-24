package com.example.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.MemoFlowApp
import com.example.data.model.ReminderRepeat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra(ReminderScheduler.EXTRA_NOTE_ID, -1L)
        if (noteId == -1L) return

        val noteTitle = intent.getStringExtra(ReminderScheduler.EXTRA_NOTE_TITLE) ?: "Note Reminder"
        val noteContent = intent.getStringExtra(ReminderScheduler.EXTRA_NOTE_CONTENT) ?: ""
        val repeatStr = intent.getStringExtra(ReminderScheduler.EXTRA_NOTE_REPEAT) ?: ReminderRepeat.NONE.name

        // Open app intent
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_note_id", noteId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            noteId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Complete action intent
        val doneIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_MARK_DONE
            putExtra(ReminderScheduler.EXTRA_NOTE_ID, noteId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (noteId * 10 + 1).toInt(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze 15 min action intent
        val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_SNOOZE_15
            putExtra(ReminderScheduler.EXTRA_NOTE_ID, noteId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (noteId * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, MemoFlowApp.REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("⏰ $noteTitle")
            .setContentText(noteContent.ifBlank { "You set a reminder for this note" })
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(if (noteContent.isNotBlank()) noteContent else "You set a reminder for this note")
                    .setBigContentTitle("⏰ $noteTitle")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Mark Done", donePendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze 15m", snoozePendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(noteId.toInt(), notification)

        // If repeating, compute next occurrence and schedule
        val repeat = try {
            ReminderRepeat.valueOf(repeatStr)
        } catch (e: Exception) {
            ReminderRepeat.NONE
        }

        if (repeat != ReminderRepeat.NONE) {
            val app = context.applicationContext as? MemoFlowApp
            app?.let { flowApp ->
                CoroutineScope(Dispatchers.IO).launch {
                    val note = flowApp.repository.getNoteByIdDirect(noteId)
                    if (note != null && !note.isTrashed && !note.isReminderCompleted) {
                        val nextTime = calculateNextOccurrence(note.reminderTimeMillis ?: System.currentTimeMillis(), repeat)
                        val updatedNote = note.copy(reminderTimeMillis = nextTime, updatedAt = System.currentTimeMillis())
                        flowApp.repository.updateNote(updatedNote)
                        ReminderScheduler.scheduleReminder(context, updatedNote)
                    }
                }
            }
        }
    }

    private fun calculateNextOccurrence(currentMillis: Long, repeat: ReminderRepeat): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = currentMillis
        when (repeat) {
            ReminderRepeat.DAILY -> calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            ReminderRepeat.WEEKLY -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1)
            ReminderRepeat.MONTHLY -> calendar.add(java.util.Calendar.MONTH, 1)
            ReminderRepeat.NONE -> {}
        }
        return calendar.timeInMillis
    }
}
