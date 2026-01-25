package org.guakamole.onair.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import org.guakamole.onair.data.RadioStation

@Composable
fun FilterBar(
        selectedRegions: Set<String>,
        onRegionsChange: (Set<String>) -> Unit,
        selectedStyles: Set<String>,
        onStylesChange: (Set<String>) -> Unit,
        stations: List<RadioStation> = emptyList(),
        modifier: Modifier = Modifier
) {
        // Calculate region counts
        val regionCounts =
                remember(stations) {
                        FilterData.regions.associate { item ->
                                val count =
                                        if (item.id == "world") {
                                                stations.size
                                        } else {
                                                val countries =
                                                        FilterData.getCountriesForFilter(item)
                                                stations.count { station ->
                                                        countries.contains(station.country)
                                                }
                                        }
                                item.id to count
                        }
                }

        // Calculate style counts
        val styleCounts =
                remember(stations) {
                        FilterData.styles.associate { item ->
                                val count =
                                        stations.count { station ->
                                                station.tags.contains(
                                                        item.tag,
                                                        ignoreCase = true
                                                ) || station.primaryTag == item.tag
                                        }
                                item.id to count
                        }
                }

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
                        itemCounts = regionCounts,
                        modifier = Modifier.weight(1f)
                )

                FilterDropdown(
                        label = stringResource(org.guakamole.onair.R.string.style),
                        icon = Icons.Default.Style,
                        items = FilterData.styles,
                        selectedIds = selectedStyles,
                        onSelectionChange = onStylesChange,
                        showGenreDots = true,
                        itemCounts = styleCounts,
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
        itemCounts: Map<String, Int> = emptyMap(),
        modifier: Modifier = Modifier
) {
        var expanded by remember { mutableStateOf(false) }
        var drillDownRegionId by remember { mutableStateOf<String?>(null) }

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
                        onDismissRequest = {
                                expanded = false
                                drillDownRegionId = null
                        },
                        modifier =
                                Modifier.width(220.dp).background(MaterialTheme.colorScheme.surface)
                ) {
                        if (drillDownRegionId == null) {
                                // Top level list (Regions and World)
                                items
                                        .filter {
                                                (it.isRegion && it.id != "world") ||
                                                        it.id == "world"
                                        }
                                        .forEach { item ->
                                                val isSelected =
                                                        selectedIds.contains(item.id) ||
                                                                (item.id == "world" &&
                                                                        selectedIds.isEmpty())
                                                val count = itemCounts[item.id] ?: 0

                                                DropdownMenuItem(
                                                        text = {
                                                                Row(
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically
                                                                ) {
                                                                        Text(
                                                                                text =
                                                                                        stringResource(
                                                                                                item.nameRes
                                                                                        ),
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .bodyMedium,
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                1f
                                                                                        )
                                                                        )
                                                                        Surface(
                                                                                shape =
                                                                                        RoundedCornerShape(
                                                                                                10.dp
                                                                                        ),
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .surfaceVariant
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.7f
                                                                                                ),
                                                                                modifier =
                                                                                        Modifier.padding(
                                                                                                horizontal =
                                                                                                        4.dp
                                                                                        )
                                                                        ) {
                                                                                Text(
                                                                                        text =
                                                                                                count.toString(),
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
                                                                                                        .onSurfaceVariant,
                                                                                        modifier =
                                                                                                Modifier.padding(
                                                                                                        horizontal =
                                                                                                                6.dp,
                                                                                                        vertical =
                                                                                                                2.dp
                                                                                                )
                                                                                )
                                                                        }
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.width(
                                                                                                8.dp
                                                                                        )
                                                                        )
                                                                        if (item.isRegion) {
                                                                                Icon(
                                                                                        imageVector =
                                                                                                Icons.Default
                                                                                                        .KeyboardArrowRight,
                                                                                        contentDescription =
                                                                                                null,
                                                                                        modifier =
                                                                                                Modifier.size(
                                                                                                        18.dp
                                                                                                ),
                                                                                        tint =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onSurfaceVariant
                                                                                )
                                                                        } else if (isSelected) {
                                                                                Icon(
                                                                                        imageVector =
                                                                                                Icons.Default
                                                                                                        .Check,
                                                                                        contentDescription =
                                                                                                null,
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
                                                                if (item.isRegion) {
                                                                        drillDownRegionId = item.id
                                                                } else {
                                                                        onSelectionChange(
                                                                                setOf("world")
                                                                        )
                                                                        expanded = false
                                                                }
                                                        }
                                                )
                                        }
                        } else {
                                // Drill down level (Countries in a region)
                                val currentRegion = items.find { it.id == drillDownRegionId }
                                val context = androidx.compose.ui.platform.LocalContext.current
                                val subItems =
                                        remember(drillDownRegionId, items) {
                                                items
                                                        .filter { item ->
                                                                currentRegion?.countries?.contains(
                                                                        item.nameRes
                                                                ) == true && !item.isRegion
                                                        }
                                                        .sortedBy { context.getString(it.nameRes) }
                                        }

                                // Region-level toggle at the top
                                DropdownMenuItem(
                                        text = {
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Icon(
                                                                imageVector =
                                                                        Icons.Default.ArrowBack,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                                text =
                                                                        stringResource(
                                                                                org.guakamole
                                                                                        .onair
                                                                                        .R
                                                                                        .string
                                                                                        .back
                                                                        ),
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelLarge,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .primary
                                                        )
                                                }
                                        },
                                        onClick = { drillDownRegionId = null }
                                )

                                Divider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                )

                                // The region itself option
                                val regionIsSelected = selectedIds.contains(drillDownRegionId)
                                DropdownMenuItem(
                                        text = {
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Text(
                                                                text =
                                                                        stringResource(
                                                                                currentRegion
                                                                                        ?.nameRes
                                                                                        ?: 0
                                                                        ),
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium.copy(
                                                                                fontWeight =
                                                                                        androidx.compose
                                                                                                .ui
                                                                                                .text
                                                                                                .font
                                                                                                .FontWeight
                                                                                                .Bold
                                                                        ),
                                                                modifier = Modifier.weight(1f)
                                                        )
                                                        if (regionIsSelected) {
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
                                                newSelection.remove("world")
                                                if (regionIsSelected) {
                                                        newSelection.remove(drillDownRegionId!!)
                                                } else {
                                                        // Deselect children when selecting parent
                                                        // for clarity
                                                        subItems.forEach {
                                                                newSelection.remove(it.id)
                                                        }
                                                        newSelection.add(drillDownRegionId!!)
                                                }
                                                onSelectionChange(newSelection)
                                        }
                                )

                                subItems.forEach { item ->
                                        val isSelected = selectedIds.contains(item.id)
                                        val count = itemCounts[item.id] ?: 0

                                        DropdownMenuItem(
                                                text = {
                                                        Row(
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                Text(
                                                                        text =
                                                                                stringResource(
                                                                                        item.nameRes
                                                                                ),
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodyMedium,
                                                                        modifier =
                                                                                Modifier.weight(1f)
                                                                )
                                                                Surface(
                                                                        shape =
                                                                                RoundedCornerShape(
                                                                                        10.dp
                                                                                ),
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .surfaceVariant
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.7f
                                                                                        ),
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                        horizontal =
                                                                                                4.dp
                                                                                )
                                                                ) {
                                                                        Text(
                                                                                text =
                                                                                        count.toString(),
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
                                                                                                .onSurfaceVariant,
                                                                                modifier =
                                                                                        Modifier.padding(
                                                                                                horizontal =
                                                                                                        6.dp,
                                                                                                vertical =
                                                                                                        2.dp
                                                                                        )
                                                                        )
                                                                }
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.width(8.dp)
                                                                )
                                                                if (isSelected) {
                                                                        Icon(
                                                                                imageVector =
                                                                                        Icons.Default
                                                                                                .Check,
                                                                                contentDescription =
                                                                                        null,
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
                                                        val newSelection =
                                                                selectedIds.toMutableSet()
                                                        newSelection.remove("world")
                                                        newSelection.remove(
                                                                drillDownRegionId!!
                                                        ) // Deselect parent when selecting child
                                                        if (isSelected) {
                                                                newSelection.remove(item.id)
                                                        } else {
                                                                newSelection.add(item.id)
                                                        }
                                                        onSelectionChange(newSelection)
                                                }
                                        )
                                }
                        }
                }
        }
}
