package org.guakamole.onair.ui.theme

import androidx.compose.ui.graphics.Color

object TagColors {
    fun getColorForTag(tag: String): Color {
        return when (tag.lowercase()) {
            // Pop - Magenta/Pink
            "pop" -> Color(0xFFE91E63)

            // Rock - Deep Red
            "rock" -> Color(0xFFD32F2F)

            // Hits - Orange
            "hits" -> Color(0xFFFF9800)

            // Alternative & Indie - Deep Purple
            "indie",
            "alternative",
            "electronic" -> Color(0xFF673AB7)

            // World & Eclectic - Teal
            "world",
            "eclectic" -> Color(0xFF00897B)

            // News & Talk - Indigo
            "news",
            "talk" -> Color(0xFF3F51B5)

            // Classical & Culture - Green
            "classical",
            "culture" -> Color(0xFF009688)

            // Jazz & Soul - Deep Orange
            "jazz",
            "soul" -> Color(0xFFFF5722)

            // Ambient & Chill - Blue Grey
            "ambient",
            "chill" -> Color(0xFF607D8B)

            // Oldies - Amber
            "oldies" -> Color(0xFFFFC107)
            else -> Color(0xFF535353) // Default Dark Grey
        }
    }
}
