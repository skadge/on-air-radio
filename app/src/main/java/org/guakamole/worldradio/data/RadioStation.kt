package org.guakamole.worldradio.data

/** Represents a radio station with its streaming information */
data class RadioStation(
        val id: String,
        val name: String,
        val streamUrl: String,
        val logoUrl: String,
        val description: String = "",
        val genre: String = "",
        val country: String = "",
        val isFavorite: Boolean = false
)
