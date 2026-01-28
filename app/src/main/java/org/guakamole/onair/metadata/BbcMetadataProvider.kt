package org.guakamole.onair.metadata

import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class BbcMetadataProvider : MetadataProvider {
    override suspend fun fetchMetadata(param: String): MetadataResult? =
            withContext(Dispatchers.IO) {
                try {
                    val url = URL("https://rms.api.bbc.co.uk/v2/services/$param/segments/latest")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000

                    if (connection.responseCode == 200) {
                        val response = Scanner(connection.inputStream).useDelimiter("\\A").next()
                        val json = JSONObject(response)
                        val dataArray = json.getJSONArray("data")
                        if (dataArray.length() > 0) {
                            val latest = dataArray.getJSONObject(0)
                            if (latest.optJSONObject("offset")?.optBoolean("now_playing") == true) {
                                val titles = latest.getJSONObject("titles")
                                val artist = titles.optString("primary")
                                val title = titles.optString("secondary")

                                if (!title.isNullOrBlank()) {
                                    return@withContext MetadataResult(
                                            artist = if (artist.isNullOrBlank()) null else artist,
                                            title = title,
                                            type = MetadataType.SONG
                                    )
                                }
                            }
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    android.util.Log.e(
                            "BbcMetadataProvider",
                            "Error polling BBC metadata for $param",
                            e
                    )
                }
                return@withContext null
            }
}
