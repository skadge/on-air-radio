package org.guakamole.onair.report

import android.os.Build
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.guakamole.onair.BuildConfig
import org.guakamole.onair.service.PlaybackError
import org.json.JSONObject

object ReportManager {
    private const val REPORT_URL = "http://apps.guakamole.org/report"

    suspend fun reportIssue(error: PlaybackError): Boolean =
            withContext(Dispatchers.IO) {
                try {
                    val url = URL(REPORT_URL)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json; utf-8")
                    conn.setRequestProperty("Accept", "application/json")
                    conn.doOutput = true

                    val deviceInfo =
                            "${Build.MANUFACTURER} ${Build.MODEL} - API ${Build.VERSION.SDK_INT}"
                    val country = Locale.getDefault().country ?: "unknown"

                    val jsonBody =
                            JSONObject().apply {
                                put("station_id", error.stationId ?: "unknown")
                                put("stream_url", error.streamUrl ?: "unknown")
                                put("error_code", error.errorCode)
                                put("error_message", error.message)
                                put("device_info", deviceInfo)
                                put("app_version", BuildConfig.VERSION_NAME)
                                put("user_agent", error.userAgent ?: "unknown")
                                put("country", country)
                            }

                    conn.outputStream.use { os ->
                        val input = jsonBody.toString().toByteArray(Charsets.UTF_8)
                        os.write(input, 0, input.size)
                    }

                    val responseCode = conn.responseCode
                    return@withContext responseCode in 200..299
                } catch (e: Exception) {
                    android.util.Log.e("ReportManager", "Error reporting issue", e)
                    false
                }
            }
}
