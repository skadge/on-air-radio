package org.guakamole.worldradio.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import org.guakamole.worldradio.ui.theme.GenreColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StationListScreen(
        stations: List<RadioStation>,
        currentStationId: String?,
        onStationClick: (RadioStation) -> Unit,
        onFavoriteToggle: (RadioStation) -> Unit,
        modifier: Modifier = Modifier
) {
        val gridState = rememberLazyGridState()
        val scope = rememberCoroutineScope()

        LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                items(stations, key = { it.id }) { station ->
                        StationCard(
                                station = station,
                                isPlaying = station.id == currentStationId,
                                onClick = { onStationClick(station) },
                                onFavoriteClick = {
                                        onFavoriteToggle(station)
                                        if (!station.isFavorite) {
                                                scope.launch { gridState.animateScrollToItem(0) }
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
        val baseColor = GenreColors.getColorForGenre(station.genre)
        val gradient =
                Brush.verticalGradient(
                        colors =
                                listOf(
                                        baseColor,
                                        Color(
                                                red = baseColor.red * 0.7f,
                                                green = baseColor.green * 0.7f,
                                                blue = baseColor.blue * 0.7f,
                                                alpha = baseColor.alpha
                                        )
                                )
                )

        Card(
                modifier = modifier.fillMaxWidth().height(110.dp).clickable(onClick = onClick),
                shape = RoundedCornerShape(12.dp),
                elevation =
                        CardDefaults.cardElevation(
                                defaultElevation = if (isPlaying) 8.dp else 2.dp
                        ),
                border =
                        if (isPlaying)
                                androidx.compose.foundation.BorderStroke(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary
                                )
                        else null
        ) {
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .background(gradient)
                                        .clip(RoundedCornerShape(12.dp))
                ) {
                        // Station Name at top-left
                        Text(
                                text = station.name,
                                style =
                                        MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                lineHeight = 22.sp
                                        ),
                                color = Color.White,
                                modifier =
                                        Modifier.padding(12.dp)
                                                .align(Alignment.TopStart)
                                                .fillMaxWidth(0.7f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                        )

                        // Angled Station Logo at bottom-right
                        AsyncImage(
                                model =
                                        ImageRequest.Builder(LocalContext.current)
                                                .data(
                                                        if (station.logoResId != 0)
                                                                station.logoResId
                                                        else station.logoUrl
                                                )
                                                .crossfade(true)
                                                .build(),
                                contentDescription = null,
                                modifier =
                                        Modifier.align(Alignment.BottomEnd)
                                                .offset(
                                                        x = 8.dp,
                                                        y = 8.dp
                                                ) // Less aggressive offset
                                                .size(70.dp)
                                                .rotate(-25f), // Handled by parent clip
                                contentScale = ContentScale.Crop
                        )

                        // Country and Favorite Icon
                        Row(
                                modifier =
                                        Modifier.align(Alignment.BottomStart)
                                                .padding(bottom = 4.dp, start = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                                        if (station.isFavorite) Color.White
                                                        else Color.White.copy(alpha = 0.5f),
                                                modifier = Modifier.size(20.dp)
                                        )
                                }
                                Text(
                                        text = stringResource(station.country),
                                        style =
                                                MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium
                                                ),
                                        color = Color.White.copy(alpha = 0.4f)
                                )
                        }
                }
        }
}
