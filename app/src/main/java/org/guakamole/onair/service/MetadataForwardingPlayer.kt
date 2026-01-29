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
    fun setOverriddenMetadata(metadata: MediaMetadata?) {
        overriddenMetadata = metadata
        if (metadata != null) {
            listeners.forEach { it.onMediaMetadataChanged(metadata) }
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
