package org.guakamole.onair.ui

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
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.guakamole.onair.R
import org.guakamole.onair.data.RadioRepository
import org.guakamole.onair.data.RadioStation
import org.guakamole.onair.data.SortOrder
import org.guakamole.onair.ui.theme.TagColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StationListScreen(
        stations: List<RadioStation>,
        currentStationId: String?,
        onStationClick: (RadioStation) -> Unit,
        onFavoriteToggle: (RadioStation) -> Unit,
        modifier: Modifier = Modifier
) {
        if (stations.isEmpty()) {
                Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                                text = stringResource(R.string.no_stations_found),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                }
                return
        }

        val gridState = rememberLazyGridState()
        var sortOrder by remember { mutableStateOf(RadioRepository.getSortOrder()) }

        // Get favorites and sorted non-favorites
        val favorites = stations.filter { it.isFavorite }
        val others =
                remember(sortOrder, stations) {
                        val nonFavorites = stations.filter { !it.isFavorite }
                        when (sortOrder) {
                                SortOrder.ALPHABETICAL ->
                                        nonFavorites.sortedBy { it.name.lowercase() }
                                SortOrder.MOST_LISTENED ->
                                        nonFavorites.sortedByDescending {
                                                RadioRepository.getListenCount(it.id)
                                        }
                        }
                }
        val hasFavorites = favorites.isNotEmpty()

        LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                state = gridState,
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                if (hasFavorites) {
                        item(
                                span = {
                                        androidx.compose.foundation.lazy.grid.GridItemSpan(
                                                maxLineSpan
                                        )
                                }
                        ) { SectionHeader(stringResource(R.string.favorites)) }

                        items(favorites, key = { it.id }) { station ->
                                StationCard(
                                        station = station,
                                        isPlaying = station.id == currentStationId,
                                        onClick = { onStationClick(station) },
                                        onFavoriteClick = { onFavoriteToggle(station) },
                                        modifier = Modifier.animateItemPlacement()
                                )
                        }

                        item(
                                span = {
                                        androidx.compose.foundation.lazy.grid.GridItemSpan(
                                                maxLineSpan
                                        )
                                }
                        ) {
                                AllStationsSectionHeader(
                                        sortOrder = sortOrder,
                                        onSortOrderChange = { newOrder ->
                                                sortOrder = newOrder
                                                RadioRepository.setSortOrder(newOrder)
                                        },
                                        modifier = Modifier.padding(top = 16.dp)
                                )
                        }
                } else {
                        // No favorites - show sort header for all stations
                        item(
                                span = {
                                        androidx.compose.foundation.lazy.grid.GridItemSpan(
                                                maxLineSpan
                                        )
                                }
                        ) {
                                AllStationsSectionHeader(
                                        sortOrder = sortOrder,
                                        onSortOrderChange = { newOrder ->
                                                sortOrder = newOrder
                                                RadioRepository.setSortOrder(newOrder)
                                        }
                                )
                        }
                }

                items(others, key = { it.id }) { station ->
                        StationCard(
                                station = station,
                                isPlaying = station.id == currentStationId,
                                onClick = { onStationClick(station) },
                                onFavoriteClick = { onFavoriteToggle(station) },
                                modifier = Modifier.animateItemPlacement()
                        )
                }
        }
}

@Composable
private fun AllStationsSectionHeader(
        sortOrder: SortOrder,
        onSortOrderChange: (SortOrder) -> Unit,
        modifier: Modifier = Modifier
) {
        Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
                Text(
                        text = stringResource(R.string.all_stations),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        // Sort by name button
                        IconButton(
                                onClick = { onSortOrderChange(SortOrder.ALPHABETICAL) },
                                modifier = Modifier.size(32.dp)
                        ) {
                                Icon(
                                        imageVector = Icons.Outlined.SortByAlpha,
                                        contentDescription = stringResource(R.string.sort_by_name),
                                        tint =
                                                if (sortOrder == SortOrder.ALPHABETICAL)
                                                        MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                )
                        }

                        // Sort by most listened button
                        IconButton(
                                onClick = { onSortOrderChange(SortOrder.MOST_LISTENED) },
                                modifier = Modifier.size(32.dp)
                        ) {
                                Icon(
                                        imageVector = Icons.Outlined.TrendingUp,
                                        contentDescription =
                                                stringResource(R.string.sort_by_most_listened),
                                        tint =
                                                if (sortOrder == SortOrder.MOST_LISTENED)
                                                        MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                )
                        }
                }
        }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
        Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = modifier.padding(bottom = 8.dp)
        )
}

@Composable
fun StationCard(
        station: RadioStation,
        isPlaying: Boolean,
        onClick: () -> Unit,
        onFavoriteClick: () -> Unit,
        modifier: Modifier = Modifier
) {
        val baseColor = TagColors.getColorForTag(station.primaryTag)
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

                        if (station.tags.isNotEmpty()) {
                                val allTags = station.tags.split(",").map { it.trim() }
                                // Put primary tag first, then other tags sorted
                                val sortedTags =
                                        if (station.primaryTag.isNotEmpty()) {
                                                listOf(station.primaryTag) +
                                                        allTags.filter { it != station.primaryTag }
                                        } else {
                                                allTags
                                        }

                                val annotatedText = buildAnnotatedString {
                                        sortedTags.forEachIndexed { index, tag ->
                                                if (index > 0) append(" • ")
                                                if (tag == station.primaryTag) {
                                                        withStyle(
                                                                SpanStyle(
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                        ) { append(translateTag(tag)) }
                                                } else {
                                                        append(translateTag(tag))
                                                }
                                        }
                                }

                                Text(
                                        text = annotatedText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.7f),
                                        modifier =
                                                Modifier.padding(start = 12.dp, top = 64.dp)
                                                        .align(Alignment.TopStart)
                                                        .fillMaxWidth(0.6f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )
                        }

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
                                contentScale = ContentScale.Fit
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

@Composable
private fun translateTag(tag: String): String {
        return when (tag.lowercase()) {
                "pop" -> stringResource(R.string.tag_pop)
                "rock" -> stringResource(R.string.tag_rock)
                "hits" -> stringResource(R.string.tag_hits)
                "jazz" -> stringResource(R.string.tag_jazz)
                "classical" -> stringResource(R.string.tag_classical)
                "news" -> stringResource(R.string.tag_news)
                "talk" -> stringResource(R.string.tag_talk)
                "ambient" -> stringResource(R.string.tag_ambient)
                "world" -> stringResource(R.string.tag_world)
                "oldies" -> stringResource(R.string.tag_oldies)
                else -> tag.replaceFirstChar { it.uppercase() }
        }
}
