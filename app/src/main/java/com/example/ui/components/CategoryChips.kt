package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun CategoryChips(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    onAddNewCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultCategories = listOf("All", "Personal", "Work", "Tasks", "Ideas", "Study")
    val allCategories = (listOf("All") + (defaultCategories.drop(1) + categories).distinct())

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_filter_row")
    ) {
        items(allCategories) { category ->
            val isSelected = (selectedCategory == null && category == "All") || (selectedCategory == category)

            FilterChip(
                selected = isSelected,
                onClick = {
                    if (category == "All") {
                        onCategorySelected(null)
                    } else {
                        onCategorySelected(if (isSelected) null else category)
                    }
                },
                label = { Text(category) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else null,
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("category_chip_$category")
            )
        }

        item {
            FilterChip(
                selected = false,
                onClick = onAddNewCategory,
                label = { Text("+ Tag") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("add_tag_chip")
            )
        }
    }
}
