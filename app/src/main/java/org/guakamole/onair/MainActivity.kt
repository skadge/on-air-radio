package org.guakamole.onair

import android.content.ComponentName
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import org.guakamole.onair.data.RadioRepository
import org.guakamole.onair.data.RadioStation
import org.guakamole.onair.service.PlaybackError
import org.guakamole.onair.service.RadioPlaybackService
import org.guakamole.onair.ui.RadioApp
import org.guakamole.onair.ui.theme.RadioTheme

class MainActivity : ComponentActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() - Manual implementation for SDK 33 compatibility if needed
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            var currentStationId by remember { mutableStateOf<String?>(null) }
            var isPlaying by remember { mutableStateOf(false) }
            var isBuffering by remember { mutableStateOf(false) }
            var currentTitle by remember { mutableStateOf<String?>(null) }
            var currentArtist by remember { mutableStateOf<String?>(null) }
            var currentPlaybackError by remember { mutableStateOf<PlaybackError?>(null) }

            // Setup player listener
            DisposableEffect(Unit) {
                val listener =
                        object : Player.Listener {
                            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                                currentStationId = mediaItem?.mediaId
                            }

                            override fun onIsPlayingChanged(playing: Boolean) {
                                isPlaying = playing
                            }

                            override fun onPlaybackStateChanged(playbackState: Int) {
                                isBuffering = playbackState == Player.STATE_BUFFERING
                            }

                            override fun onMediaMetadataChanged(metadata: MediaMetadata) {
                                // Stream metadata (song title) from ICY headers
                                currentTitle = metadata.title?.toString()
                                currentArtist = metadata.artist?.toString()
                                android.util.Log.d(
                                        "MetadataDebug",
                                        "Main: Metadata received. Title: $currentTitle"
                                )
                            }
                        }

                mediaController?.addListener(listener)

                onDispose { mediaController?.removeListener(listener) }
            }

            // Update state when controller connects
            LaunchedEffect(mediaController) {
                mediaController?.let { controller ->
                    currentStationId = controller.currentMediaItem?.mediaId
                    isPlaying = controller.isPlaying
                    isBuffering = controller.playbackState == Player.STATE_BUFFERING
                    currentTitle = controller.currentMediaItem?.mediaMetadata?.title?.toString()
                    currentArtist = controller.currentMediaItem?.mediaMetadata?.artist?.toString()
                    // Initial error check not easily possible from controller without custom
                    // session commands,
                    // but onPlayerError in Service will update the StateFlow which we should
                    // collect.
                }
            }

            RadioTheme {
                RadioApp(
                        currentStationId = currentStationId,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        currentTitle = currentTitle,
                        currentArtist = currentArtist,
                        playbackError = currentPlaybackError,
                        onStationSelect = { station -> playStation(station) },
                        onPlayPause = {
                            mediaController?.let { controller ->
                                if (controller.isPlaying) {
                                    controller.pause()
                                } else {
                                    controller.play()
                                }
                            }
                        },
                        onStop = { mediaController?.stop() },
                        onPrevious = {
                            val currentIndex =
                                    currentStationId?.let { RadioRepository.getStationIndex(it) }
                                            ?: 0
                            val previousIndex =
                                    if (currentIndex > 0) {
                                        currentIndex - 1
                                    } else {
                                        RadioRepository.stations.size - 1
                                    }
                            RadioRepository.getStationByIndex(previousIndex)?.let { station ->
                                playStation(station, isPlaying)
                            }
                        },
                        onNext = {
                            val currentIndex =
                                    currentStationId?.let { RadioRepository.getStationIndex(it) }
                                            ?: -1
                            val nextIndex = (currentIndex + 1) % RadioRepository.stations.size
                            RadioRepository.getStationByIndex(nextIndex)?.let { station ->
                                playStation(station, isPlaying)
                            }
                        }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        initializeController()
    }

    override fun onStop() {
        releaseController()
        super.onStop()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(this, ComponentName(this, RadioPlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener(
                {
                    mediaController = controllerFuture?.get()
                    // Force recomposition with updated controller
                    setContent {
                        var currentStationId by remember {
                            mutableStateOf(mediaController?.currentMediaItem?.mediaId)
                        }
                        var isPlaying by remember {
                            mutableStateOf(mediaController?.isPlaying ?: false)
                        }
                        var isBuffering by remember {
                            mutableStateOf(mediaController?.playbackState == Player.STATE_BUFFERING)
                        }
                        var currentTitle by remember {
                            mutableStateOf(
                                    mediaController?.currentMediaItem?.mediaMetadata?.title
                                            ?.toString()
                            )
                        }
                        var currentArtist by remember {
                            mutableStateOf(
                                    mediaController?.currentMediaItem?.mediaMetadata?.artist
                                            ?.toString()
                            )
                        }

                        DisposableEffect(mediaController) {
                            val listener =
                                    object : Player.Listener {
                                        override fun onMediaItemTransition(
                                                mediaItem: MediaItem?,
                                                reason: Int
                                        ) {
                                            currentStationId = mediaItem?.mediaId
                                        }

                                        override fun onIsPlayingChanged(playing: Boolean) {
                                            isPlaying = playing
                                        }

                                        override fun onPlaybackStateChanged(playbackState: Int) {
                                            isBuffering = playbackState == Player.STATE_BUFFERING
                                        }

                                        override fun onMediaMetadataChanged(
                                                metadata: MediaMetadata
                                        ) {
                                            currentTitle = metadata.title?.toString()
                                            currentArtist = metadata.artist?.toString()
                                        }
                                    }

                            mediaController?.addListener(listener)

                            onDispose { mediaController?.removeListener(listener) }
                        }

                        RadioTheme {
                            RadioApp(
                                    currentStationId = currentStationId,
                                    isPlaying = isPlaying,
                                    isBuffering = isBuffering,
                                    currentTitle = currentTitle,
                                    currentArtist = currentArtist,
                                    onStationSelect = { station -> playStation(station) },
                                    onPlayPause = {
                                        mediaController?.let { controller ->
                                            if (controller.isPlaying) {
                                                controller.pause()
                                            } else {
                                                controller.play()
                                            }
                                        }
                                    },
                                    onStop = { mediaController?.stop() },
                                    onPrevious = {
                                        val currentIndex =
                                                currentStationId?.let {
                                                    RadioRepository.getStationIndex(it)
                                                }
                                                        ?: 0
                                        val previousIndex =
                                                if (currentIndex > 0) {
                                                    currentIndex - 1
                                                } else {
                                                    RadioRepository.stations.size - 1
                                                }
                                        RadioRepository.getStationByIndex(previousIndex)?.let {
                                                station ->
                                            playStation(station, isPlaying)
                                        }
                                    },
                                    onNext = {
                                        val currentIndex =
                                                currentStationId?.let {
                                                    RadioRepository.getStationIndex(it)
                                                }
                                                        ?: -1
                                        val nextIndex =
                                                (currentIndex + 1) % RadioRepository.stations.size
                                        RadioRepository.getStationByIndex(nextIndex)?.let { station
                                            ->
                                            playStation(station, isPlaying)
                                        }
                                    }
                            )
                        }
                    }
                },
                MoreExecutors.directExecutor()
        )
    }

    private fun releaseController() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
    }

    private fun playStation(station: RadioStation, autoPlay: Boolean = true) {
        mediaController?.let { controller ->
            val mediaItem =
                    MediaItem.Builder()
                            .setMediaId(station.id)
                            .setUri(station.streamUrl)
                            .setMediaMetadata(
                                    MediaMetadata.Builder()
                                            .setTitle(station.name)
                                            .setArtworkUri(Uri.parse(station.logoUrl))
                                            .build()
                            )
                            .build()

            controller.setMediaItem(mediaItem)
            controller.prepare()
            if (autoPlay) {
                controller.play()
            }
        }
    }
}
