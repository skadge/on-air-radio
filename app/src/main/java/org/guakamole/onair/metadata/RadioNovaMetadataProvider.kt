package org.guakamole.onair.metadata

import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class RadioNovaMetadataProvider : MetadataProvider {
    override suspend fun fetchMetadata(param: String): MetadataResult? =
            withContext(Dispatchers.IO) {
                try {
                    val url = URL("https://www.nova.fr/radios-data/www.nova.fr/all.json")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.setRequestProperty("User-Agent", "OnAir Radio/1.0 (Android)")

                    if (connection.responseCode == 200) {
                        val response = Scanner(connection.inputStream).useDelimiter("\\A").next()
                        val jsonArray = JSONArray(response)

                        for (i in 0 until jsonArray.length()) {
                            val entry = jsonArray.getJSONObject(i)
                            val radio = entry.optJSONObject("radio") ?: continue
                            val code = radio.optString("code")
                            val name = radio.optString("name")

                            if (code == param || name == param) {
                                val currentTrack = entry.optJSONObject("currentTrack")
                                if (currentTrack != null) {
                                    val artist = currentTrack.optString("artist")
                                    val title = currentTrack.optString("title")

                                    android.util.Log.d(
                                            "MetadataDebug",
                                            "RadioNovaProvider: Found track for $param: $artist - $title"
                                    )
                                    return@withContext MetadataResult(
                                            artist = if (artist.isNullOrBlank()) null else artist,
                                            title = if (title.isNullOrBlank()) null else title,
                                            type = MetadataType.SONG
                                    )
                                } else {
                                    // Fallback to currentShow if it's a talk program
                                    val currentShow = entry.optJSONObject("currentShow")

                                    if (currentShow != null) {
                                        val author = currentShow.optString("author")
                                        val title = currentShow.optString("title")

                                        android.util.Log.d(
                                                "MetadataDebug",
                                                "RadioNovaProvider: Found show for $param: $author - $title"
                                        )
                                        return@withContext MetadataResult(
                                                artist =
                                                        if (author.isNullOrBlank()) null
                                                        else author,
                                                title = if (title.isNullOrBlank()) null else title,
                                                type = MetadataType.PROGRAM
                                        )
                                    } else {
                                        android.util.Log.d(
                                                "MetadataDebug",
                                                "RadioNovaProvider: No track or show found for $param"
                                        )
                                        // Return station name ensuring we clear old metadata
                                        return@withContext MetadataResult(
                                                artist = null,
                                                title = name,
                                                type = MetadataType.PROGRAM
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        android.util.Log.e(
                                "MetadataDebug",
                                "RadioNovaProvider: Failed to fetch: HTTP ${connection.responseCode}"
                        )
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    android.util.Log.e(
                            "MetadataDebug",
                            "RadioNovaProvider: Error polling Radio Nova for $param",
                            e
                    )
                }
                return@withContext null
            }
}
