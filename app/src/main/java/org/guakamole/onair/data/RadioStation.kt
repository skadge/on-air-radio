package org.guakamole.onair.data

import androidx.annotation.StringRes

/** Represents a radio station with its streaming information */
data class RadioStation(
        val id: String,
        val name: String,
        val streamUrl: String,
        val logoUrl: String,
        val logoResId: Int = 0,
        val description: String = "",
        @StringRes val genre: Int = 0,
        @StringRes val country: Int = 0,
        val isFavorite: Boolean = false,
        val popularity: Int = 0,
        val tags: String = ""
)
