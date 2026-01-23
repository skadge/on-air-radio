package org.guakamole.worldradio.ui.theme

import androidx.compose.ui.graphics.Color
import org.guakamole.worldradio.R

object GenreColors {
    fun getColorForGenre(genreResId: Int): Color {
        return when (genreResId) {
            R.string.genre_pop_rock -> Color(0xFFE91E63) // Pink
            R.string.genre_adult_contemporary -> Color(0xFF9C27B0) // Purple
            R.string.genre_news_talk -> Color(0xFF3F51B5) // Indigo
            R.string.genre_classical -> Color(0xFF009688) // Teal
            R.string.genre_talk_music -> Color(0xFFFF5722) // Deep Orange
            R.string.genre_culture -> Color(0xFF8BC34A) // Light Green
            R.string.genre_eclectic -> Color(0xFFFFC107) // Amber
            R.string.genre_eclectic_world -> Color(0xFF795548) // Brown
            R.string.genre_news -> Color(0xFF2196F3) // Blue
            R.string.genre_alternative -> Color(0xFF673AB7) // Deep Purple
            R.string.genre_classical_culture -> Color(0xFF00BCD4) // Cyan
            R.string.genre_news_culture -> Color(0xFF4CAF50) // Green
            R.string.genre_jazz -> Color(0xFFF44336) // Red
            R.string.genre_ambient_chill -> Color(0xFF607D8B) // Blue Grey
            R.string.genre_ambient -> Color(0xFF9E9E9E) // Grey
            else -> Color(0xFF535353) // Default Dark Grey
        }
    }
}
