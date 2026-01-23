package org.guakamole.worldradio.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import org.guakamole.worldradio.data.RadioStation

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StationListScreen(
        stations: List<RadioStation>,
        currentStationId: String?,
        onStationClick: (RadioStation) -> Unit,
        onFavoriteToggle: (RadioStation) -> Unit,
        modifier: Modifier = Modifier
) {
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        LazyColumn(
                state = listState,
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                items(stations, key = { it.id }) { station ->
                        StationCard(
                                station = station,
                                isPlaying = station.id == currentStationId,
                                onClick = { onStationClick(station) },
                                onFavoriteClick = {
                                        onFavoriteToggle(station)
                                        // If it was favorited, scroll to top
                                        if (!station.isFavorite) {
                                                scope.launch { listState.animateScrollToItem(0) }
                                        }
                                },
                                modifier = Modifier.animateItemPlacement()
                        )
                }
        }
}

@Composable
fun StationCard(
        station: RadioStation,
        isPlaying: Boolean,
        onClick: () -> Unit,
        onFavoriteClick: () -> Unit,
        modifier: Modifier = Modifier
) {
        Card(
                modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
                shape = RoundedCornerShape(16.dp),
                colors =
                        CardDefaults.cardColors(
                                containerColor =
                                        if (isPlaying) {
                                                MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                        }
                        ),
                elevation =
                        CardDefaults.cardElevation(defaultElevation = if (isPlaying) 8.dp else 2.dp)
        ) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                        // Station Logo
                        Surface(
                                modifier = Modifier.size(64.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface
                        ) {
                                val placeholder = rememberVectorPainter(Icons.Default.Radio)
                                AsyncImage(
                                        model =
                                                ImageRequest.Builder(LocalContext.current)
                                                        .data(station.logoUrl)
                                                        .crossfade(true)
                                                        .build(),
                                        placeholder = placeholder,
                                        error = placeholder,
                                        contentDescription = "${station.name} logo",
                                        modifier =
                                                Modifier.fillMaxSize()
                                                        .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                )
                        }

                        // Station Info
                        Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        Text(
                                                text = station.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color =
                                                        if (isPlaying) {
                                                                MaterialTheme.colorScheme
                                                                        .onPrimaryContainer
                                                        } else {
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                        },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                        )

                                        IconButton(
                                                onClick = onFavoriteClick,
                                                modifier = Modifier.size(24.dp)
                                        ) {
                                                Icon(
                                                        imageVector =
                                                                if (station.isFavorite)
                                                                        Icons.Filled.Star
                                                                else Icons.Filled.StarBorder,
                                                        contentDescription =
                                                                if (station.isFavorite)
                                                                        "Remove from favorites"
                                                                else "Add to favorites",
                                                        tint =
                                                                if (station.isFavorite)
                                                                        Color(0xFFFFD700)
                                                                else
                                                                        LocalContentColor.current
                                                                                .copy(alpha = 0.6f)
                                                )
                                        }
                                }

                                Text(
                                        text = station.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color =
                                                if (isPlaying) {
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                                .copy(alpha = 0.7f)
                                                } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                                .copy(alpha = 0.7f)
                                                },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )
                                // ... rest of the file ...

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        AssistChip(
                                                onClick = {},
                                                label = {
                                                        Text(
                                                                text = station.genre,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall
                                                        )
                                                },
                                                modifier = Modifier.height(24.dp)
                                        )
                                        AssistChip(
                                                onClick = {},
                                                label = {
                                                        Text(
                                                                text = station.country,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall
                                                        )
                                                },
                                                modifier = Modifier.height(24.dp)
                                        )
                                }
                        }

                        // Playing indicator
                        if (isPlaying) {
                                Surface(
                                        modifier = Modifier.size(12.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary
                                ) {}
                        }
                }
        }
}
