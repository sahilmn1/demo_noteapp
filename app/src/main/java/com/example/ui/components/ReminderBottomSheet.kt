package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReminderRepeat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderBottomSheet(
    sheetState: SheetState,
    initialTimeMillis: Long?,
    initialRepeat: String,
    onDismiss: () -> Unit,
    onSaveReminder: (Long?, String) -> Unit
) {
    val context = LocalContext.current
    var selectedTimeMillis by remember {
        mutableStateOf(initialTimeMillis ?: (System.currentTimeMillis() + 3600 * 1000))
    }
    var selectedRepeat by remember { mutableStateOf(initialRepeat) }
    var hasReminder by remember { mutableStateOf(initialTimeMillis != null) }

    val calendar = remember(selectedTimeMillis) {
        Calendar.getInstance().apply { timeInMillis = selectedTimeMillis }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.size(width = 36.dp, height = 4.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                ) {}
            }
        },
        modifier = Modifier.testTag("reminder_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Set Reminder",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (hasReminder) {
                    TextButton(
                        onClick = {
                            hasReminder = false
                            onSaveReminder(null, ReminderRepeat.NONE.name)
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("remove_reminder_button")
                    ) {
                        Icon(Icons.Default.AlarmOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove")
                    }
                }
            }

            Text(
                text = "Quick Presets",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Quick Preset Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetChip(
                    icon = Icons.Default.Nightlight,
                    title = "Today",
                    subtitle = "6:00 PM",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 18)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            if (timeInMillis <= System.currentTimeMillis()) {
                                add(Calendar.HOUR_OF_DAY, 2)
                            }
                        }
                        selectedTimeMillis = cal.timeInMillis
                        hasReminder = true
                    }
                )

                PresetChip(
                    icon = Icons.Default.WbSunny,
                    title = "Tomorrow",
                    subtitle = "9:00 AM",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }
                        selectedTimeMillis = cal.timeInMillis
                        hasReminder = true
                    }
                )

                PresetChip(
                    icon = Icons.Default.CalendarMonth,
                    title = "Next Week",
                    subtitle = "Mon 9:00 AM",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.WEEK_OF_YEAR, 1)
                            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }
                        selectedTimeMillis = cal.timeInMillis
                        hasReminder = true
                    }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Date & Time Picker Trigger Buttons
            Text(
                text = "Custom Schedule",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Date Selector Card
                val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
                Card(
                    modifier = Modifier
                        .weight(1.2f)
                        .clickable {
                            val curYear = calendar.get(Calendar.YEAR)
                            val curMonth = calendar.get(Calendar.MONTH)
                            val curDay = calendar.get(Calendar.DAY_OF_MONTH)

                            DatePickerDialog(context, { _, year, month, dayOfMonth ->
                                val newCal = Calendar.getInstance().apply {
                                    timeInMillis = selectedTimeMillis
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                }
                                selectedTimeMillis = newCal.timeInMillis
                                hasReminder = true
                            }, curYear, curMonth, curDay).show()
                        }
                        .testTag("date_picker_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Date", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(dateFormat.format(Date(selectedTimeMillis)), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Time Selector Card
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val curHour = calendar.get(Calendar.HOUR_OF_DAY)
                            val curMinute = calendar.get(Calendar.MINUTE)

                            TimePickerDialog(context, { _, hourOfDay, minute ->
                                val newCal = Calendar.getInstance().apply {
                                    timeInMillis = selectedTimeMillis
                                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                                    set(Calendar.MINUTE, minute)
                                    set(Calendar.SECOND, 0)
                                }
                                selectedTimeMillis = newCal.timeInMillis
                                hasReminder = true
                            }, curHour, curMinute, false).show()
                        }
                        .testTag("time_picker_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Time", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(timeFormat.format(Date(selectedTimeMillis)), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Repeat options
            Text(
                text = "Repeat",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ReminderRepeat.values().forEach { repeat ->
                    val isSelected = selectedRepeat == repeat.name
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedRepeat = repeat.name },
                        label = { Text(repeat.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("repeat_chip_${repeat.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("cancel_reminder_button")
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        onSaveReminder(selectedTimeMillis, selectedRepeat)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("save_reminder_button")
                ) {
                    Text("Save Reminder")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PresetChip(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag("preset_${title.lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
