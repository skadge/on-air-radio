package org.guakamole.worldradio.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import org.guakamole.worldradio.R
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
                modifier = modifier.fillMaxWidth().height(100.dp).clickable(onClick = onClick),
                shape = RoundedCornerShape(16.dp),
                colors =
                        CardDefaults.cardColors(
                                containerColor =
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                elevation =
                        CardDefaults.cardElevation(
                                defaultElevation = if (isPlaying) 8.dp else 4.dp
                        ),
                border =
                        if (isPlaying)
                                androidx.compose.foundation.BorderStroke(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary
                                )
                        else null
        ) {
                Box(modifier = Modifier.fillMaxSize()) {
                        // Background Gradient
                        Box(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(
                                                        brush =
                                                                Brush.horizontalGradient(
                                                                        colors =
                                                                                listOf(
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .surface,
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .surfaceVariant
                                                                                )
                                                                )
                                                )
                        ) {
                                // Station Logo
                                AsyncImage(
                                        model =
                                                ImageRequest.Builder(LocalContext.current)
                                                        .data(station.logoUrl)
                                                        .crossfade(true)
                                                        .build(),
                                        contentDescription = null,
                                        modifier =
                                                Modifier.align(Alignment.CenterStart)
                                                        .padding(start = 16.dp)
                                                        .size(80.dp)
                                                        .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                )
                        }

                        // Content Overlay
                        Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Spacer(modifier = Modifier.width(96.dp))

                                Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.Center
                                ) {
                                        Text(
                                                text = station.name,
                                                style =
                                                        MaterialTheme.typography.titleLarge.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                letterSpacing = 0.5.sp
                                                        ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                        text =
                                                                stringResource(station.genre)
                                                                        .uppercase(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                )

                                                Text(
                                                        text =
                                                                " • ${stringResource(station.country)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant.copy(
                                                                        alpha = 0.7f
                                                                )
                                                )
                                        }
                                }

                                IconButton(
                                        onClick = onFavoriteClick,
                                        modifier = Modifier.size(32.dp)
                                ) {
                                        Icon(
                                                imageVector =
                                                        if (station.isFavorite) Icons.Filled.Star
                                                        else Icons.Filled.StarBorder,
                                                contentDescription =
                                                        if (station.isFavorite)
                                                                stringResource(
                                                                        R.string
                                                                                .remove_from_favorites
                                                                )
                                                        else
                                                                stringResource(
                                                                        R.string.add_to_favorites
                                                                ),
                                                tint =
                                                        if (station.isFavorite)
                                                                MaterialTheme.colorScheme.secondary
                                                        else
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant.copy(
                                                                        alpha = 0.4f
                                                                ),
                                                modifier = Modifier.size(24.dp)
                                        )
                                }
                        }
                }
        }
}
