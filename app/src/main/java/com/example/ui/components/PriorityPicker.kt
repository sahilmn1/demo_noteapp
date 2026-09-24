package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotePriority

@Composable
fun PriorityPickerRow(
    selectedPriority: String,
    onPrioritySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("priority_picker_row")
    ) {
        NotePriority.values().forEach { priority ->
            val isSelected = selectedPriority.equals(priority.name, ignoreCase = true)
            val color = Color(priority.color)

            Surface(
                onClick = { onPrioritySelected(priority.name) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) color else Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("priority_chip_${priority.name}")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = color,
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Text(
                        text = " " + priority.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
