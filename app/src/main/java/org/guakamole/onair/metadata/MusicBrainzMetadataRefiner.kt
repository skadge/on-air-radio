package org.guakamole.onair.metadata

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Scanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object MusicBrainzMetadataRefiner {
    private const val SEARCH_BASE_URL = "https://musicbrainz.org/ws/2/recording/"
    private const val COVER_ART_BASE_URL = "https://coverartarchive.org/release/"
    private const val USER_AGENT = "OnAirRadio/1.0 ( https://github.com/guakamole/on-air-radio )"

    suspend fun refine(artist: String?, title: String?, type: MetadataType): MetadataResult? =
            withContext(Dispatchers.IO) {
                if (artist.isNullOrBlank() || title.isNullOrBlank()) return@withContext null

                // No point in searching for artwork if it's not a song
                if (type != MetadataType.SONG) {
                    return@withContext MetadataResult(artist, title, null, type)
                }

                try {
                    // Search for recording
                    val query = "recording:\"$title\" AND artist:\"$artist\""
                    android.util.Log.d(
                            "MetadataRefiner",
                            "Searching MusicBrainz with query: $query"
                    )
                    val encodedQuery = URLEncoder.encode(query, "UTF-8")
                    val url = URL("$SEARCH_BASE_URL?query=$encodedQuery&fmt=json")

                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.setRequestProperty("User-Agent", USER_AGENT)

                    if (connection.responseCode == 200) {
                        val response = Scanner(connection.inputStream).useDelimiter("\\A").next()
                        val json = JSONObject(response)
                        val recordings = json.optJSONArray("recordings")

                        if (recordings != null && recordings.length() > 0) {
                            val recording = recordings.getJSONObject(0)

                            // Standardize artist and title from MusicBrainz
                            val refinedTitle = recording.optString("title")
                            val artistCredit = recording.optJSONArray("artist-credit")
                            val refinedArtist =
                                    if (artistCredit != null && artistCredit.length() > 0) {
                                        artistCredit.getJSONObject(0).optString("name")
                                    } else {
                                        null
                                    }

                            // Look for release MBID to fetch artwork
                            val releases = recording.optJSONArray("releases")
                            val artworkUrl =
                                    if (releases != null && releases.length() > 0) {
                                        val releaseId = releases.getJSONObject(0).optString("id")
                                        if (!releaseId.isNullOrBlank()) {
                                            // Using the front-500 thumbnail size from Cover Art
                                            // Archive
                                            "$COVER_ART_BASE_URL$releaseId/front-500"
                                        } else null
                                    } else null

                            android.util.Log.d(
                                    "MetadataRefiner",
                                    "Refined result: artist=$refinedArtist, title=$refinedTitle, artwork=$artworkUrl"
                            )
                            return@withContext MetadataResult(
                                    artist =
                                            if (refinedArtist.isNullOrBlank()) artist
                                            else refinedArtist,
                                    title =
                                            if (refinedTitle.isNullOrBlank()) title
                                            else refinedTitle,
                                    artworkUrl = artworkUrl,
                                    type = MetadataType.SONG
                            )
                        } else {
                            android.util.Log.d("MetadataRefiner", "No recordings found for query")
                        }
                    } else {
                        android.util.Log.e(
                                "MetadataRefiner",
                                "Search failed: HTTP ${connection.responseCode}"
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e(
                            "MetadataRefiner",
                            "Error refining metadata with MusicBrainz",
                            e
                    )
                }

                return@withContext MetadataResult(artist, title, null, type)
            }
}
