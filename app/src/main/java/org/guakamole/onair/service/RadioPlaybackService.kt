package org.guakamole.onair.service

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.guakamole.onair.BuildConfig
import org.guakamole.onair.MainActivity
import org.guakamole.onair.billing.PremiumManager
import org.guakamole.onair.data.RadioRepository
import org.guakamole.onair.data.RadioStation
import org.guakamole.onair.metadata.MetadataType
import org.guakamole.onair.service.metadata.ArtworkManager
import org.guakamole.onair.service.metadata.MetadataPoller
import org.guakamole.onair.service.metadata.RadioMetadataManager

data class PlaybackError(
        val errorCode: Int,
        val message: String,
        val stationId: String?,
        val streamUrl: String?,
        val userAgent: String? = null
)

/** Media playback service that supports both in-app playback and Android Auto */
@OptIn(UnstableApi::class)
class RadioPlaybackService : MediaLibraryService() {

        private var player: MetadataForwardingPlayer? = null
        private var basePlayer: ExoPlayer? = null
        private var mediaLibrarySession: MediaLibrarySession? = null
        private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        private lateinit var metadataManager: RadioMetadataManager
        private lateinit var metadataPoller: MetadataPoller
        private lateinit var artworkManager: ArtworkManager

        private val _playbackError = MutableStateFlow<PlaybackError?>(null)
        val playbackError = _playbackError.asStateFlow()

        // Session tracking for listen counts
        private var currentStationId: String? = null
        private var playbackStartTimeMs: Long = 0
        private val LISTEN_SESSION_THRESHOLD_MS = 10_000L // 10 seconds

        private val userAgent by lazy {
                "OnAir Radio/${org.guakamole.onair.BuildConfig.VERSION_NAME} (Android ${android.os.Build.VERSION.RELEASE}; ${android.os.Build.MODEL})"
        }

        companion object {
                private const val ROOT_ID = "root"
                private const val STATIONS_ID = "stations"
                private const val FAVORITES_ID = "favorites"
                private const val MOST_LISTENED_ID = "most_listened"
                private const val GENRES_ID = "genres"
                private const val GENRE_PREFIX = "genre_"
                private const val PREMIUM_REQUIRED_ID = "premium_required"
                private const val ANDROID_AUTO_PACKAGE = "com.google.android.projection.gearhead"

                // Genre tags for filtering (string res IDs resolved at runtime)
                val AUTO_GENRE_TAGS = listOf("news", "hits", "rock", "jazz", "classical")

                const val EXT_CONTENT_TYPE = "org.guakamole.onair.CONTENT_TYPE"
                const val EXT_IS_SONG_ARTWORK = "org.guakamole.onair.IS_SONG_ARTWORK"

                fun getIsSongArtwork(metadata: MediaMetadata): Boolean {
                        return metadata.extras?.getBoolean(EXT_IS_SONG_ARTWORK) ?: false
                }

                fun getContentType(metadata: MediaMetadata): MetadataType {
                        val typeStr = metadata.extras?.getString(EXT_CONTENT_TYPE)
                        return try {
                                MetadataType.valueOf(typeStr ?: MetadataType.UNKNOWN.name)
                        } catch (e: Exception) {
                                MetadataType.UNKNOWN
                        }
                }
        }

        override fun onCreate() {
                super.onCreate()

                artworkManager = ArtworkManager(this)
                metadataManager = RadioMetadataManager(serviceScope, artworkManager)
                metadataPoller = MetadataPoller(serviceScope)

                initializePlayer()
                initializeSession()

                // Observe metadata updates from the manager
                serviceScope.launch {
                        metadataManager.metadataUpdates.collect { metadata ->
                                android.util.Log.d(
                                        "MetadataDebug",
                                        "Service: Received metadata from manager: title=${metadata?.title}, artist=${metadata?.artist}"
                                )
                                if (metadata != null) {
                                        player?.setOverriddenMetadata(metadata)
                                }
                        }
                }

                // Observe polled updates and feed them to the manager
                serviceScope.launch {
                        metadataPoller.metadataUpdates.collect { result ->
                                metadataManager.onPolledMetadata(result)
                        }
                }
        }

