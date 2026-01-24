package org.guakamole.onair.data

import android.content.Context
import android.content.SharedPreferences
import org.guakamole.onair.R

/** Repository providing a curated list of public radio stations */
object RadioRepository {

        private const val PREFS_NAME = "radio_prefs"
        private const val FAVORITES_KEY = "favorite_stations"
        private var prefs: SharedPreferences? = null
        private val favoriteIds = mutableSetOf<String>()

        private val baseStations: List<RadioStation> =
                listOf(
                        // International
                        RadioStation(
                                id = "bbc_radio1",
                                name = "BBC Radio 1",
                                streamUrl =
                                        "https://a.files.bbci.co.uk/ms6/live/3441A116-B12E-4D2F-ACA8-C1984642FA4B/audio/simulcast/hls/nonuk/pc_hd_abr_v2/ak/bbc_radio_one.m3u8",
                                logoUrl = "https://cdn-radiotime-logos.tunein.com/s24939q.png",
                                logoResId = R.drawable.logo_bbc_radio_1,
                                description = "The best new music and entertainment",
                                genre = R.string.genre_pop_rock,
                                country = R.string.country_uk
                        ),
                        RadioStation(
                                id = "bbc_radio2",
                                name = "BBC Radio 2",
                                streamUrl =
                                        "https://as-hls-ww-live.akamaized.net/pool_74208725/live/ww/bbc_radio_two/bbc_radio_two.isml/bbc_radio_two-audio%3d128000.norewind.m3u8",
                                logoUrl = "https://cdn-radiotime-logos.tunein.com/s24940q.png",
                                logoResId = R.drawable.logo_bbc_radio_2,
                                description = "Great music, great variety",
                                genre = R.string.genre_adult_contemporary,
                                country = R.string.country_uk
                        ),
                        RadioStation(
                                id = "bbc_radio4",
                                name = "BBC Radio 4",
                                streamUrl =
                                        "https://as-hls-ww-live.akamaized.net/pool_55057080/live/ww/bbc_radio_fourfm/bbc_radio_fourfm.isml/bbc_radio_fourfm-audio%3d128000.norewind.m3u8",
                                logoUrl = "https://cdn-radiotime-logos.tunein.com/s25419q.png",
                                logoResId = R.drawable.logo_bbc_radio_4,
                                description = "Intelligent speech radio",
                                genre = R.string.genre_news_talk,
                                country = R.string.country_uk
                        ),
                        RadioStation(
                                id = "classic_fm",
                                name = "Classic FM",
                                streamUrl = "https://media-ice.musicradio.com/ClassicFMMP3",
                                logoUrl =
                                        "https://www.classicfm.com/assets_v4r/classic/img/favicon-196x196.png",
                                logoResId = R.drawable.logo_classic_fm,
                                description = "The world's greatest music",
                                genre = R.string.genre_classical,
                                country = R.string.country_uk
                        ),

                        // France
                        RadioStation(
                                id = "france_inter",
                                name = "France Inter",
                                streamUrl = "https://icecast.radiofrance.fr/franceinter-midfi.mp3",
                                logoUrl =
                                        "https://www.lesentreprisesdupaysage.fr/content/uploads/2022/03/france_inter_logo_2021-svg-1024x1024.png",
                                description = "La radio de service public",
                                genre = R.string.genre_talk_music,
                                country = R.string.country_france
                        ),
                        RadioStation(
                                id = "france_culture",
                                name = "France Culture",
                                streamUrl =
                                        "https://icecast.radiofrance.fr/franceculture-midfi.mp3",
                                logoUrl =
                                        "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d6/France_Culture_logo_2021.svg/250px-France_Culture_logo_2021.svg.png",
                                logoResId = R.drawable.logo_france_culture,
                                description = "Culture et savoirs",
                                genre = R.string.genre_culture,
                                country = R.string.country_france
                        ),
                        RadioStation(
                                id = "france_musique",
                                name = "France Musique",
                                streamUrl =
                                        "https://icecast.radiofrance.fr/francemusique-midfi.mp3",
                                logoUrl =
                                        "https://upload.wikimedia.org/wikipedia/commons/6/6c/France_Musique_logo_2021.svg",
                                logoResId = R.drawable.logo_france_musique,
                                description = "La musique classique et plus",
                                genre = R.string.genre_classical,
                                country = R.string.country_france
                        ),
                        RadioStation(
                                id = "fip",
                                name = "FIP",
                                streamUrl = "https://icecast.radiofrance.fr/fip-midfi.mp3",
                                logoUrl =
                                        "https://upload.wikimedia.org/wikipedia/commons/1/16/FIP_logo_2021.svg",
                                logoResId = R.drawable.logo_fip,
                                description = "Éclectisme musical",
                                genre = R.string.genre_eclectic,
                                country = R.string.country_france
                        ),
                        RadioStation(
                                id = "radio_nova",
                                name = "Radio Nova",
                                streamUrl = "https://novazz.ice.infomaniak.ch/novazz-128.mp3",
                                logoUrl =
                                        "https://www.nova.fr/wp-content/thumbnails/uploads/sites/2/2024/05/NOVA-nova-400x496-1-t-1700x1030.png",
                                logoResId = R.drawable.logo_radio_nova,
                                description = "Le Grand Mix de Radio Nova",
                                genre = R.string.genre_eclectic_world,
                                country = R.string.country_france
                        ),

                        // USA
                        RadioStation(
                                id = "npr",
                                name = "NPR News",
                                streamUrl = "https://npr-ice.streamguys1.com/live.mp3",
                                logoUrl =
                                        "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d7/National_Public_Radio_logo.svg/200px-National_Public_Radio_logo.svg.png",
                                logoResId = R.drawable.logo_npr,
                                description = "National Public Radio",
                                genre = R.string.genre_news,
                                country = R.string.country_usa
                        ),
                        RadioStation(
                                id = "kexp",
                                name = "KEXP 90.3",
                                streamUrl = "https://kexp-mp3-128.streamguys1.com/kexp128.mp3",
                                logoUrl =
                                        "https://upload.wikimedia.org/wikipedia/commons/thumb/6/69/KEXP_logo.svg/200px-KEXP_logo.svg.png",
                                logoResId = R.drawable.logo_kexp,
                                description = "Where the music matters",
                                genre = R.string.genre_alternative,
                                country = R.string.country_usa
                        ),

                        // Germany
                        RadioStation(
                                id = "wdr3",
                                name = "WDR 3",
                                streamUrl =
                                        "https://wdr-wdr3-live.icecastssl.wdr.de/wdr/wdr3/live/mp3/128/stream.mp3",
                                logoUrl =
                                        "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e6/WDR_3_logo_2012.svg/200px-WDR_3_logo_2012.svg.png",
                                logoResId = R.drawable.logo_wdr3,
                                description = "Kultur und Klassik",
                                genre = R.string.genre_classical_culture,
                                country = R.string.country_germany
                        ),
                        RadioStation(
                                id = "deutschlandfunk",
                                name = "Deutschlandfunk",
                                streamUrl =
                                        "https://st01.sslstream.dlf.de/dlf/01/128/mp3/stream.mp3",
                                logoUrl =
                                        "https://upload.wikimedia.org/wikipedia/commons/thumb/1/14/Deutschlandfunk_Logo_2017.svg/200px-Deutschlandfunk_Logo_2017.svg.png",
                                logoResId = R.drawable.logo_deutschlandradio,
                                description = "Nachrichten und Kultur",
                                genre = R.string.genre_news_culture,
                                country = R.string.country_germany
                        ),

                        // Jazz
                        RadioStation(
                                id = "jazz_radio",
                                name = "Jazz Radio",
                                streamUrl =
                                        "https://jazzradio.ice.infomaniak.ch/jazzradio-high.mp3",
                                logoUrl =
                                        "https://upload.wikimedia.org/wikipedia/fr/thumb/2/24/Logo_Jazz_Radio.svg/200px-Logo_Jazz_Radio.svg.png",
                                //logoResId = R.drawable.logo_jazz_radio,
                                description = "100% Jazz",
                                genre = R.string.genre_jazz,
                                country = R.string.country_france
                        ),
                        RadioStation(
                                id = "tsfjazz",
                                name = "TSF Jazz",
                                streamUrl = "https://tsfjazz.ice.infomaniak.ch/tsfjazz-high.mp3",
                                logoUrl =
                                        "https://upload.wikimedia.org/wikipedia/fr/thumb/4/44/TSF_Jazz_logo.svg/200px-TSF_Jazz_logo.svg.png",
                                //logoResId = R.drawable.logo_tsfjazz,
                                description = "La radio jazz",
                                genre = R.string.genre_jazz,
                                country = R.string.country_france
                        ),

                        // World
                        RadioStation(
                                id = "soma_groove",
                                name = "SomaFM Groove Salad",
                                streamUrl = "https://ice1.somafm.com/groovesalad-128-mp3",
                                logoUrl = "https://somafm.com/img3/groovesalad-400.jpg",
                                //logoResId = R.drawable.logo_somafm,
                                description = "A nicely chilled plate of ambient beats",
                                genre = R.string.genre_ambient_chill,
                                country = R.string.country_usa
                        ),
                        RadioStation(
                                id = "soma_drone",
                                name = "SomaFM Drone Zone",
                                streamUrl = "https://ice1.somafm.com/dronezone-128-mp3",
                                logoUrl = "https://somafm.com/img3/dronezone-400.jpg",
                                //logoResId = R.drawable.logo_somafm,
                                description = "Atmospheric textures with minimal beats",
                                genre = R.string.genre_ambient,
                                country = R.string.country_usa
                        )
                )

        /** Initialize favorites from persistent storage */
        fun initialize(context: Context) {
                if (prefs == null) {
                        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val savedFavorites =
                                prefs?.getStringSet(FAVORITES_KEY, emptySet()) ?: emptySet()
                        favoriteIds.clear()
                        favoriteIds.addAll(savedFavorites)
                }
        }

        /**
         * Returns the list of stations with correctly set isFavorite flags, sorted by favorite
         * status
         */
        val stations: List<RadioStation>
                get() =
                        baseStations
                                .map { station ->
                                        station.copy(isFavorite = favoriteIds.contains(station.id))
                                }
                                .sortedByDescending { it.isFavorite }

        fun toggleFavorite(stationId: String) {
                if (favoriteIds.contains(stationId)) {
                        favoriteIds.remove(stationId)
                } else {
                        favoriteIds.add(stationId)
                }

                prefs?.edit()?.putStringSet(FAVORITES_KEY, favoriteIds)?.apply()
        }

        fun getStationById(id: String): RadioStation? {
                return stations.find { it.id == id }
        }

        fun getStationByIndex(index: Int): RadioStation? {
                return stations.getOrNull(index)
        }

        fun getStationIndex(id: String): Int {
                return stations.indexOfFirst { it.id == id }
        }
}
