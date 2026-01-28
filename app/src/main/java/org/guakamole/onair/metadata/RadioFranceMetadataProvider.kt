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
                        val now = System.currentTimeMillis() / 1000

                        var bestStep: JSONObject? = null
                        var maxDepth = -1

                        steps.keys().forEach { key ->
                            val step = steps.getJSONObject(key)
                            val start = step.optLong("start")
                            val end = step.optLong("end", Long.MAX_VALUE)

                            if (now in start until end) {
                                val depth = step.optInt("depth", 0)
                                if (depth > maxDepth) {
                                    maxDepth = depth
                                    bestStep = step
                                }
                            }
                        }

                        bestStep?.let { activeStep ->
                            val embedType = activeStep.optString("embedType")
                            val title = activeStep.optString("title")

                            return@withContext if (embedType == "song") {
                                val artist = activeStep.optString("authors")
                                MetadataResult(
                                        artist = if (artist.isNullOrBlank()) null else artist,
                                        title = if (title.isNullOrBlank()) null else title,
                                        type = MetadataType.SONG
                                )
                            } else {
                                // For programs/expressions
                                val concept = activeStep.optString("titleConcept")
                                MetadataResult(
                                        artist = if (concept.isNullOrBlank()) null else concept,
                                        title = if (title.isNullOrBlank()) null else title,
                                        type = MetadataType.PROGRAM
                                )
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
