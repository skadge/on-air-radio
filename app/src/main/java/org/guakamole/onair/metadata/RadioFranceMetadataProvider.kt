package org.guakamole.onair.metadata

import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class RadioFranceMetadataProvider : MetadataProvider {
    override suspend fun fetchMetadata(param: String): MetadataResult? =
            withContext(Dispatchers.IO) {
                try {
                    val url = URL("https://api.radiofrance.fr/livemeta/pull/$param")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000

                    if (connection.responseCode == 200) {
                        val response = Scanner(connection.inputStream).useDelimiter("\\A").next()
                        val json = JSONObject(response)

                        val steps = json.optJSONObject("steps") ?: return@withContext null
                        val levels = json.optJSONArray("levels") ?: return@withContext null

                        if (levels.length() > 0) {
                            val items = levels.getJSONObject(0).optJSONArray("items")
                            if (items != null && items.length() > 0) {
                                // The last item in the list is usually the currently playing one
                                val lastStepId = items.getString(items.length() - 1)
                                val activeStep = steps.optJSONObject(lastStepId)

                                if (activeStep != null) {
                                    val embedType = activeStep.optString("embedType")
                                    val title = activeStep.optString("title")

                                    return@withContext if (embedType == "song") {
                                        val artist = activeStep.optString("authors")
                                        MetadataResult(
                                                artist =
                                                        if (artist.isNullOrBlank()) null
                                                        else artist,
                                                title = if (title.isNullOrBlank()) null else title
                                        )
                                    } else {
                                        // For programs/expressions
                                        val concept = activeStep.optString("titleConcept")
                                        MetadataResult(
                                                artist =
                                                        if (concept.isNullOrBlank()) null
                                                        else concept,
                                                title = if (title.isNullOrBlank()) null else title
                                        )
                                    }
                                }
                            }
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    android.util.Log.e(
                            "RadioFranceProvider",
                            "Error polling Radio France for $param",
                            e
                    )
                }
                return@withContext null
            }
}
