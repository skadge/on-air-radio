package org.guakamole.onair.ui.theme

import androidx.compose.ui.graphics.Color
import org.guakamole.onair.R

object GenreColors {
    fun getColorForGenre(genreResId: Int): Color {
        return when (genreResId) {
            // Pop & Hits
            R.string.genre_hits,
            R.string.genre_pop_rock,
            R.string.genre_variety -> Color(0xFFE91E63) // Pink

            // Adult Contemporary
            R.string.genre_adult_contemporary -> Color(0xFF9C27B0) // Purple

            // Alternative & Eclectic
            R.string.genre_alternative,
            R.string.genre_eclectic,
            R.string.genre_eclectic_world -> Color(0xFF673AB7) // Deep Purple

            // News & Talk
            R.string.genre_news,
            R.string.genre_news_talk,
            R.string.genre_talk_music -> Color(0xFF3F51B5) // Indigo

            // Classical & Culture
            R.string.genre_classical,
            R.string.genre_classical_culture,
            R.string.genre_culture,
            R.string.genre_news_culture -> Color(0xFF009688) // Teal

            // Jazz
            R.string.genre_jazz -> Color(0xFFFF5722) // Deep Orange

            // Ambient & Chill
            R.string.genre_ambient,
            R.string.genre_ambient_chill -> Color(0xFF607D8B) // Blue Grey

            // Oldies
            R.string.genre_oldies -> Color(0xFFFFC107) // Amber
            else -> Color(0xFF535353) // Default Dark Grey
        }
    }
}
