package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReminderRepeat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReminderBadge(
    timeMillis: Long,
    isCompleted: Boolean = false,
    repeat: String = ReminderRepeat.NONE.name,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val isOverdue = !isCompleted && timeMillis < now

    val (bg, textColor) = when {
        isCompleted -> Color(0xFFE2E8F0) to Color(0xFF64748B)
        isOverdue -> Color(0xFFFFE4E6) to Color(0xFFBE123C)
        else -> Color(0xFFE0E7FF) to Color(0xFF4338CA)
    }

    val formattedTime = formatReminderDate(timeMillis)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("reminder_badge"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Alarm,
            contentDescription = "Reminder",
            tint = textColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = formattedTime,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
        )
        if (repeat != ReminderRepeat.NONE.name) {
            Icon(
                imageVector = Icons.Default.Repeat,
                contentDescription = "Repeating reminder",
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

fun formatReminderDate(timeMillis: Long): String {
    val date = Date(timeMillis)
    val calNote = Calendar.getInstance().apply { timeInMillis = timeMillis }
    val calNow = Calendar.getInstance()

    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(date)

    return when {
        isSameDay(calNow, calNote) -> "Today, $formattedTime"
        isTomorrow(calNow, calNote) -> "Tomorrow, $formattedTime"
        isYesterday(calNow, calNote) -> "Yesterday, $formattedTime"
        calNow.get(Calendar.YEAR) == calNote.get(Calendar.YEAR) -> {
            val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            dateFormat.format(date)
        }
        else -> {
            val fullFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            fullFormat.format(date)
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isTomorrow(calNow: Calendar, calTarget: Calendar): Boolean {
    val tomorrow = (calNow.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
    return isSameDay(tomorrow, calTarget)
}

private fun isYesterday(calNow: Calendar, calTarget: Calendar): Boolean {
    val yesterday = (calNow.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    return isSameDay(yesterday, calTarget)
}
