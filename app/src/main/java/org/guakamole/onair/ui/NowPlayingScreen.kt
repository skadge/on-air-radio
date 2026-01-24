package org.guakamole.onair.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.guakamole.onair.R
import org.guakamole.onair.data.RadioStation

@Composable
fun NowPlayingScreen(
        station: RadioStation?,
        isPlaying: Boolean,
        isBuffering: Boolean,
        currentTitle: String?,
        previousStationName: String?,
        nextStationName: String?,
        onPlayPause: () -> Unit,
        onStop: () -> Unit,
        onPrevious: () -> Unit,
        onNext: () -> Unit,
        onFavoriteToggle: () -> Unit,
        onBackToList: () -> Unit,
        modifier: Modifier = Modifier
) {
        // Pulsing animation for playing state (limited to 3 cycles)
        val scaleAnim = remember { Animatable(1f) }
        LaunchedEffect(station?.id, isPlaying, isBuffering) {
                if (isPlaying && !isBuffering) {
                        repeat(3) {
                                scaleAnim.animateTo(
                                        targetValue = 1.05f,
                                        animationSpec = tween(1000, easing = EaseInOut)
                                )
                                scaleAnim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(1000, easing = EaseInOut)
                                )
                        }
                } else {
                        scaleAnim.snapTo(1f)
                }
        }

        var showMenu by remember { mutableStateOf(false) }

        Column(
                modifier =
                        modifier.fillMaxSize()
                                .background(
                                        Brush.verticalGradient(
                                                colors =
                                                        listOf(
                                                                MaterialTheme.colorScheme
                                                                        .primaryContainer,
                                                                MaterialTheme.colorScheme.background
                                                        )
                                        )
                                )
                                .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
                // Top Bar with Back and Menu
                Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        IconButton(onClick = onBackToList) {
                                Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription =
                                                stringResource(R.string.back_to_stations),
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.onBackground
                                )
                        }

                        Box {
                                IconButton(onClick = { showMenu = true }) {
                                        Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Options",
                                                modifier = Modifier.size(28.dp),
                                                tint = MaterialTheme.colorScheme.onBackground
                                        )
                                }
                                DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                ) {
                                        if (station != null) {
                                                DropdownMenuItem(
                                                        text = {
                                                                Text(
                                                                        if (station.isFavorite)
                                                                                stringResource(
                                                                                        R.string
                                                                                                .remove_from_favorites
                                                                                )
                                                                        else
                                                                                stringResource(
                                                                                        R.string
                                                                                                .add_to_favorites
                                                                                )
                                                                )
                                                        },
                                                        onClick = {
                                                                onFavoriteToggle()
                                                                showMenu = false
                                                        },
                                                        leadingIcon = {
                                                                Icon(
                                                                        imageVector =
                                                                                if (station.isFavorite
                                                                                )
                                                                                        Icons.Default
                                                                                                .Star
                                                                                else
                                                                                        Icons.Default
                                                                                                .StarBorder,
                                                                        contentDescription = null
                                                                )
                                                        }
                                                )
                                        }
                                }
                        }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (station != null) {

                        // Station Logo with animation
                        Surface(
                                modifier = Modifier.size(260.dp).scale(scaleAnim.value),
                                shape = RoundedCornerShape(32.dp),
                                shadowElevation = 0.dp,
                                color = Color.Transparent
                        ) {
                                val placeholder = rememberVectorPainter(Icons.Default.Radio)
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
                                        placeholder = placeholder,
                                        error = placeholder,
                                        contentDescription =
                                                stringResource(
                                                        R.string.station_logo_description,
                                                        station.name
                                                ),
                                        modifier =
                                                Modifier.fillMaxSize()
                                                        .clip(RoundedCornerShape(32.dp)),
                                        contentScale = ContentScale.Crop
                                )
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        // Station Name
                        Text(
                                text = station.name,
                                style =
                                        MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold
                                        ),
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Current Title (from stream metadata) or description
                        Text(
                                text = station.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Genre and Country chips
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SuggestionChip(
                                        onClick = {},
                                        label = { Text(stringResource(station.genre)) }
                                )
                                SuggestionChip(
                                        onClick = {},
                                        label = { Text(stringResource(station.country)) }
                                )
                        }

                        // Dedicated Song Title Box
                        val showTitle =
                                !currentTitle.isNullOrBlank() && currentTitle != station.name

                        SideEffect {
                                android.util.Log.d(
                                        "MetadataDebug",
                                        "UI: station=${station.name}, title='$currentTitle', show=$showTitle"
                                )
                        }

                        AnimatedVisibility(
                                visible = showTitle,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                        ) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Surface(
                                        color =
                                                MaterialTheme.colorScheme.secondaryContainer.copy(
                                                        alpha = 0.5f
                                                ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                ) {
                                        Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Default.MusicNote,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                        text = currentTitle ?: "",
                                                        style =
                                                                MaterialTheme.typography.bodyMedium
                                                                        .copy(
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Medium
                                                                        ),
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSecondaryContainer,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                )
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Buffering indicator
                        if (isBuffering) {
                                CircularProgressIndicator(
                                        modifier = Modifier.size(48.dp),
                                        color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                        text = stringResource(R.string.connecting),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                        }

                        // Playback Controls with labels
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                // Previous
                                Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(100.dp)
                                ) {
                                        FilledTonalIconButton(
                                                onClick = onPrevious,
                                                modifier = Modifier.size(56.dp)
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Default.SkipPrevious,
                                                        contentDescription =
                                                                stringResource(
                                                                        R.string.previous_station
                                                                ),
                                                        modifier = Modifier.size(32.dp)
                                                )
                                        }
                                        if (previousStationName != null) {
                                                Text(
                                                        text = previousStationName,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onBackground.copy(
                                                                        alpha = 0.5f
                                                                ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(top = 4.dp)
                                                )
                                        }
                                }

                                // Play/Pause
                                FilledIconButton(
                                        onClick = onPlayPause,
                                        modifier = Modifier.size(88.dp),
                                        shape = CircleShape,
                                        colors =
                                                IconButtonDefaults.filledIconButtonColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme.primary
                                                )
                                ) {
                                        Icon(
                                                imageVector =
                                                        if (isPlaying) Icons.Default.Pause
                                                        else Icons.Default.PlayArrow,
                                                contentDescription =
                                                        if (isPlaying)
                                                                stringResource(R.string.pause)
                                                        else stringResource(R.string.play),
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                }

                                // Next
                                Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(100.dp)
                                ) {
                                        FilledTonalIconButton(
                                                onClick = onNext,
                                                modifier = Modifier.size(56.dp)
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Default.SkipNext,
                                                        contentDescription =
                                                                stringResource(
                                                                        R.string.next_station
                                                                ),
                                                        modifier = Modifier.size(32.dp)
                                                )
                                        }
                                        if (nextStationName != null) {
                                                Text(
                                                        text = nextStationName,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onBackground.copy(
                                                                        alpha = 0.5f
                                                                ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(top = 4.dp)
                                                )
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(48.dp))
                } else {
                        // No station selected
                        Spacer(modifier = Modifier.weight(1f))

                        Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = null,
                                modifier = Modifier.size(120.dp),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                                text = stringResource(R.string.no_station_selected),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(onClick = onBackToList) {
                                Text(stringResource(R.string.browse_stations))
                        }

                        Spacer(modifier = Modifier.weight(1f))
                }
        }
}
