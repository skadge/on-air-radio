package org.guakamole.worldradio.data

/**
 * Repository providing a curated list of public radio stations
 */
object RadioRepository {
    
    val stations: List<RadioStation> = listOf(
        // International
        RadioStation(
            id = "bbc_radio1",
            name = "BBC Radio 1",
            streamUrl = "http://stream.live.vc.bbcmedia.co.uk/bbc_radio_one",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f8/BBC_Radio_1.svg/200px-BBC_Radio_1.svg.png",
            description = "The best new music and entertainment",
            genre = "Pop/Rock",
            country = "UK"
        ),
        RadioStation(
            id = "bbc_radio2",
            name = "BBC Radio 2",
            streamUrl = "http://stream.live.vc.bbcmedia.co.uk/bbc_radio_two",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9f/BBC_Radio_2.svg/200px-BBC_Radio_2.svg.png",
            description = "Great music, great variety",
            genre = "Adult Contemporary",
            country = "UK"
        ),
        RadioStation(
            id = "bbc_radio4",
            name = "BBC Radio 4",
            streamUrl = "http://stream.live.vc.bbcmedia.co.uk/bbc_radio_fourfm",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4d/BBC_Radio_4.svg/200px-BBC_Radio_4.svg.png",
            description = "Intelligent speech radio",
            genre = "News/Talk",
            country = "UK"
        ),
        RadioStation(
            id = "classic_fm",
            name = "Classic FM",
            streamUrl = "https://media-ice.musicradio.com/ClassicFMMP3",
            logoUrl = "https://upload.wikimedia.org/wikipedia/en/thumb/b/b2/Classic_FM_UK.svg/200px-Classic_FM_UK.svg.png",
            description = "The world's greatest music",
            genre = "Classical",
            country = "UK"
        ),
        
        // France
        RadioStation(
            id = "france_inter",
            name = "France Inter",
            streamUrl = "https://icecast.radiofrance.fr/franceinter-midfi.mp3",
            logoUrl = "https://upload.wikimedia.org/wikipedia/fr/thumb/7/7d/France_Inter_logo_2021.svg/200px-France_Inter_logo_2021.svg.png",
            description = "La radio de service public",
            genre = "Talk/Music",
            country = "France"
        ),
        RadioStation(
            id = "france_culture",
            name = "France Culture",
            streamUrl = "https://icecast.radiofrance.fr/franceculture-midfi.mp3",
            logoUrl = "https://upload.wikimedia.org/wikipedia/fr/thumb/9/9d/France_Culture_logo_2021.svg/200px-France_Culture_logo_2021.svg.png",
            description = "Culture et savoirs",
            genre = "Culture",
            country = "France"
        ),
        RadioStation(
            id = "france_musique",
            name = "France Musique",
            streamUrl = "https://icecast.radiofrance.fr/francemusique-midfi.mp3",
            logoUrl = "https://upload.wikimedia.org/wikipedia/fr/thumb/7/72/France_Musique_logo_2021.svg/200px-France_Musique_logo_2021.svg.png",
            description = "La musique classique et plus",
            genre = "Classical",
            country = "France"
        ),
        RadioStation(
            id = "fip",
            name = "FIP",
            streamUrl = "https://icecast.radiofrance.fr/fip-midfi.mp3",
            logoUrl = "https://upload.wikimedia.org/wikipedia/fr/thumb/7/77/FIP_logo_2021.svg/200px-FIP_logo_2021.svg.png",
            description = "Éclectisme musical",
            genre = "Eclectic",
            country = "France"
        ),
        
        // USA
        RadioStation(
            id = "npr",
            name = "NPR News",
            streamUrl = "https://npr-ice.streamguys1.com/live.mp3",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d7/National_Public_Radio_logo.svg/200px-National_Public_Radio_logo.svg.png",
            description = "National Public Radio",
            genre = "News",
            country = "USA"
        ),
        RadioStation(
            id = "kexp",
            name = "KEXP 90.3",
            streamUrl = "https://kexp-mp3-128.streamguys1.com/kexp128.mp3",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/69/KEXP_logo.svg/200px-KEXP_logo.svg.png",
            description = "Where the music matters",
            genre = "Alternative",
            country = "USA"
        ),
        
        // Germany
        RadioStation(
            id = "wdr3",
            name = "WDR 3",
            streamUrl = "https://wdr-wdr3-live.icecastssl.wdr.de/wdr/wdr3/live/mp3/128/stream.mp3",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e6/WDR_3_logo_2012.svg/200px-WDR_3_logo_2012.svg.png",
            description = "Kultur und Klassik",
            genre = "Classical/Culture",
            country = "Germany"
        ),
        RadioStation(
            id = "deutschlandfunk",
            name = "Deutschlandfunk",
            streamUrl = "https://st01.sslstream.dlf.de/dlf/01/128/mp3/stream.mp3",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/14/Deutschlandfunk_Logo_2017.svg/200px-Deutschlandfunk_Logo_2017.svg.png",
            description = "Nachrichten und Kultur",
            genre = "News/Culture",
            country = "Germany"
        ),
        
        // Jazz
        RadioStation(
            id = "jazz_radio",
            name = "Jazz Radio",
            streamUrl = "https://jazzradio.ice.infomaniak.ch/jazzradio-high.mp3",
            logoUrl = "https://upload.wikimedia.org/wikipedia/fr/thumb/2/24/Logo_Jazz_Radio.svg/200px-Logo_Jazz_Radio.svg.png",
            description = "100% Jazz",
            genre = "Jazz",
            country = "France"
        ),
        RadioStation(
            id = "tsfjazz",
            name = "TSF Jazz",
            streamUrl = "https://tsfjazz.ice.infomaniak.ch/tsfjazz-high.mp3",
            logoUrl = "https://upload.wikimedia.org/wikipedia/fr/thumb/4/44/TSF_Jazz_logo.svg/200px-TSF_Jazz_logo.svg.png",
            description = "La radio jazz",
            genre = "Jazz",
            country = "France"
        ),
        
        // World
        RadioStation(
            id = "soma_groove",
            name = "SomaFM Groove Salad",
            streamUrl = "https://ice1.somafm.com/groovesalad-128-mp3",
            logoUrl = "https://somafm.com/img3/groovesalad-400.jpg",
            description = "A nicely chilled plate of ambient beats",
            genre = "Ambient/Chill",
            country = "USA"
        ),
        RadioStation(
            id = "soma_drone",
            name = "SomaFM Drone Zone",
            streamUrl = "https://ice1.somafm.com/dronezone-128-mp3",
            logoUrl = "https://somafm.com/img3/dronezone-400.jpg",
            description = "Atmospheric textures with minimal beats",
            genre = "Ambient",
            country = "USA"
        )
    )
    
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
