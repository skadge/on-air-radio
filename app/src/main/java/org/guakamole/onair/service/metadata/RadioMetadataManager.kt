package org.guakamole.onair.service.metadata

import android.net.Uri
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.guakamole.onair.data.RadioRepository
import org.guakamole.onair.metadata.MetadataResult
import org.guakamole.onair.metadata.MetadataType
import org.guakamole.onair.metadata.MusicBrainzMetadataRefiner
import org.guakamole.onair.service.RadioPlaybackService

/**
 * Centralized manager for radio metadata. Handles merging of raw stream metadata, polled API
 * metadata, and refined (MusicBrainz) metadata. Emits the final calculated metadata to be applied
 * to the player.
 */
class RadioMetadataManager(
        private val scope: CoroutineScope,
        private val artworkManager: ArtworkManager
) {

    private val _metadataUpdates = MutableSharedFlow<MediaMetadata?>()
    val metadataUpdates: SharedFlow<MediaMetadata?> = _metadataUpdates.asSharedFlow()

    private var currentStationId: String? = null

    // Last Raw states for change detection
    private var lastRawArtist: String? = null
    private var lastRawTitle: String? = null
    private var lastRawStationId: String? = null

    // Current state inputs
    private var currentRawTitle: String? = null
    private var currentRawArtist: String? = null

    // We store the "active" metadata to check against weak raw updates
    private var activeType: MetadataType = MetadataType.UNKNOWN

    fun onStationChange(stationId: String?) {
        currentStationId = stationId
        lastRawArtist = null
        lastRawTitle = null
        lastRawStationId = null
        activeType = MetadataType.UNKNOWN

        // Clear metadata logic handled by service (setOverriddenMetadata(null))
        // But we reset our internal state
    }

    fun onRawMetadata(title: String?, artist: String?) {
        currentRawTitle = title
        currentRawArtist = artist
        processMetadata(title, artist, null, MetadataType.UNKNOWN)
    }

    fun onPolledMetadata(result: MetadataResult) {
        processMetadata(result.title, result.artist, result.artworkUrl, result.type)
    }

    private fun processMetadata(
            titleArg: String?,
            artistArg: String?,
            artworkUrlArg: String? = null,
            typeArg: MetadataType = MetadataType.UNKNOWN
    ) {
        // Prepare final candidates
        var finalTitle = titleArg ?: currentRawTitle
        var finalArtist = artistArg
        val finalArtworkUrl = artworkUrlArg
        var finalType = typeArg

        // Intelligent splitting for "Artist - Title" format common in ICY streams
        if (finalArtist == null && finalTitle != null && finalTitle.contains(" - ")) {
            val parts = finalTitle.split(" - ", limit = 2)
            finalArtist = parts[0].trim()
            finalTitle = parts[1].trim()

            // Assume it's a song if we successfully split "Artist - Title" from raw metadata
            if (finalType == MetadataType.UNKNOWN) {
                finalType = MetadataType.SONG
            }
        }

        // Fallback for artist if still null
        if (finalArtist == null && currentStationId != null) {
            val station = RadioRepository.getStationById(currentStationId!!)
            finalArtist = station?.name ?: currentRawArtist
        }

        val station = currentStationId?.let { RadioRepository.getStationById(it) }

        // Logic Rule 1: Ignore weak raw updates if we have a specific override active
        val isRawUpdate = typeArg == MetadataType.UNKNOWN
        if (isRawUpdate && activeType != MetadataType.UNKNOWN) {
            if (titleArg.isNullOrBlank() || titleArg.trim().equals(station?.name, ignoreCase = true)
            ) {
                android.util.Log.d(
                        "MetadataDebug",
                        "Manager: Ignoring weak raw update over specific override"
                )
                return
            }
        }

        // Update active type
        if (!isRawUpdate) {
            activeType = finalType
        } else if (activeType != MetadataType.UNKNOWN &&
                        (titleArg.isNullOrBlank() ||
                                titleArg.trim().equals(station?.name, ignoreCase = true))
        ) {
            // If we accepted a weak raw update (because activeType was UNKNOWN), we shouldn't
            // promote it to specific
            // But here we are in the "else" of the ignore check, meaning either activeType IS
            // unknown
            // OR the raw update IS strong.
        }

        // Logic Rule 2: Check for changes relative to last RAW data for refinement
        // Only refine if this is a RAW update or a generic update that needs refinement
        // AND we don't have a direct artwork URL (refinement is mostly for artwork/cleanup)
        if (artworkUrlArg == null &&
                        (finalArtist != lastRawArtist ||
                                finalTitle != lastRawTitle ||
                                currentStationId != lastRawStationId)
        ) {
            lastRawArtist = finalArtist
            lastRawTitle = finalTitle
            lastRawStationId = currentStationId

            // Only refine SONGS
            if (finalType == MetadataType.SONG && finalArtist != null && finalTitle != null) {
                scope.launch {
                    val refined =
                            MusicBrainzMetadataRefiner.refine(finalArtist, finalTitle, finalType)
                    if (refined != null) {
                        // Apply refined immediatley serves as recursive call with specific type
                        processMetadata(
                                refined.title,
                                refined.artist,
                                refined.artworkUrl,
                                refined.type
                        )
                    } else {
                        // Apply what we have
                        buildEmittedMetadata(finalTitle, finalArtist, finalArtworkUrl, finalType)
                    }
                }
                return
            }
        } else if (artworkUrlArg == null && isRawUpdate) {
            // No changes in raw metadata, same old same old. Skip.
            return
        }

        buildEmittedMetadata(finalTitle, finalArtist, finalArtworkUrl, finalType)
    }

    private fun buildEmittedMetadata(
            title: String?,
            artist: String?,
            artworkUrl: String?,
            type: MetadataType
    ) {
        val typeName = type.name
        val builder = MediaMetadata.Builder().setTitle(title).setArtist(artist).setSubtitle(artist)

        // Artwork handling
        val effectiveArtworkUri: Uri?
        val isSongArtwork: Boolean

        if (artworkUrl != null) {
            effectiveArtworkUri = Uri.parse(artworkUrl)
            isSongArtwork = (type == MetadataType.SONG)
        } else {
            val station = currentStationId?.let { RadioRepository.getStationById(it) }
            effectiveArtworkUri = station?.let { artworkManager.getStationArtworkUri(it) }
            isSongArtwork = false
        }

        if (effectiveArtworkUri != null) {
            builder.setArtworkUri(effectiveArtworkUri)
            // Asynchronously load artwork data
            scope.launch {
                val artworkData = artworkManager.loadArtwork(effectiveArtworkUri)
                if (artworkData != null) {
                    emitFinalMetadata(builder, typeName, artworkData, isSongArtwork)
                } else {
                    emitFinalMetadata(builder, typeName, null, isSongArtwork)
                }
            }
        } else {
            // No artwork URL, emit text only
            scope.launch { emitFinalMetadata(builder, typeName, null, false) }
        }
    }

    private suspend fun emitFinalMetadata(
            builder: MediaMetadata.Builder,
            typeName: String,
            artworkData: ByteArray?,
            isSongArtwork: Boolean
    ) {
        val extras =
                android.os.Bundle().apply {
                    putString(RadioPlaybackService.EXT_CONTENT_TYPE, typeName)
                    putBoolean(RadioPlaybackService.EXT_IS_SONG_ARTWORK, isSongArtwork)
                }

        builder.setExtras(extras)

        if (artworkData != null) {
            builder.setArtworkData(artworkData, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            builder.setArtworkUri(null) // Prioritize data
        }

        _metadataUpdates.emit(builder.build())
    }
}
