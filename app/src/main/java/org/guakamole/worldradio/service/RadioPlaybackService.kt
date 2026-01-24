package org.guakamole.worldradio.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import coil.Coil
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.guakamole.worldradio.MainActivity
import org.guakamole.worldradio.data.RadioRepository
import org.guakamole.worldradio.data.RadioStation

/** Media playback service that supports both in-app playback and Android Auto */
@OptIn(UnstableApi::class)
class RadioPlaybackService : MediaLibraryService() {

    private var player: ExoPlayer? = null
    private var mediaLibrarySession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var imageLoader: ImageLoader

    companion object {
        private const val ROOT_ID = "root"
        private const val STATIONS_ID = "stations"
    }

    override fun onCreate() {
        super.onCreate()
        imageLoader = Coil.imageLoader(this)
        initializePlayer()
        initializeSession()
    }

    private fun initializePlayer() {
        val exoPlayer =
                ExoPlayer.Builder(this)
                        .setAudioAttributes(
                                AudioAttributes.Builder()
                                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                                        .setUsage(C.USAGE_MEDIA)
                                        .build(),
                                true // Handle audio focus
                        )
                        .setHandleAudioBecomingNoisy(true)
                        .build()

        exoPlayer.addListener(
                object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        mediaItem?.mediaMetadata?.artworkUri?.let { uri -> loadAndSetArtwork(uri) }
                    }
                }
        )

        player = exoPlayer
    }

    private fun loadAndSetArtwork(uri: Uri) {
        serviceScope.launch {
            val request =
                    ImageRequest.Builder(this@RadioPlaybackService)
                            .data(uri)
                            .size(Size.ORIGINAL)
                            .allowHardware(false)
                            .build()

            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    updateMetadataWithArtwork(bitmap)
                }
            }
        }
    }

    private fun updateMetadataWithArtwork(bitmap: Bitmap) {
        val player = player ?: return
        val currentItem = player.currentMediaItem ?: return

        // Only update if it doesn't have artwork data yet
        if (currentItem.mediaMetadata.artworkData != null) return

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val artworkData = stream.toByteArray()

        val updatedMetadata =
                currentItem
                        .mediaMetadata
                        .buildUpon()
                        .setArtworkData(artworkData, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                        .build()

        val updatedItem = currentItem.buildUpon().setMediaMetadata(updatedMetadata).build()

        player.setMediaItem(updatedItem, /* resetPosition= */ false)
    }

    private fun initializeSession() {
        val sessionActivityPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

        mediaLibrarySession =
                MediaLibrarySession.Builder(this, player!!, LibrarySessionCallback())
                        .setSessionActivity(sessionActivityPendingIntent)
                        .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player?.release()
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }

    /** Callback handling media library browsing for Android Auto and other clients */
    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()

            return MediaSession.ConnectionResult.accept(
                    availableSessionCommands.build(),
                    connectionResult.availablePlayerCommands
            )
        }

        override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem =
                    MediaItem.Builder()
                            .setMediaId(ROOT_ID)
                            .setMediaMetadata(
                                    MediaMetadata.Builder()
                                            .setIsBrowsable(true)
                                            .setIsPlayable(false)
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                            .setTitle("Radio")
                                            .build()
                            )
                            .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val children =
                    when (parentId) {
                        ROOT_ID -> {
                            // Return stations folder
                            listOf(
                                    MediaItem.Builder()
                                            .setMediaId(STATIONS_ID)
                                            .setMediaMetadata(
                                                    MediaMetadata.Builder()
                                                            .setIsBrowsable(true)
                                                            .setIsPlayable(false)
                                                            .setMediaType(
                                                                    MediaMetadata
                                                                            .MEDIA_TYPE_FOLDER_RADIO_STATIONS
                                                            )
                                                            .setTitle("Radio Stations")
                                                            .build()
                                            )
                                            .build()
                            )
                        }
                        STATIONS_ID -> {
                            // Return all radio stations
                            RadioRepository.stations.map { station -> createMediaItem(station) }
                        }
                        else -> emptyList()
                    }
            return Futures.immediateFuture(
                    LibraryResult.ofItemList(ImmutableList.copyOf(children), params)
            )
        }

        override fun onGetItem(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val station = RadioRepository.getStationById(mediaId)
            return if (station != null) {
                Futures.immediateFuture(LibraryResult.ofItem(createMediaItem(station), null))
            } else {
                Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
            }
        }

        override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            // Resolve media items with full URIs
            val resolvedItems =
                    mediaItems
                            .map { item ->
                                val station = RadioRepository.getStationById(item.mediaId)
                                if (station != null) {
                                    createMediaItem(station)
                                } else {
                                    item
                                }
                            }
                            .toMutableList()
            return Futures.immediateFuture(resolvedItems)
        }

        override fun onSetMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>,
                startIndex: Int,
                startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            // Resolve all items
            val resolvedItems =
                    mediaItems
                            .map { item ->
                                val station = RadioRepository.getStationById(item.mediaId)
                                if (station != null) {
                                    createMediaItem(station)
                                } else {
                                    item
                                }
                            }
                            .toMutableList()

            return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(
                            resolvedItems,
                            startIndex,
                            startPositionMs
                    )
            )
        }
    }

    private fun createMediaItem(station: RadioStation): MediaItem {
        return MediaItem.Builder()
                .setMediaId(station.id)
                .setUri(station.streamUrl)
                .setMediaMetadata(
                        MediaMetadata.Builder()
                                .setTitle(station.name)
                                .setSubtitle(station.description)
                                .setArtist(getString(station.genre))
                                .setArtworkUri(
                                        if (station.logoResId != 0) {
                                            Uri.parse(
                                                    "android.resource://${packageName}/${station.logoResId}"
                                            )
                                        } else {
                                            Uri.parse(station.logoUrl)
                                        }
                                )
                                .setIsBrowsable(false)
                                .setIsPlayable(true)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                                .build()
                )
                .build()
    }
}
