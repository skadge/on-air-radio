package org.guakamole.onair.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.guakamole.onair.data.FilterData
import org.guakamole.onair.data.FilterItem
import org.guakamole.onair.ui.theme.TagColors

@Composable
fun FilterBar(
        selectedRegions: Set<String>,
        onRegionsChange: (Set<String>) -> Unit,
        selectedStyles: Set<String>,
        onStylesChange: (Set<String>) -> Unit,
        modifier: Modifier = Modifier
) {
        Row(
                modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                FilterDropdown(
                        label = stringResource(org.guakamole.onair.R.string.region),
                        icon = Icons.Default.Public,
                        items = FilterData.regions,
                        selectedIds = selectedRegions,
                        onSelectionChange = onRegionsChange,
                        modifier = Modifier.weight(1f)
                )

                FilterDropdown(
                        label = stringResource(org.guakamole.onair.R.string.style),
                        icon = Icons.Default.Style,
                        items = FilterData.styles,
                        selectedIds = selectedStyles,
                        onSelectionChange = onStylesChange,
                        showGenreDots = true,
                        modifier = Modifier.weight(1f)
                )
        }
}

@Composable
fun FilterDropdown(
        label: String,
        icon: ImageVector,
        items: List<FilterItem>,
        selectedIds: Set<String>,
        onSelectionChange: (Set<String>) -> Unit,
        showGenreDots: Boolean = false,
        modifier: Modifier = Modifier
) {
        var expanded by remember { mutableStateOf(false) }

        Box(modifier = modifier) {
                Surface(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .clickable { expanded = true },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(22.dp)
                ) {
                        Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (selectedIds.isNotEmpty() &&
                                                        !selectedIds.contains("world")
                                        ) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                                Text(
                                                                        text =
                                                                                selectedIds.size
                                                                                        .toString(),
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelSmall
                                                                                        .copy(
                                                                                                fontSize =
                                                                                                        10.sp
                                                                                        ),
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onPrimary
                                                                )
                                                        }
                                                }
                                        }
                                }
                                Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                )
                        }
                }

                DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier =
                                Modifier.width(200.dp).background(MaterialTheme.colorScheme.surface)
                ) {
                        items.forEach { item ->
                                val isSelected =
                                        selectedIds.contains(item.id) ||
                                                (item.id == "world" && selectedIds.isEmpty())

                                DropdownMenuItem(
                                        text = {
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        if (showGenreDots && item.tag.isNotEmpty()
                                                        ) {
                                                                Box(
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                                end =
                                                                                                        8.dp
                                                                                        )
                                                                                        .size(8.dp)
                                                                                        .background(
                                                                                                color =
                                                                                                        TagColors
                                                                                                                .getColorForTag(
                                                                                                                        item.tag
                                                                                                                ),
                                                                                                shape =
                                                                                                        RoundedCornerShape(
                                                                                                                4.dp
                                                                                                        )
                                                                                        )
                                                                )
                                                        }
                                                        Text(
                                                                text = stringResource(item.nameRes),
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium,
                                                                modifier = Modifier.weight(1f)
                                                        )
                                                        if (isSelected) {
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.Default.Check,
                                                                        contentDescription = null,
                                                                        modifier =
                                                                                Modifier.size(
                                                                                        18.dp
                                                                                ),
                                                                        tint =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                )
                                                        }
                                                }
                                        },
                                        onClick = {
                                                val newSelection = selectedIds.toMutableSet()
                                                if (item.id == "world") {
                                                        newSelection.clear()
                                                        newSelection.add("world")
                                                } else {
                                                        newSelection.remove("world")
                                                        if (isSelected) {
                                                                newSelection.remove(item.id)
                                                        } else {
                                                                newSelection.add(item.id)
                                                        }
                                                }
                                                onSelectionChange(newSelection)
                                                // Immediate update is requested, so we don't close
                                                // until clicked
                                                // outside or manual choice
                                                // but let's close for better UX or stay open for
                                                // multi-select?
                                                // The user said "as soon as I click eg a region,
                                                // the list of visible
                                                // stations immediately updates."
                                                // Let's keep it open for multi-select but it
                                                // updates the background.
                                        }
                                )
                        }
                }
        }
}
