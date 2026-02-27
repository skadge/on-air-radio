package org.guakamole.onair.metadata

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
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
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
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

                            // Look for the best release to fetch artwork from.
                            // Prefer singles/original albums over compilations.
                            val releases = recording.optJSONArray("releases")
                            val artworkUrl =
                                    if (releases != null && releases.length() > 0) {
                                        val bestRelease = pickBestRelease(releases)
                                        val releaseId = bestRelease.optString("id")
                                        if (!releaseId.isNullOrBlank()) {
                                            android.util.Log.d(
                                                    "MetadataRefiner",
                                                    "Selected release: ${bestRelease.optString("title")} (type: ${bestRelease.optJSONObject("release-group")?.optString("primary-type")})"
                                            )
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
    /**
     * Pick the best release from a JSONArray of releases. Prefers singles and original albums over
     * compilations.
     */
    private fun pickBestRelease(releases: JSONArray): JSONObject {
        var bestRelease = releases.getJSONObject(0)
        var bestScore = releaseScore(bestRelease)

        for (i in 1 until releases.length()) {
            val release = releases.getJSONObject(i)
            val score = releaseScore(release)
            if (score > bestScore) {
                bestScore = score
                bestRelease = release
            }
        }
        return bestRelease
    }

    /**
     * Score a release for cover art preference. Higher = more likely to be the "original" release.
     */
    private fun releaseScore(release: JSONObject): Int {
        val releaseGroup = release.optJSONObject("release-group") ?: return 0
        val primaryType = releaseGroup.optString("primary-type", "").lowercase()
        val secondaryTypes = releaseGroup.optJSONArray("secondary-types")
        val secondaryList = mutableListOf<String>()
        if (secondaryTypes != null) {
            for (i in 0 until secondaryTypes.length()) {
                secondaryList.add(secondaryTypes.optString(i).lowercase())
            }
        }

        val isCompilation = "compilation" in secondaryList
        val hasSecondary = secondaryList.isNotEmpty()

        return when {
            primaryType == "album" && !hasSecondary -> 4
            primaryType == "single" && !hasSecondary -> 3
            primaryType == "album" && !isCompilation -> 2
            !isCompilation -> 1
            else -> 0 // compilation — least preferred
        }
    }
}
