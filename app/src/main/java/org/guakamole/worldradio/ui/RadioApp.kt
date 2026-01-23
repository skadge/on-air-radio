package org.guakamole.worldradio.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.guakamole.worldradio.data.RadioRepository
import org.guakamole.worldradio.data.RadioStation

enum class Screen {
    StationList,
    NowPlaying
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun RadioApp(
        currentStationId: String?,
        isPlaying: Boolean,
        isBuffering: Boolean,
        currentTitle: String?,
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
                    TopAppBar(
                            title = { Text("Radio") },
                            colors =
                                    TopAppBarDefaults.topAppBarColors(
                                            containerColor =
                                                    MaterialTheme.colorScheme.primaryContainer,
                                            titleContentColor =
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                    )
                }
            },
            bottomBar = {
                // Mini player when on station list and something is playing
                if (currentScreen == Screen.StationList && currentStation != null) {
                    MiniPlayer(
                            station = currentStation,
                            isPlaying = isPlaying,
                            isBuffering = isBuffering,
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
                            stations = stations,
                            currentStationId = currentStationId,
                            onStationClick = { station -> onStationSelect(station) },
                            onFavoriteToggle = { station ->
                                RadioRepository.toggleFavorite(station.id)
                                refreshTrigger++
                            },
                            modifier = Modifier.padding(paddingValues)
                    )
                }
                Screen.NowPlaying -> {
                    NowPlayingScreen(
                            station = currentStation,
                            isPlaying = isPlaying,
                            isBuffering = isBuffering,
                            currentTitle = currentTitle,
                            onPlayPause = onPlayPause,
                            onStop = onStop,
                            onPrevious = onPrevious,
                            onNext = onNext,
                            onBackToList = { currentScreen = Screen.StationList }
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
        onPlayPause: () -> Unit,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 8.dp,
            onClick = onClick
    ) {
        Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = station.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                        text = if (isBuffering) "Loading..." else station.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            if (isBuffering) {
                CircularProgressIndicator(
                        modifier = Modifier.size(40.dp).padding(8.dp),
                        strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onPlayPause) {
                    Icon(
                            imageVector =
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
            }
        }
    }
}
