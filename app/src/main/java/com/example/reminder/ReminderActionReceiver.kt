package com.example.reminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.MemoFlowApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_DONE = "com.aistudio.memoflow.ACTION_MARK_DONE"
        const val ACTION_SNOOZE_15 = "com.aistudio.memoflow.ACTION_SNOOZE_15"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra(ReminderScheduler.EXTRA_NOTE_ID, -1L)
        if (noteId == -1L) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(noteId.toInt())

        val app = context.applicationContext as? MemoFlowApp ?: return

        when (intent.action) {
            ACTION_MARK_DONE -> {
                CoroutineScope(Dispatchers.IO).launch {
                    app.repository.setReminderCompleted(noteId, true)
                }
            }
            ACTION_SNOOZE_15 -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val note = app.repository.getNoteByIdDirect(noteId)
                    if (note != null) {
                        val snoozedTime = System.currentTimeMillis() + (15 * 60 * 1000) // 15 mins
                        val updated = note.copy(
                            reminderTimeMillis = snoozedTime,
                            isReminderCompleted = false,
                            updatedAt = System.currentTimeMillis()
                        )
                        app.repository.updateNote(updated)
                        ReminderScheduler.scheduleReminder(context, updated)
                    }
                }
            }
        }
    }
}
