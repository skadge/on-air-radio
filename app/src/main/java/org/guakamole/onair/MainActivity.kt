package org.guakamole.onair

import android.content.ComponentName
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import org.guakamole.onair.metadata.MetadataType
import org.guakamole.onair.service.PlaybackError
import org.guakamole.onair.service.RadioPlaybackService
import org.guakamole.onair.ui.RadioApp
import org.guakamole.onair.ui.theme.RadioTheme

class MainActivity : ComponentActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController by mutableStateOf<MediaController?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        setContent {
            var currentStationId by remember { mutableStateOf<String?>(null) }
            var isPlaying by remember { mutableStateOf(false) }
            var isBuffering by remember { mutableStateOf(false) }
            var currentTitle by remember { mutableStateOf<String?>(null) }
            var currentArtist by remember { mutableStateOf<String?>(null) }
            var currentContentType by remember { mutableStateOf(MetadataType.UNKNOWN) }
            var currentArtworkData by remember { mutableStateOf<ByteArray?>(null) }
            var isSongArtwork by remember { mutableStateOf(false) }
            var currentPlaybackError by remember { mutableStateOf<PlaybackError?>(null) }

            DisposableEffect(mediaController) {
                val controller = mediaController
                if (controller == null) {
                    return@DisposableEffect onDispose {}
                }

                // Sync initial state
                currentStationId = controller.currentMediaItem?.mediaId
                isPlaying = controller.isPlaying
                isBuffering = controller.playbackState == Player.STATE_BUFFERING
                currentTitle = controller.currentMediaItem?.mediaMetadata?.title?.toString()
                currentArtist = controller.currentMediaItem?.mediaMetadata?.artist?.toString()
                currentContentType =
                        controller.currentMediaItem?.mediaMetadata?.let {
                            RadioPlaybackService.getContentType(it)
                        }
                                ?: MetadataType.UNKNOWN
                currentArtworkData = controller.currentMediaItem?.mediaMetadata?.artworkData
                isSongArtwork =
                        controller.currentMediaItem?.mediaMetadata?.let {
                            RadioPlaybackService.getIsSongArtwork(it)
                        }
                                ?: false
                android.util.Log.d(
                        "MetadataDebug",
                        "Main: Initial metadata: title=$currentTitle, artist=$currentArtist, artworkDataSize=${currentArtworkData?.size ?: 0}"
                )

                val listener =
                        object : Player.Listener {
                            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                                currentStationId = mediaItem?.mediaId
                                currentPlaybackError = null
                                currentContentType = MetadataType.UNKNOWN
                                currentArtworkData = null
                                isSongArtwork = false
                                android.util.Log.d(
                                        "MetadataDebug",
                                        "Main: Transition to ${mediaItem?.mediaId}"
                                )
                            }

                            override fun onIsPlayingChanged(playing: Boolean) {
                                isPlaying = playing
                            }

                            override fun onPlaybackStateChanged(playbackState: Int) {
                                isBuffering = playbackState == Player.STATE_BUFFERING
                            }

                            override fun onMediaMetadataChanged(metadata: MediaMetadata) {
                                currentTitle = metadata.title?.toString()
                                currentArtist = metadata.artist?.toString()
                                currentContentType = RadioPlaybackService.getContentType(metadata)
                                currentArtworkData = metadata.artworkData
                                isSongArtwork = RadioPlaybackService.getIsSongArtwork(metadata)
                                android.util.Log.d(
                                        "MetadataDebug",
                                        "Main: Metadata changed: title=$currentTitle, artist=$currentArtist, artworkDataSize=${currentArtworkData?.size ?: 0}"
                                )
                            }

                            override fun onPlayerError(
                                    error: androidx.media3.common.PlaybackException
                            ) {
                                android.util.Log.e(
                                        "MainActivity",
                                        "Player error received: ${error.message}",
                                        error
                                )
                                currentPlaybackError =
                                        PlaybackError(
                                                errorCode = error.errorCode,
                                                message = error.message ?: "Unknown error",
                                                stationId = controller.currentMediaItem?.mediaId,
                                                streamUrl =
                                                        controller.currentMediaItem
                                                                ?.localConfiguration?.uri
                                                                ?.toString(),
                                                userAgent =
                                                        "OnAir Radio/${BuildConfig.VERSION_NAME}"
                                        )
                            }
                        }

                controller.addListener(listener)
                onDispose { controller.removeListener(listener) }
            }

            RadioTheme {
                RadioApp(
                        currentStationId = currentStationId,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        currentTitle = currentTitle,
                        currentArtist = currentArtist,
                        currentContentType = currentContentType,
                        currentArtworkData = currentArtworkData,
                        playbackError = currentPlaybackError,
                        onStationSelect = { station -> playStation(station) },
                        onPlayPause = {
                            mediaController?.let { controller ->
                                if (controller.isPlaying) controller.pause() else controller.play()
                            }
                        },
                        onStop = { mediaController?.stop() },
                        onPrevious = { switchStation(relativeIndex = -1, currentStationId) },
                        onNext = { switchStation(relativeIndex = 1, currentStationId) }
                )
            }
        }
    }

    private fun switchStation(relativeIndex: Int, currentStationId: String?) {
        val stations = RadioRepository.stations
        if (stations.isEmpty()) return

        val currentIndex =
                currentStationId?.let { id -> stations.indexOfFirst { it.id == id } } ?: -1
        val nextIndex =
                if (currentIndex == -1) 0
                else (currentIndex + relativeIndex + stations.size) % stations.size

        RadioRepository.getStationByIndex(nextIndex)?.let { station -> playStation(station) }
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
                { mediaController = controllerFuture?.get() },
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
