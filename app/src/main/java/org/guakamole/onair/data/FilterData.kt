package org.guakamole.onair.data

import org.guakamole.onair.R

data class FilterItem(
        val id: String,
        val nameRes: Int,
        val iconRes: Int? = null,
        val countries: List<Int> = emptyList(), // For regions
        val tag: String = "" // For style filters
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
                                                R.string.country_germany,
                                                R.string.country_italy,
                                                R.string.country_spain,
                                                R.string.country_netherlands
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
                        FilterItem("germany", R.string.country_germany),
                        FilterItem("italy", R.string.country_italy),
                        FilterItem("spain", R.string.country_spain),
                        FilterItem("netherlands", R.string.country_netherlands)
                )

        val styles =
                listOf(
                        FilterItem("pop", R.string.tag_pop, tag = "pop"),
                        FilterItem("rock", R.string.tag_rock, tag = "rock"),
                        FilterItem("hits", R.string.tag_hits, tag = "hits"),
                        FilterItem("jazz", R.string.tag_jazz, tag = "jazz"),
                        FilterItem("classical", R.string.tag_classical, tag = "classical"),
                        FilterItem("news", R.string.tag_news, tag = "news"),
                        FilterItem("talk", R.string.tag_talk, tag = "talk"),
                        FilterItem("ambient", R.string.tag_ambient, tag = "ambient"),
                        FilterItem("world", R.string.tag_world, tag = "world"),
                        FilterItem("oldies", R.string.tag_oldies, tag = "oldies")
                )

        // Helper to get all countries covered by a filter item (region or country)
        fun getCountriesForFilter(item: FilterItem): List<Int> {
                if (item.id == "world") return emptyList() // All countries
                if (item.countries.isNotEmpty()) return item.countries
                return listOf(item.nameRes)
        }
}