        private fun initializePlayer() {
                // Configure HttpDataSource with User-Agent and ICY metadata request
                val httpDataSourceFactory =
                        DefaultHttpDataSource.Factory()
                                .setUserAgent(userAgent)
                                .setAllowCrossProtocolRedirects(true)
                                .setDefaultRequestProperties(mapOf("Icy-MetaData" to "1"))

                val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)
                val mediaSourceFactory =
                        DefaultMediaSourceFactory(dataSourceFactory)
                                .setLoadErrorHandlingPolicy(RadioLoadErrorHandlingPolicy())

                val exoPlayer =
                        ExoPlayer.Builder(this)
                                .setMediaSourceFactory(mediaSourceFactory)
                                .setAudioAttributes(
                                        AudioAttributes.Builder()
                                                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                                                .setUsage(C.USAGE_MEDIA)
                                                .build(),
                                        true // Handle audio focus
                                )
                                .setHandleAudioBecomingNoisy(true)
                                .build()

                basePlayer = exoPlayer
                val forwardingPlayer = MetadataForwardingPlayer(exoPlayer)

                // Removed: exoPlayer.addAnalyticsListener(EventLogger())

                exoPlayer.addListener(
                        object : Player.Listener {
                                override fun onMediaItemTransition(
                                        mediaItem: MediaItem?,
                                        reason: Int
                                ) {
                                        // Check if previous station played long enough to count
                                        val previousStationId = currentStationId
                                        if (previousStationId != null && playbackStartTimeMs > 0) {
                                                val listenDurationMs =
                                                        System.currentTimeMillis() -
                                                                playbackStartTimeMs
                                                if (listenDurationMs >= LISTEN_SESSION_THRESHOLD_MS
                                                ) {
                                                        RadioRepository.incrementListenCount(
                                                                previousStationId
                                                        )
                                                        android.util.Log.d(
                                                                "ListenTracking",
                                                                "Session counted for $previousStationId (${listenDurationMs}ms)"
                                                        )
                                                }
                                        }

                                        // Start tracking new station
                                        currentStationId = mediaItem?.mediaId
                                        playbackStartTimeMs = System.currentTimeMillis()

                                        player?.setOverriddenMetadata(null)
                                        metadataManager.onStationChange(mediaItem?.mediaId)
                                        metadataPoller.startPolling(mediaItem?.mediaId)
                                        _playbackError.value = null // Reset error on transition
                                }

                                override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                                        android.util.Log.d(
                                                "MetadataDebug",
                                                "Service: onMediaMetadataChanged: title=${mediaMetadata.title}, artist=${mediaMetadata.artist}"
                                        )
                                        metadataManager.onRawMetadata(
                                                mediaMetadata.title?.toString(),
                                                mediaMetadata.artist?.toString()
                                        )
                                }

                                override fun onMetadata(metadata: androidx.media3.common.Metadata) {
                                        for (i in 0 until metadata.length()) {
                                                val entry = metadata.get(i)
                                                if (entry is
                                                                androidx.media3.extractor.metadata.icy.IcyInfo
                                                ) {
                                                        android.util.Log.d(
                                                                "MetadataDebug",
                                                                "Service: Received IcyInfo: title=${entry.title}, url=${entry.url}"
                                                        )
                                                        metadataManager.onRawMetadata(
                                                                entry.title,
                                                                null // Artist usually in title
                                                        )
                                                }
                                                // Logging remains for debugging
                                                android.util.Log.d(
                                                        "MetadataDebug",
                                                        "Service: Received metadata entry: type=${entry::class.java.simpleName}, content=$entry"
                                                )
                                        }
                                }

                                override fun onPlayerError(
                                        error: androidx.media3.common.PlaybackException
                                ) {
                                        android.util.Log.e(
                                                "RadioPlaybackService",
                                                "Player error: ${error.message}",
                                                error
                                        )
                                        _playbackError.value =
                                                PlaybackError(
                                                        errorCode = error.errorCode,
                                                        message = error.message ?: "Unknown error",
                                                        stationId =
                                                                player?.currentMediaItem?.mediaId,
                                                        streamUrl =
                                                                player?.currentMediaItem
                                                                        ?.localConfiguration?.uri
                                                                        ?.toString(),
                                                        userAgent = userAgent
                                                )
                                }
                        }
                )

                player = forwardingPlayer
        }

        override fun onDestroy() {
                // Count session for current station before destroying
                if (currentStationId != null && playbackStartTimeMs > 0) {
                        val listenDurationMs = System.currentTimeMillis() - playbackStartTimeMs
                        if (listenDurationMs >= LISTEN_SESSION_THRESHOLD_MS) {
                                RadioRepository.incrementListenCount(currentStationId!!)
                                android.util.Log.d(
                                        "ListenTracking",
                                        "Session counted on destroy for $currentStationId (${listenDurationMs}ms)"
                                )
                        }
                }

                metadataPoller.stopPolling()
                serviceScope.cancel()
                mediaLibrarySession?.run {
                        basePlayer?.release()
                        release()
                        mediaLibrarySession = null
                }
                super.onDestroy()
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

        override fun onGetSession(
                controllerInfo: MediaSession.ControllerInfo
        ): MediaLibrarySession? {
                return mediaLibrarySession
        }

        /** Callback handling media library browsing for Android Auto and other clients */
        private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

                override fun onConnect(
                        session: MediaSession,
                        controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                        val connectionResult = super.onConnect(session, controller)
                        val availableSessionCommands =
                                connectionResult.availableSessionCommands.buildUpon()

                        // Expose next/previous commands for Android Auto station navigation
                        val availablePlayerCommands =
                                connectionResult
                                        .availablePlayerCommands
                                        .buildUpon()
                                        .add(Player.COMMAND_SEEK_TO_NEXT)
                                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                                        .build()

                        return MediaSession.ConnectionResult.accept(
                                availableSessionCommands.build(),
                                availablePlayerCommands
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
                                                        .setMediaType(
                                                                MediaMetadata
                                                                        .MEDIA_TYPE_FOLDER_MIXED
                                                        )
                                                        .setTitle("Radio")
                                                        .build()
                                        )
                                        .build()
                        return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
                }

                /** Check if the connecting client is Android Auto. */
                private fun isAndroidAuto(controller: MediaSession.ControllerInfo): Boolean {
                        return controller.packageName == ANDROID_AUTO_PACKAGE
                }

                override fun onGetChildren(
                        session: MediaLibrarySession,
                        browser: MediaSession.ControllerInfo,
                        parentId: String,
                        page: Int,
                        pageSize: Int,
                        params: LibraryParams?
                ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                        // Gate Android Auto access for non-premium users
                        val isPremium =
                                PremiumManager.getInstance(this@RadioPlaybackService)
                                        .isPremium
                                        .value
                        val isAutoClient = isAndroidAuto(browser)

                        if (isAutoClient && !isPremium) {
                                // Return a "Premium Required" placeholder for Android Auto
                                val premiumItem =
                                        MediaItem.Builder()
                                                .setMediaId(PREMIUM_REQUIRED_ID)
                                                .setMediaMetadata(
                                                        MediaMetadata.Builder()
                                                                .setIsBrowsable(false)
                                                                .setIsPlayable(false)
                                                                .setMediaType(
                                                                        MediaMetadata
                                                                                .MEDIA_TYPE_MIXED
                                                                )
                                                                .setTitle(
                                                                        getString(
                                                                                org.guakamole
                                                                                        .onair
                                                                                        .R
                                                                                        .string
                                                                                        .premium_required
                                                                        )
                                                                )
                                                                .setSubtitle(
                                                                        getString(
                                                                                org.guakamole
                                                                                        .onair
                                                                                        .R
                                                                                        .string
                                                                                        .premium_required_desc
                                                                        )
                                                                )
                                                                .build()
                                                )
                                                .build()
                                return Futures.immediateFuture(
                                        LibraryResult.ofItemList(
                                                ImmutableList.of(premiumItem),
                                                params
                                        )
                                )
                        }

                        val children =
                                when (parentId) {
                                        ROOT_ID -> {
                                                // Return main folders
                                                listOf(
                                                        createFolderItem(
                                                                STATIONS_ID,
                                                                getString(
                                                                        org.guakamole
                                                                                .onair
                                                                                .R
                                                                                .string
                                                                                .all_stations
                                                                ),
                                                                MediaMetadata
                                                                        .MEDIA_TYPE_FOLDER_RADIO_STATIONS
                                                        ),
                                                        createFolderItem(
                                                                FAVORITES_ID,
                                                                getString(
                                                                        org.guakamole
                                                                                .onair
                                                                                .R
                                                                                .string
                                                                                .favorites
                                                                ),
                                                                MediaMetadata
                                                                        .MEDIA_TYPE_FOLDER_RADIO_STATIONS
                                                        ),
                                                        createFolderItem(
                                                                MOST_LISTENED_ID,
                                                                getString(
                                                                        org.guakamole
                                                                                .onair
                                                                                .R
                                                                                .string
                                                                                .most_listened
                                                                ),
                                                                MediaMetadata
                                                                        .MEDIA_TYPE_FOLDER_RADIO_STATIONS
                                                        ),
                                                        createFolderItem(
                                                                GENRES_ID,
                                                                getString(
                                                                        org.guakamole
                                                                                .onair
                                                                                .R
                                                                                .string
                                                                                .by_genre
                                                                ),
                                                                MediaMetadata
                                                                        .MEDIA_TYPE_FOLDER_GENRES
                                                        )
                                                )
                                        }
                                        STATIONS_ID -> {
                                                // Return all radio stations with smart ordering
                                                RadioRepository.getStationsForAndroidAuto().map {
                                                        station ->
                                                        createMediaItem(station)
                                                }
                                        }
                                        FAVORITES_ID -> {
                                                // Return favorite stations
                                                RadioRepository.getFavorites().map { station ->
                                                        createMediaItem(station)
                                                }
                                        }
                                        MOST_LISTENED_ID -> {
                                                // Return most listened stations
                                                RadioRepository.getMostListened(10).map { station ->
                                                        createMediaItem(station)
                                                }
                                        }
                                        GENRES_ID -> {
                                                // Return genre folders
                                                AUTO_GENRE_TAGS.map { tag ->
                                                        createFolderItem(
                                                                GENRE_PREFIX + tag,
                                                                getGenreDisplayName(tag),
                                                                MediaMetadata
                                                                        .MEDIA_TYPE_FOLDER_RADIO_STATIONS
                                                        )
                                                }
                                        }
                                        else -> {
                                                // Check if it's a genre folder
                                                if (parentId.startsWith(GENRE_PREFIX)) {
                                                        val tag =
                                                                parentId.removePrefix(GENRE_PREFIX)
                                                        RadioRepository.getStationsByGenre(tag)
                                                                .map { station ->
                                                                        createMediaItem(station)
                                                                }
                                                } else {
                                                        emptyList()
                                                }
                                        }
                                }
                        return Futures.immediateFuture(
                                LibraryResult.ofItemList(ImmutableList.copyOf(children), params)
                        )
                }

                private fun createFolderItem(id: String, title: String, mediaType: Int): MediaItem {
                        return MediaItem.Builder()
                                .setMediaId(id)
                                .setMediaMetadata(
                                        MediaMetadata.Builder()
                                                .setIsBrowsable(true)
                                                .setIsPlayable(false)
                                                .setMediaType(mediaType)
                                                .setTitle(title)
                                                .build()
                                )
                                .build()
                }

                private fun getGenreDisplayName(tag: String): String {
                        val resId =
                                when (tag) {
                                        "news" -> org.guakamole.onair.R.string.tag_news
                                        "hits" -> org.guakamole.onair.R.string.tag_hits
                                        "rock" -> org.guakamole.onair.R.string.tag_rock
                                        "jazz" -> org.guakamole.onair.R.string.tag_jazz
                                        "classical" -> org.guakamole.onair.R.string.tag_classical
                                        else -> return tag.replaceFirstChar { it.uppercase() }
                                }
                        return getString(resId)
                }

                override fun onGetItem(
                        session: MediaLibrarySession,
                        browser: MediaSession.ControllerInfo,
                        mediaId: String
                ): ListenableFuture<LibraryResult<MediaItem>> {
                        val station = RadioRepository.getStationById(mediaId)
                        return if (station != null) {
                                Futures.immediateFuture(
                                        LibraryResult.ofItem(createMediaItem(station), null)
                                )
                        } else {
                                Futures.immediateFuture(
                                        LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                                )
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
                                                val station =
                                                        RadioRepository.getStationById(item.mediaId)
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
                                                val station =
                                                        RadioRepository.getStationById(item.mediaId)
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
                                        .setArtist(station.name)
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
