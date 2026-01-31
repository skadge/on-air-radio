package org.guakamole.onair.service

import androidx.media3.common.C
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import java.io.IOException

/**
 * Custom load error handling policy for radio streams. Retries network errors for up to 15 seconds
 * with exponential backoff, then surfaces the error to pause playback.
 */
class RadioLoadErrorHandlingPolicy : DefaultLoadErrorHandlingPolicy() {

    companion object {
        private const val MAX_RETRY_DURATION_MS = 15_000L // 15 seconds
        private const val INITIAL_RETRY_DELAY_MS = 1_000L // 1 second
        private const val MAX_RETRY_DELAY_MS = 4_000L // 4 seconds max between retries
    }

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val exception = loadErrorInfo.exception
        val errorCount = loadErrorInfo.errorCount

        // Calculate total time spent retrying (approximate based on error count and delays)
        val estimatedTotalRetryTime = calculateEstimatedRetryTime(errorCount)

        // Only retry network-related errors
        if (isRetryableNetworkError(exception)) {
            if (estimatedTotalRetryTime < MAX_RETRY_DURATION_MS) {
                // Exponential backoff: 1s, 2s, 4s, 4s, 4s...
                val delay =
                        minOf(
                                INITIAL_RETRY_DELAY_MS * (1L shl (errorCount - 1).coerceAtMost(2)),
                                MAX_RETRY_DELAY_MS
                        )
                android.util.Log.d(
                        "RadioLoadError",
                        "Retrying after ${delay}ms (attempt $errorCount, ~${estimatedTotalRetryTime}ms elapsed)"
                )
                return delay
            } else {
                android.util.Log.w(
                        "RadioLoadError",
                        "Max retry duration exceeded ($estimatedTotalRetryTime >= $MAX_RETRY_DURATION_MS), giving up"
                )
                return C.TIME_UNSET
            }
        }

        // For non-network errors, use default behavior
        return super.getRetryDelayMsFor(loadErrorInfo)
    }

    private fun isRetryableNetworkError(exception: IOException): Boolean {
        // Retry HTTP connection errors and timeouts
        return exception is HttpDataSource.HttpDataSourceException ||
                exception is java.net.SocketTimeoutException ||
                exception is java.net.ConnectException ||
                exception is java.net.UnknownHostException ||
                exception.message?.contains("timeout", ignoreCase = true) == true ||
                exception.message?.contains("connect", ignoreCase = true) == true
    }

    private fun calculateEstimatedRetryTime(errorCount: Int): Long {
        // Sum of delays: 1s + 2s + 4s + 4s + ...
        var total = 0L
        for (i in 1 until errorCount) {
            total +=
                    minOf(
                            INITIAL_RETRY_DELAY_MS * (1L shl (i - 1).coerceAtMost(2)),
                            MAX_RETRY_DELAY_MS
                    )
        }
        return total
    }
}
