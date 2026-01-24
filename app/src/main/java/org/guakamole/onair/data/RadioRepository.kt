// AUTO-GENERATED from stations.yaml - DO NOT EDIT MANUALLY
// Run: ./gradlew buildStations (or python scripts/build_stations.py)
// Generated with 53 stations

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
                        RadioStation(
                                id = "antenne_bayern",
                                name = "Antenne Bayern",
                                streamUrl = "https://stream.antenne.de/antenne",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1b/Antenne_Bayern_logo.svg/200px-Antenne_Bayern_logo.svg.png",
                                logoResId = R.drawable.logo_antenne_bayern,
                                description = "Hits für Bayern",
                                genre = R.string.genre_hits,
                                country = R.string.country_germany
                        ),
                        RadioStation(
                                id = "bayern_1",
                                name = "Bayern 1",
                                streamUrl = "https://dispatcher.rndfnk.com/br/br1/obb/mp3/mid",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8b/Bayern_1_Logo.svg/200px-Bayern_1_Logo.svg.png",
                                logoResId = R.drawable.logo_bayern_1,
                                description = "Die beste Musik für Bayern",
                                genre = R.string.genre_oldies,
                                country = R.string.country_germany
                        ),
                        RadioStation(
                                id = "bbc_radio_1",
                                name = "BBC Radio 1",
                                streamUrl = "https://as-hls-ww-live.akamaized.net/pool_01505109/live/ww/bbc_radio_one/bbc_radio_one.isml/bbc_radio_one-audio%3d96000.norewind.m3u8",
                                logoUrl = "https://cdn-radiotime-logos.tunein.com/s24939q.png",
                                logoResId = R.drawable.logo_bbc_radio_1,
                                description = "The best new music and entertainment",
                                genre = R.string.genre_pop_rock,
                                country = R.string.country_uk
                        ),
                        RadioStation(
                                id = "bbc_radio_2",
                                name = "BBC Radio 2",
                                streamUrl = "https://as-hls-ww-live.akamaized.net/pool_74208725/live/ww/bbc_radio_two/bbc_radio_two.isml/bbc_radio_two-audio%3d128000.norewind.m3u8",
                                logoUrl = "https://cdn-radiotime-logos.tunein.com/s24940q.png",
                                logoResId = R.drawable.logo_bbc_radio_2,
                                description = "Great music, great variety",
                                genre = R.string.genre_adult_contemporary,
                                country = R.string.country_uk
                        ),
                        RadioStation(
                                id = "bbc_radio_3",
                                name = "BBC Radio 3",
                                streamUrl = "http://as-hls-ww-live.akamaized.net/pool_904/live/ww/bbc_radio_three/bbc_radio_three.isml/bbc_radio_three-audio%3d96000.norewind.m3u8",
                                logoUrl = "https://cdn-radiotime-logos.tunein.com/s24941q.png",
                                logoResId = R.drawable.logo_bbc_radio_3,
                                description = "Classical, jazz, world music, arts and drama",
                                genre = R.string.genre_classical,
                                country = R.string.country_uk
                        ),
                        RadioStation(
                                id = "bbc_radio_4",
                                name = "BBC Radio 4",
                                streamUrl = "https://as-hls-ww-live.akamaized.net/pool_55057080/live/ww/bbc_radio_fourfm/bbc_radio_fourfm.isml/bbc_radio_fourfm-audio%3d128000.norewind.m3u8",
                                logoUrl = "https://cdn-radiotime-logos.tunein.com/s25419q.png",
                                logoResId = R.drawable.logo_bbc_radio_4,
                                description = "Intelligent speech radio",
                                genre = R.string.genre_news_talk,
                                country = R.string.country_uk
                        ),
                        RadioStation(
                                id = "bbc_radio_5_live",
                                name = "BBC Radio 5 Live",
                                streamUrl = "http://as-hls-ww-live.akamaized.net/pool_904/live/ww/bbc_radio_five_live/bbc_radio_five_live.isml/bbc_radio_five_live-audio=96000.m3u8",
                                logoUrl = "https://cdn-radiotime-logos.tunein.com/s24943q.png",
                                logoResId = R.drawable.logo_bbc_radio_5_live,
                                description = "Live news and live sport",
                                genre = R.string.genre_news_talk,
                                country = R.string.country_uk
                        ),
                        RadioStation(
                                id = "bbc_6music",
                                name = "BBC Radio 6 Music",
                                streamUrl = "https://as-hls-ww-live.akamaized.net/pool_904/live/ww/bbc_6music/bbc_6music.isml/bbc_6music-audio%3d96000.norewind.m3u8",
                                logoUrl = "https://cdn-radiotime-logos.tunein.com/s24944q.png",
                                logoResId = R.drawable.logo_bbc_6music,
                                description = "The place for alternative music",
                                genre = R.string.genre_alternative,
                                country = R.string.country_uk
                        ),
                        RadioStation(
                                id = "cadena_dial",
                                name = "Cadena Dial",
                                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/CADENADIAL.mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/34/Cadena_Dial_logo.svg/200px-Cadena_Dial_logo.svg.png",
                                logoResId = R.drawable.logo_cadena_dial,
                                description = "La radio de las baladas",
                                genre = R.string.genre_adult_contemporary,
                                country = R.string.country_spain
                        ),
                        RadioStation(
                                id = "cadena_ser",
                                name = "Cadena SER",
                                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/CADENASER.mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4e/Cadena_SER_logo.svg/200px-Cadena_SER_logo.svg.png",
                                logoResId = R.drawable.logo_cadena_ser,
                                description = "La radio líder en España",
                                genre = R.string.genre_news_talk,
                                country = R.string.country_spain
                        ),
                        RadioStation(
                                id = "capital_fm",
                                name = "Capital FM",
                                streamUrl = "http://icecast.thisisdax.com/CapitalUKMP3",
                                logoUrl = "https://cdn-radiotime-logos.tunein.com/s2728q.png",
                                logoResId = R.drawable.logo_capital_fm,
                                description = "The UK's No.1 Hit Music Station",
                                genre = R.string.genre_hits,
                                country = R.string.country_uk
                        ),
                        RadioStation(
                                id = "classic_fm",
                                name = "Classic FM",
                                streamUrl = "https://media-ice.musicradio.com/ClassicFMMP3",
                                logoUrl = "https://www.classicfm.com/assets_v4r/classic/img/favicon-196x196.png",
                                logoResId = R.drawable.logo_classic_fm,
                                description = "The world's greatest music",
                                genre = R.string.genre_classical,
                                country = R.string.country_uk
                        ),
                        RadioStation(
                                id = "cope",
                                name = "COPE",
                                streamUrl = "https://flucast23-h.akamaihd.net/COPEMADRID_SC",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/Cope_logo.svg/200px-Cope_logo.svg.png",
                                logoResId = R.drawable.logo_cope,
                                description = "Radio COPE - Cadena de Ondas Populares Españolas",
                                genre = R.string.genre_news_talk,
                                country = R.string.country_spain
                        ),
                        RadioStation(
                                id = "deutschlandradio",
                                name = "Deutschlandfunk",
                                streamUrl = "https://st01.sslstream.dlf.de/dlf/01/128/mp3/stream.mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/14/Deutschlandfunk_Logo_2017.svg/200px-Deutschlandfunk_Logo_2017.svg.png",
                                logoResId = R.drawable.logo_deutschlandradio,
                                description = "Nachrichten und Kultur",
                                genre = R.string.genre_news_culture,
                                country = R.string.country_germany
                        ),
                        RadioStation(
                                id = "fip",
                                name = "FIP",
                                streamUrl = "https://icecast.radiofrance.fr/fip-midfi.mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/1/16/FIP_logo_2021.svg",
                                logoResId = R.drawable.logo_fip,
                                description = "Éclectisme musical",
                                genre = R.string.genre_eclectic,
                                country = R.string.country_france
                        ),
                        RadioStation(
                                id = "france_culture",
                                name = "France Culture",
                                streamUrl = "https://icecast.radiofrance.fr/franceculture-midfi.mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d6/France_Culture_logo_2021.svg/250px-France_Culture_logo_2021.svg.png",
                                logoResId = R.drawable.logo_france_culture,
                                description = "Culture et savoirs",
                                genre = R.string.genre_culture,
                                country = R.string.country_france
                        ),
                        RadioStation(
                                id = "france_inter",
                                name = "France Inter",
                                streamUrl = "https://icecast.radiofrance.fr/franceinter-midfi.mp3",
                                logoUrl = "https://www.lesentreprisesdupaysage.fr/content/uploads/2022/03/france_inter_logo_2021-svg-1024x1024.png",
                                logoResId = R.drawable.logo_france_inter,
                                description = "La radio de service public",
                                genre = R.string.genre_talk_music,
                                country = R.string.country_france
                        ),
                        RadioStation(
                                id = "france_musique",
                                name = "France Musique",
                                streamUrl = "https://icecast.radiofrance.fr/francemusique-midfi.mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/6/6c/France_Musique_logo_2021.svg",
                                logoResId = R.drawable.logo_france_musique,
                                description = "La musique classique et plus",
                                genre = R.string.genre_classical,
                                country = R.string.country_france
                        ),
                        RadioStation(
                                id = "franceinfo",
                                name = "Franceinfo",
                                streamUrl = "https://icecast.radiofrance.fr/franceinfo-midfi.mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/74/France_Info_2016.svg/200px-France_Info_2016.svg.png",
                                logoResId = R.drawable.logo_franceinfo,
                                description = "L'info en continu",
                                genre = R.string.genre_news,
                                country = R.string.country_france
                        ),
                        RadioStation(
                                id = "heart_uk",
                                name = "Heart UK",
                                streamUrl = "http://ice-sov.musicradio.com/HeartLondonMP3",
                                logoUrl = "https://cdn-radiotime-logos.tunein.com/s121960q.png",
                                logoResId = R.drawable.logo_heart_uk,
                                description = "Turn up the feel good",
                                genre = R.string.genre_pop_rock,
                                country = R.string.country_uk
                        ),
                        RadioStation(
                                id = "jazz_radio",
                                name = "Jazz Radio",
                                streamUrl = "https://jazzradio.ice.infomaniak.ch/jazzradio-high.mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/fr/thumb/2/24/Logo_Jazz_Radio.svg/200px-Logo_Jazz_Radio.svg.png",
                                logoResId = 0,
                                description = "100% Jazz",
                                genre = R.string.genre_jazz,
                                country = R.string.country_france
                        ),
                        RadioStation(
                                id = "jazz24",
                                name = "Jazz24",
                                streamUrl = "https://knkx-live-a.edge.audiocdn.com/6285_128k",
                                logoUrl = "https://cdn-radiotime-logos.tunein.com/s6861q.png",
                                logoResId = 0,
                                description = "Jazz from Seattle's KNKX",
                                genre = R.string.genre_jazz,
                                country = R.string.country_usa
                        ),
                        RadioStation(
                                id = "kcrw",
                                name = "KCRW 89.9",
                                streamUrl = "https://kcrw.streamguys1.com/kcrw_128k_mp3_on_air",
                                logoUrl = "https://cdn-radiotime-logos.tunein.com/s24748q.png",
                                logoResId = R.drawable.logo_kcrw,
                                description = "Music, news, and culture from LA",
                                genre = R.string.genre_eclectic,
                                country = R.string.country_usa
                        ),
                        RadioStation(
                                id = "kexp",
                                name = "KEXP 90.3",
                                streamUrl = "https://kexp-mp3-128.streamguys1.com/kexp128.mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/69/KEXP_logo.svg/200px-KEXP_logo.svg.png",
                                logoResId = R.drawable.logo_kexp,
                                description = "Where the music matters",
                                genre = R.string.genre_alternative,
                                country = R.string.country_usa
                        ),
                        RadioStation(
                                id = "lbc",
                                name = "LBC",
                                streamUrl = "http://icecast.thisisdax.com/LBCUKMP3",
                                logoUrl = "https://cdn-radiotime-logos.tunein.com/s25071q.png",
                                logoResId = R.drawable.logo_lbc,
                                description = "Leading Britain's Conversation",
                                genre = R.string.genre_news_talk,
                                country = R.string.country_uk
                        ),
                        RadioStation(
                                id = "los40",
                                name = "LOS40",
                                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/LOS40.mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5f/LOS40_logo.svg/200px-LOS40_logo.svg.png",
                                logoResId = R.drawable.logo_los40,
                                description = "La radio de los éxitos",
                                genre = R.string.genre_hits,
                                country = R.string.country_spain
                        ),
                        RadioStation(
                                id = "nostalgie",
                                name = "Nostalgie",
                                streamUrl = "https://scdn.nrjaudio.fm/adwz2/fr/30601/mp3_128.mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/fr/thumb/e/ed/Logo_Nostalgie_2001.svg/200px-Logo_Nostalgie_2001.svg.png",
                                logoResId = R.drawable.logo_nostalgie,
                                description = "Que des tubes",
                                genre = R.string.genre_oldies,
                                country = R.string.country_france
                        ),
                        RadioStation(
                                id = "npo_radio_2",
                                name = "NPO Radio 2",
                                streamUrl = "http://icecast.omroep.nl/radio2-bb-mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a2/NPO_Radio_2_logo.svg/200px-NPO_Radio_2_logo.svg.png",
                                logoResId = R.drawable.logo_npo_radio_2,
                                description = "De beste muziekmix",
                                genre = R.string.genre_variety,
                                country = R.string.country_netherlands
                        ),
                        RadioStation(
                                id = "npr",
                                name = "NPR News",
                                streamUrl = "https://npr-ice.streamguys1.com/live.mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d7/National_Public_Radio_logo.svg/200px-National_Public_Radio_logo.svg.png",
                                logoResId = R.drawable.logo_npr,
                                description = "National Public Radio",
                                genre = R.string.genre_news,
                                country = R.string.country_usa
                        ),
                        RadioStation(
                                id = "nrj",
                                name = "NRJ",
                                streamUrl = "https://scdn.nrjaudio.fm/adwz2/fr/30001/mp3_128.mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/da/NRJ_logo.svg/200px-NRJ_logo.svg.png",
                                logoResId = R.drawable.logo_nrj,
                                description = "Hit Music Only",
                                genre = R.string.genre_hits,
                                country = R.string.country_france
                        ),
                        RadioStation(
                                id = "onda_cero",
                                name = "Onda Cero",
                                streamUrl = "https://atres-live.ondacero.es/live/ondacero/bitrate_1.m3u8",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/96/Onda_Cero_logo.svg/200px-Onda_Cero_logo.svg.png",
                                logoResId = R.drawable.logo_onda_cero,
                                description = "Radio en español",
                                genre = R.string.genre_variety,
                                country = R.string.country_spain
                        ),
                        RadioStation(
                                id = "qmusic",
                                name = "Qmusic",
                                streamUrl = "https://icecast-qmusicnl-cdp.triple-it.nl/qmusic.mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/89/Qmusic_logo.svg/200px-Qmusic_logo.svg.png",
                                logoResId = R.drawable.logo_qmusic,
                                description = "Q sounds better with you",
                                genre = R.string.genre_hits,
                                country = R.string.country_netherlands
                        ),
                        RadioStation(
                                id = "radio_10",
                                name = "Radio 10",
                                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/RADIO10.mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0a/Radio_10_logo.svg/200px-Radio_10_logo.svg.png",
                                logoResId = R.drawable.logo_radio_10,
                                description = "Gewoon de beste hits",
                                genre = R.string.genre_oldies,
                                country = R.string.country_netherlands
                        ),
                        RadioStation(
                                id = "radio_105",
                                name = "Radio 105",
                                streamUrl = "https://icecast.unitedradio.it/Radio105.aac",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/39/Radio_105_logo.svg/200px-Radio_105_logo.svg.png",
                                logoResId = R.drawable.logo_radio_105,
                                description = "La radio che suona la musica",
                                genre = R.string.genre_hits,
                                country = R.string.country_italy
                        ),
                        RadioStation(
                                id = "radio_538",
                                name = "Radio 538",
                                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/RADIO538.mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/99/Radio_538_logo.svg/200px-Radio_538_logo.svg.png",
                                logoResId = R.drawable.logo_radio_538,
                                description = "De nummer 1 hits",
                                genre = R.string.genre_hits,
                                country = R.string.country_netherlands
                        ),
                        RadioStation(
                                id = "radio_deejay",
                                name = "Radio Deejay",
                                streamUrl = "https://streamcdnm24-dd782ed59e2a4e86aabf6fc508674b59.msvdn.net/live/S56912040/bMtRaShKbDaq/playlist.m3u8",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/47/Radio_DeeJay_logo.svg/200px-Radio_DeeJay_logo.svg.png",
                                logoResId = R.drawable.logo_radio_deejay,
                                description = "La radio che libera la musica",
                                genre = R.string.genre_hits,
                                country = R.string.country_italy
                        ),
                        RadioStation(
                                id = "radio_italia",
                                name = "Radio Italia",
                                streamUrl = "https://stream4.xdevel.com/audio0s976608-1379/stream/icecast.audio",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Logo_Radio_Italia.svg/200px-Logo_Radio_Italia.svg.png",
                                logoResId = R.drawable.logo_radio_italia,
                                description = "Solo musica italiana",
                                genre = R.string.genre_hits,
                                country = R.string.country_italy
                        ),
                        RadioStation(
                                id = "radio_nova",
                                name = "Radio Nova",
                                streamUrl = "https://novazz.ice.infomaniak.ch/novazz-128.mp3",
                                logoUrl = "https://www.nova.fr/wp-content/thumbnails/uploads/sites/2/2024/05/NOVA-nova-400x496-1-t-1700x1030.png",
                                logoResId = R.drawable.logo_radio_nova,
                                description = "Le Grand Mix de Radio Nova",
                                genre = R.string.genre_eclectic_world,
                                country = R.string.country_france
                        ),
                        RadioStation(
                                id = "radio_paradise",
                                name = "Radio Paradise",
                                streamUrl = "https://stream.radioparadise.com/mp3-128",
                                logoUrl = "https://www.radioparadise.com/graphics/logo_rp_75.png",
                                logoResId = 0,
                                description = "Eclectic blend of music from California",
                                genre = R.string.genre_eclectic,
                                country = R.string.country_usa
                        ),
                        RadioStation(
                                id = "rds",
                                name = "RDS",
                                streamUrl = "https://streamcdnm9-dd782ed59e2a4e86aabf6fc508674b59.msvdn.net/live/S44552214/uKpgwOHmle0S/playlist.m3u8",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8b/RDS_logo.svg/200px-RDS_logo.svg.png",
                                logoResId = R.drawable.logo_rds,
                                description = "Radio Dimensione Suono - 100% Grandi Successi",
                                genre = R.string.genre_hits,
                                country = R.string.country_italy
                        ),
                        RadioStation(
                                id = "rmc",
                                name = "RMC",
                                streamUrl = "https://audio.bfmtv.com/bfmbusiness/rmc_info-smil",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a9/RMC_logo.svg/200px-RMC_logo.svg.png",
                                logoResId = R.drawable.logo_rmc,
                                description = "Info Talk Sport",
                                genre = R.string.genre_news_talk,
                                country = R.string.country_france
                        ),
                        RadioStation(
                                id = "rtl",
                                name = "RTL",
                                streamUrl = "http://streaming.radio.rtl.fr/rtl-1-44-128",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2e/RTL-Logo.svg/200px-RTL-Logo.svg.png",
                                logoResId = R.drawable.logo_rtl,
                                description = "On a tous une bonne raison d'écouter RTL",
                                genre = R.string.genre_news_talk,
                                country = R.string.country_france
                        ),
                        RadioStation(
                                id = "rtl_102_5",
                                name = "RTL 102.5",
                                streamUrl = "https://streamcdnb4-dd782ed59e2a4e86aabf6fc508674b59.msvdn.net/live/S97044836/tbbP8T1ZRPBL/playlist_audio.m3u8",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4c/RTL_102.5_logo.svg/200px-RTL_102.5_logo.svg.png",
                                logoResId = R.drawable.logo_rtl_102_5,
                                description = "La radio italiana più ascoltata",
                                genre = R.string.genre_hits,
                                country = R.string.country_italy
                        ),
                        RadioStation(
                                id = "sky_radio",
                                name = "Sky Radio",
                                streamUrl = "https://playerservices.streamtheworld.com/api/livestream-redirect/SRGSTR01.mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/75/Sky_Radio_logo.svg/200px-Sky_Radio_logo.svg.png",
                                logoResId = R.drawable.logo_sky_radio,
                                description = "The feel good station",
                                genre = R.string.genre_adult_contemporary,
                                country = R.string.country_netherlands
                        ),
                        RadioStation(
                                id = "soma_drone",
                                name = "SomaFM Drone Zone",
                                streamUrl = "https://ice1.somafm.com/dronezone-128-mp3",
                                logoUrl = "https://somafm.com/img3/dronezone-400.jpg",
                                logoResId = 0,
                                description = "Atmospheric textures with minimal beats",
                                genre = R.string.genre_ambient,
                                country = R.string.country_usa
                        ),
                        RadioStation(
                                id = "soma_groove",
                                name = "SomaFM Groove Salad",
                                streamUrl = "https://ice1.somafm.com/groovesalad-128-mp3",
                                logoUrl = "https://somafm.com/img3/groovesalad-400.jpg",
                                logoResId = 0,
                                description = "A nicely chilled plate of ambient beats",
                                genre = R.string.genre_ambient_chill,
                                country = R.string.country_usa
                        ),
                        RadioStation(
                                id = "swr3",
                                name = "SWR3",
                                streamUrl = "https://liveradio.swr.de/sw282p3/swr3/play.mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6a/SWR3_Logo.svg/200px-SWR3_Logo.svg.png",
                                logoResId = R.drawable.logo_swr3,
                                description = "Pop und mehr",
                                genre = R.string.genre_pop_rock,
                                country = R.string.country_germany
                        ),
                        RadioStation(
                                id = "tsfjazz",
                                name = "TSF Jazz",
                                streamUrl = "https://tsfjazz.ice.infomaniak.ch/tsfjazz-high.mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/fr/thumb/4/44/TSF_Jazz_logo.svg/200px-TSF_Jazz_logo.svg.png",
                                logoResId = R.drawable.logo_tsfjazz,
                                description = "La radio jazz",
                                genre = R.string.genre_jazz,
                                country = R.string.country_france
                        ),
                        RadioStation(
                                id = "wdr1",
                                name = "WDR 1",
                                streamUrl = "https://wdr-1live-live.icecastssl.wdr.de/wdr/1live/live/mp3/128/stream.mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d1/1LIVE_Logo.svg/200px-1LIVE_Logo.svg.png",
                                logoResId = R.drawable.logo_wdr1,
                                description = "Das junge Radio des WDR",
                                genre = R.string.genre_pop_rock,
                                country = R.string.country_germany
                        ),
                        RadioStation(
                                id = "wdr_2",
                                name = "WDR 2",
                                streamUrl = "https://wdr-wdr2-rheinland.icecastssl.wdr.de/wdr/wdr2/rheinland/mp3/128/stream.mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/58/WDR_2_logo.svg/200px-WDR_2_logo.svg.png",
                                logoResId = R.drawable.logo_wdr_2,
                                description = "Musik, Infos, Podcasts",
                                genre = R.string.genre_adult_contemporary,
                                country = R.string.country_germany
                        ),
                        RadioStation(
                                id = "wdr3",
                                name = "WDR 3",
                                streamUrl = "https://wdr-wdr3-live.icecastssl.wdr.de/wdr/wdr3/live/mp3/128/stream.mp3",
                                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e6/WDR_3_logo_2012.svg/200px-WDR_3_logo_2012.svg.png",
                                logoResId = R.drawable.logo_wdr3,
                                description = "Kultur und Klassik",
                                genre = R.string.genre_classical_culture,
                                country = R.string.country_germany
                        ),
                        RadioStation(
                                id = "wnyc_fm",
                                name = "WNYC 93.9 FM",
                                streamUrl = "http://fm939.wnyc.org/wnycfm.mp3",
                                logoUrl = "https://cdn-radiotime-logos.tunein.com/s21606q.png",
                                logoResId = 0,
                                description = "New York's flagship public radio station",
                                genre = R.string.genre_news_talk,
                                country = R.string.country_usa
                        ),
                        RadioStation(
                                id = "wqxr",
                                name = "WQXR 105.9 FM",
                                streamUrl = "http://stream.wqxr.org/wqxr.mp3",
                                logoUrl = "https://cdn-radiotime-logos.tunein.com/s27341q.png",
                                logoResId = R.drawable.logo_wqxr,
                                description = "New York's only dedicated classical station",
                                genre = R.string.genre_classical,
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
