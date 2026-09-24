package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.MemoFlowApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val app = context.applicationContext as? MemoFlowApp ?: return
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val pendingReminders = app.repository.getPendingFutureReminders(System.currentTimeMillis())
                    Log.d("BootReceiver", "Rescheduling ${pendingReminders.size} reminders after boot.")
                    for (note in pendingReminders) {
                        ReminderScheduler.scheduleReminder(context, note)
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to reschedule on boot: ${e.message}")
                }
            }
        }
    }
}
