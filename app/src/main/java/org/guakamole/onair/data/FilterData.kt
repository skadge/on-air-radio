package org.guakamole.onair.data

import org.guakamole.onair.R

data class FilterItem(
        val id: String,
        val nameRes: Int,
        val iconRes: Int? = null,
        val countries: List<Int> = emptyList() // For regions
)

object FilterData {
    val regions =
            listOf(
                    FilterItem("world", R.string.world),
                    FilterItem(
                            "europe",
                            R.string.region_europe,
                            countries =
                                    listOf(
                                            R.string.country_uk,
                                            R.string.country_france,
                                            R.string.country_germany
                                    )
                    ),
                    FilterItem(
                            "north_america",
                            R.string.region_north_america,
                            countries = listOf(R.string.country_usa)
                    ),
                    // Individual countries
                    FilterItem("uk", R.string.country_uk),
                    FilterItem("france", R.string.country_france),
                    FilterItem("usa", R.string.country_usa),
                    FilterItem("germany", R.string.country_germany)
            )

    val styles =
            listOf(
                    FilterItem("pop_rock", R.string.genre_pop_rock),
                    FilterItem("jazz", R.string.genre_jazz),
                    FilterItem("classical", R.string.genre_classical),
                    FilterItem("news", R.string.genre_news),
                    FilterItem("ambient", R.string.genre_ambient),
                    FilterItem("eclectic", R.string.genre_eclectic)
            )

    // Helper to get all countries covered by a filter item (region or country)
    fun getCountriesForFilter(item: FilterItem): List<Int> {
        if (item.id == "world") return emptyList() // All countries
        if (item.countries.isNotEmpty()) return item.countries
        return listOf(item.nameRes)
    }

    // Helper to check if a station matches any of the selected styles
    // Since some stations have combined genres (e.g. genre_pop_rock),
    // we might need a more flexible check or just exact match if we simplify.
    // For now, let's assume exact match with the resource ID.
}
