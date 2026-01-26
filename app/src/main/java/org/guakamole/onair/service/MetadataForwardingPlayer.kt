package org.guakamole.onair.service

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import java.util.concurrent.CopyOnWriteArraySet

/**
 * A [ForwardingPlayer] that allows for seamless metadata updates without interrupting playback.
 * This is particularly useful for radio streams where metadata is fetched out-of-band (e.g., via
 * polling).
 */
class MetadataForwardingPlayer(player: Player) : ForwardingPlayer(player) {
    private val listeners = CopyOnWriteArraySet<Player.Listener>()
    private var overriddenMetadata: MediaMetadata? = null

    override fun addListener(listener: Player.Listener) {
        super.addListener(listener)
        listeners.add(listener)
    }

    override fun removeListener(listener: Player.Listener) {
        super.removeListener(listener)
        listeners.remove(listener)
    }

    /** Updates the metadata returned by this player and notifies all listeners. */
    fun setOverriddenMetadata(metadata: MediaMetadata) {
        overriddenMetadata = metadata
        listeners.forEach { it.onMediaMetadataChanged(metadata) }
    }

    override fun getMediaMetadata(): MediaMetadata {
        return overriddenMetadata ?: super.getMediaMetadata()
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
}
