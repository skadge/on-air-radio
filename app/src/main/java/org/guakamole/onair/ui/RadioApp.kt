package org.guakamole.onair.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.guakamole.onair.BuildConfig
import org.guakamole.onair.R
import org.guakamole.onair.data.FilterData
import org.guakamole.onair.data.RadioRepository
import org.guakamole.onair.data.RadioStation

enum class Screen {
        StationList,
        NowPlaying,
        Premium
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun RadioApp(
        currentStationId: String?,
        isPlaying: Boolean,
        isBuffering: Boolean,
        currentTitle: String?,
        currentArtist: String?,
        onStationSelect: (RadioStation) -> Unit,
        onPlayPause: () -> Unit,
        onStop: () -> Unit,
        onPrevious: () -> Unit,
        onNext: () -> Unit,
        modifier: Modifier = Modifier
) {
        var currentScreen by remember { mutableStateOf(Screen.StationList) }
        var refreshTrigger by remember { mutableStateOf(0) }
        val stations = remember(refreshTrigger) { RadioRepository.stations }

        var selectedRegions by remember { mutableStateOf(RadioRepository.getSelectedRegions()) }
        var selectedStyles by remember { mutableStateOf(RadioRepository.getSelectedStyles()) }
        var searchQuery by remember { mutableStateOf("") }

        LaunchedEffect(selectedRegions) { RadioRepository.setSelectedRegions(selectedRegions) }
        LaunchedEffect(selectedStyles) { RadioRepository.setSelectedStyles(selectedStyles) }

        val filteredStations =
                remember(stations, selectedRegions, selectedStyles, searchQuery) {
                        stations.filter { station ->
                                val regionMatch =
                                        if (selectedRegions.isEmpty() ||
                                                        selectedRegions.contains("world")
                                        ) {
                                                true
                                        } else {
                                                selectedRegions.any { regId ->
                                                        val filterItem =
                                                                FilterData.regions.find {
                                                                        it.id == regId
                                                                }
                                                        filterItem?.let {
                                                                if (it.countries.isEmpty()) {
                                                                        station.country ==
                                                                                it.nameRes
                                                                } else {
                                                                        it.countries.contains(
                                                                                station.country
                                                                        )
                                                                }
                                                        }
                                                                ?: false
                                                }
                                        }

                                val styleMatch =
                                        if (selectedStyles.isEmpty() ||
                                                        selectedStyles.contains("world")
                                        ) {
                                                true
                                        } else {
                                                selectedStyles.any { styleId ->
                                                        val filterItem =
                                                                FilterData.styles.find {
                                                                        it.id == styleId
                                                                }
                                                        filterItem?.let {
                                                                station.tags.contains(
                                                                        it.tag,
                                                                        ignoreCase = true
                                                                ) || station.primaryTag == it.tag
                                                        }
                                                                ?: false
                                                }
                                        }

                                val searchMatch =
                                        if (searchQuery.isBlank()) {
                                                true
                                        } else {
                                                station.name.contains(
                                                        searchQuery,
                                                        ignoreCase = true
                                                ) ||
                                                        station.description.contains(
                                                                searchQuery,
                                                                ignoreCase = true
                                                        ) ||
                                                        station.tags.contains(
                                                                searchQuery,
                                                                ignoreCase = true
                                                        )
                                        }

                                regionMatch && styleMatch && searchMatch
                        }
                }

        val currentStation = currentStationId?.let { id -> stations.find { it.id == id } }

        // Navigate to now playing when a station starts
        LaunchedEffect(currentStationId) {
                if (currentStationId != null) {
                        currentScreen = Screen.NowPlaying
                }
        }

        Scaffold(
                modifier = modifier,
                topBar = {
                        if (currentScreen == Screen.StationList) {
                                Column {
                                        SearchTopBar(
                                                query = searchQuery,
                                                onQueryChange = { searchQuery = it },
                                                onPremiumClick = { currentScreen = Screen.Premium }
                                        )
                                        FilterBar(
                                                selectedRegions = selectedRegions,
                                                onRegionsChange = { selectedRegions = it },
                                                selectedStyles = selectedStyles,
                                                onStylesChange = { selectedStyles = it },
                                                stations = stations
                                        )
                                }
                        }
                },
                bottomBar = {
                        if (currentScreen == Screen.StationList && currentStation != null) {
                                MiniPlayer(
                                        station = currentStation,
                                        isPlaying = isPlaying,
                                        isBuffering = isBuffering,
                                        currentTitle = currentTitle,
                                        currentArtist = currentArtist,
                                        onPlayPause = onPlayPause,
                                        onClick = { currentScreen = Screen.NowPlaying }
                                )
                        }
                }
        ) { paddingValues ->
                AnimatedContent(
                        targetState = currentScreen,
                        label = "screen_transition",
                        transitionSpec = {
                                if (targetState == Screen.NowPlaying) {
                                        slideInVertically { it } + fadeIn() with
                                                slideOutVertically { -it } + fadeOut()
                                } else {
                                        slideInVertically { -it } + fadeIn() with
                                                slideOutVertically { it } + fadeOut()
                                }
                        }
                ) { screen ->
                        when (screen) {
                                Screen.StationList -> {
                                        StationListScreen(
                                                stations = filteredStations,
                                                currentStationId = currentStationId,
                                                onStationClick = { station ->
                                                        if (station.id == currentStationId) {
                                                                currentScreen = Screen.NowPlaying
                                                        } else {
                                                                onStationSelect(station)
                                                        }
                                                },
                                                onFavoriteToggle = { station ->
                                                        RadioRepository.toggleFavorite(station.id)
                                                        refreshTrigger++
                                                },
                                                modifier = Modifier.padding(paddingValues)
                                        )
                                }
                                Screen.NowPlaying -> {
                                        val currentIndex =
                                                currentStation?.let { stations.indexOf(it) } ?: -1
                                        val prevStation =
                                                if (currentIndex >= 0 && stations.isNotEmpty()) {
                                                        stations[
                                                                (currentIndex - 1 + stations.size) %
                                                                        stations.size]
                                                } else null
                                        val nextStation =
                                                if (currentIndex >= 0 && stations.isNotEmpty()) {
                                                        stations[(currentIndex + 1) % stations.size]
                                                } else null

                                        NowPlayingScreen(
                                                station = currentStation,
                                                isPlaying = isPlaying,
                                                isBuffering = isBuffering,
                                                currentTitle = currentTitle,
                                                currentArtist = currentArtist,
                                                previousStationName = prevStation?.name,
                                                nextStationName = nextStation?.name,
                                                onPlayPause = onPlayPause,
                                                onStop = onStop,
                                                onPrevious = onPrevious,
                                                onNext = onNext,
                                                onFavoriteToggle = {
                                                        currentStation?.let {
                                                                RadioRepository.toggleFavorite(
                                                                        it.id
                                                                )
                                                                refreshTrigger++
                                                        }
                                                },
                                                onBackToList = {
                                                        currentScreen = Screen.StationList
                                                }
                                        )
                                }
                                Screen.Premium -> {
                                        PremiumScreen(
                                                onBack = { currentScreen = Screen.StationList }
                                        )
                                }
                        }
                }
        }
}

@Composable
fun MiniPlayer(
        station: RadioStation,
        isPlaying: Boolean,
        isBuffering: Boolean,
        currentTitle: String?,
        currentArtist: String?,
        onPlayPause: () -> Unit,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
        Surface(
                modifier =
                        modifier.fillMaxWidth()
                                .padding(16.dp)
                                .height(64.dp)
                                .clickable(onClick = onClick),
                shape = CircleShape,
                color = Color(0xFF1E2130).copy(alpha = 0.95f),
                shadowElevation = 12.dp
        ) {
                Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                        Surface(
                                modifier = Modifier.size(48.dp).clip(CircleShape),
                                color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
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
                                        contentScale = ContentScale.Fit
                                )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                verticalArrangement = Arrangement.Center
                        ) {
                                Text(
                                        text = station.name,
                                        style =
                                                MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold
                                                ),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )
                                val displayTitle =
                                        if (!currentTitle.isNullOrBlank() &&
                                                        currentTitle != station.name
                                        ) {
                                                if (!currentArtist.isNullOrBlank() &&
                                                                currentArtist != station.name
                                                ) {
                                                        "$currentArtist - $currentTitle"
                                                } else {
                                                        currentTitle
                                                }
                                        } else {
                                                stringResource(station.country)
                                        }
                                Text(
                                        text =
                                                if (isBuffering) stringResource(R.string.buffering)
                                                else displayTitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f),
                                        maxLines = 1
                                )
                        }

                        IconButton(
                                onClick = onPlayPause,
                                modifier =
                                        Modifier.size(48.dp)
                                                .background(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        shape = CircleShape
                                                )
                        ) {
                                if (isBuffering) {
                                        CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                        )
                                } else {
                                        Icon(
                                                imageVector =
                                                        if (isPlaying) Icons.Default.Pause
                                                        else Icons.Default.PlayArrow,
                                                contentDescription =
                                                        if (isPlaying)
                                                                stringResource(R.string.pause)
                                                        else stringResource(R.string.play),
                                                tint = Color.White
                                        )
                                }
                        }
                }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(
        query: String,
        onQueryChange: (String) -> Unit,
        onPremiumClick: () -> Unit = {},
        modifier: Modifier = Modifier
) {
        val gradient =
                Brush.verticalGradient(
                        colors =
                                listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.background
                                )
                )

        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .background(gradient)
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
                OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        placeholder = {
                                Text(
                                        text = stringResource(R.string.search_stations),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color =
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                        alpha = 0.6f
                                                )
                                )
                        },
                        leadingIcon = {
                                Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                )
                        },
                        trailingIcon = {
                                Row {
                                        if (query.isNotEmpty()) {
                                                IconButton(onClick = { onQueryChange("") }) {
                                                        Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Clear search",
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }
                                        }
                                        if (!BuildConfig.IS_FDROID_BUILD) {
                                                IconButton(onClick = onPremiumClick) {
                                                        Icon(
                                                                imageVector = Icons.Default.Star,
                                                                contentDescription =
                                                                        stringResource(
                                                                                R.string
                                                                                        .premium_features
                                                                        ),
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .primary
                                                        )
                                                }
                                        }
                                }
                        },
                        colors =
                                TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor =
                                                MaterialTheme.colorScheme.outline.copy(
                                                        alpha = 0.3f
                                                ),
                                        containerColor =
                                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                                        alpha = 0.5f
                                                )
                                ),
                        shape = RoundedCornerShape(28.dp),
                        singleLine = true,
                        textStyle =
                                MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                )
                )
        }
}
