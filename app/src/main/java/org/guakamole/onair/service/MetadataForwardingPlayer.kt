package org.guakamole.onair.service

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import java.util.concurrent.CopyOnWriteArraySet

/**
 * A [ForwardingPlayer] that allows for seamless metadata updates without interrupting playback.
 * This is particularly useful for radio streams where metadata is fetched out-of-band (e.g., via
 * polling). When overridden metadata is set, raw stream metadata events are suppressed.
 */
class MetadataForwardingPlayer(private val wrappedPlayer: Player) :
        ForwardingPlayer(wrappedPlayer) {
    private val externalListeners = CopyOnWriteArraySet<Player.Listener>()
    private var overriddenMetadata: MediaMetadata? = null

    // Internal listener that intercepts raw metadata events
    private val internalListener =
            object : Player.Listener {
                override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                    // If we have overridden metadata, suppress raw stream metadata events
                    // The overridden metadata was already pushed via setOverriddenMetadata
                    if (overriddenMetadata != null) {
                        android.util.Log.d(
                                "MetadataDebug",
                                "ForwardingPlayer: Suppressing raw metadata, using override"
                        )
                        return
                    }
                    // Forward raw metadata if no override is set
                    externalListeners.forEach { it.onMediaMetadataChanged(mediaMetadata) }
                }
            }

    init {
        wrappedPlayer.addListener(internalListener)
    }

    override fun addListener(listener: Player.Listener) {
        externalListeners.add(listener)
        // Add a wrapper that forwards all events EXCEPT onMediaMetadataChanged
        // (which is handled by our internal listener)
        super.addListener(
                object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) {
                        listener.onEvents(player, events)
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        listener.onPlaybackStateChanged(playbackState)
                    }
                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        listener.onPlayWhenReadyChanged(playWhenReady, reason)
                    }
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        listener.onIsPlayingChanged(isPlaying)
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        listener.onPlayerError(error)
                    }
                    override fun onMediaItemTransition(
                            mediaItem: androidx.media3.common.MediaItem?,
                            reason: Int
                    ) {
                        listener.onMediaItemTransition(mediaItem, reason)
                    }
                    override fun onPositionDiscontinuity(
                            oldPosition: Player.PositionInfo,
                            newPosition: Player.PositionInfo,
                            reason: Int
                    ) {
                        listener.onPositionDiscontinuity(oldPosition, newPosition, reason)
                    }
                    override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
                        listener.onAvailableCommandsChanged(availableCommands)
                    }
                    // onMediaMetadataChanged is intentionally omitted - handled by internalListener
                }
        )
    }

    override fun removeListener(listener: Player.Listener) {
        externalListeners.remove(listener)
        // Note: we don't remove the internal wrapper from super - this matches common patterns
    }

    /** Updates the metadata returned by this player and notifies all listeners. */
    fun setOverriddenMetadata(metadata: MediaMetadata?) {
        overriddenMetadata = metadata
        if (metadata != null) {
            externalListeners.forEach { it.onMediaMetadataChanged(metadata) }
        }
    }

    override fun getMediaMetadata(): MediaMetadata {
        return overriddenMetadata ?: super.getMediaMetadata()
    }

    override fun getCurrentMediaItem(): androidx.media3.common.MediaItem? {
        val item = super.getCurrentMediaItem() ?: return null
        val metadata = overriddenMetadata ?: return item
        return item.buildUpon().setMediaMetadata(metadata).build()
    }

    override fun setMediaItem(mediaItem: androidx.media3.common.MediaItem) {
        overriddenMetadata = null
        super.setMediaItem(mediaItem)
    }

    override fun setMediaItem(mediaItem: androidx.media3.common.MediaItem, startPositionMs: Long) {
        overriddenMetadata = null
        super.setMediaItem(mediaItem, startPositionMs)
    }

    override fun setMediaItem(mediaItem: androidx.media3.common.MediaItem, resetPosition: Boolean) {
        overriddenMetadata = null
        super.setMediaItem(mediaItem, resetPosition)
    }

    override fun setMediaItems(mediaItems: List<androidx.media3.common.MediaItem>) {
        overriddenMetadata = null
        super.setMediaItems(mediaItems)
    }

    override fun setMediaItems(
            mediaItems: List<androidx.media3.common.MediaItem>,
            resetPosition: Boolean
    ) {
        overriddenMetadata = null
        super.setMediaItems(mediaItems, resetPosition)
    }

    override fun setMediaItems(
            mediaItems: List<androidx.media3.common.MediaItem>,
            startIndex: Int,
            startPositionMs: Long
    ) {
        overriddenMetadata = null
        super.setMediaItems(mediaItems, startIndex, startPositionMs)
    }

    override fun isCurrentMediaItemSeekable(): Boolean {
        return false
    }

    override fun getDuration(): Long {
        return androidx.media3.common.C.TIME_UNSET
    }

    override fun isCurrentMediaItemLive(): Boolean {
        return true
    }

    override fun hasNextMediaItem(): Boolean {
        return true
    }

    override fun hasPreviousMediaItem(): Boolean {
        return true
    }

    override fun seekToNext() {
        skipToStation(1)
    }

    override fun seekToPrevious() {
        skipToStation(-1)
    }

    private fun skipToStation(direction: Int) {
        val currentId = currentMediaItem?.mediaId ?: return
        val stations = org.guakamole.onair.data.RadioRepository.stations
        if (stations.isEmpty()) return

        val currentIndex = stations.indexOfFirst { it.id == currentId }
        // If current not found, default to 0
        val index = if (currentIndex == -1) 0 else currentIndex

        // Calculate new index wrapping around
        val newIndex = (index + direction).mod(stations.size)
        val newStation = stations[newIndex]

        val newItem =
                androidx.media3.common.MediaItem.Builder()
                        .setMediaId(newStation.id)
                        .setUri(newStation.streamUrl)
                        .setMediaMetadata(
                                MediaMetadata.Builder()
                                        .setTitle(newStation.name)
                                        .setArtist(newStation.name)
                                        .setArtworkUri(
                                                if (newStation.logoUrl != null)
                                                        android.net.Uri.parse(newStation.logoUrl)
                                                else null
                                        )
                                        .build()
                        )
                        .build()

        setMediaItem(newItem)
        prepare()
        play()
    }
}
