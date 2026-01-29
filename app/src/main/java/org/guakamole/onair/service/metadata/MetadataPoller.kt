package org.guakamole.onair.service.metadata

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.guakamole.onair.data.RadioRepository
import org.guakamole.onair.metadata.MetadataProviderFactory
import org.guakamole.onair.metadata.MetadataResult

/** Handles periodic polling of metadata from external providers. */
class MetadataPoller(private val scope: CoroutineScope) {
    private var pollingJob: Job? = null
    private var currentPollingType: String? = null
    private var currentPollingParam: String? = null

    private val _metadataUpdates = MutableSharedFlow<MetadataResult>()
    val metadataUpdates: SharedFlow<MetadataResult> = _metadataUpdates.asSharedFlow()

    fun startPolling(stationId: String?) {
        val station = stationId?.let { RadioRepository.getStationById(it) }
        val type = station?.metadataType
        val param = station?.metadataParam

        if (type == null || param == null) {
            stopPolling()
            return
        }

        // If we're already polling this with the same param, don't restart
        if (type == currentPollingType &&
                        param == currentPollingParam &&
                        pollingJob?.isActive == true
        ) {
            return
        }

        stopPolling()
        currentPollingType = type
        currentPollingParam = param

        val provider = MetadataProviderFactory.getProvider(type)
        if (provider == null) {
            android.util.Log.w("MetadataDebug", "MetadataPoller: No provider found for type: $type")
            return
        }

        android.util.Log.d(
                "MetadataDebug",
                "MetadataPoller: Starting polling ($type) with param: $param"
        )
        pollingJob =
                scope.launch(Dispatchers.IO) {
                    while (isActive) {
                        try {
                            val result = provider.fetchMetadata(param)
                            if (result != null) {
                                android.util.Log.d(
                                        "MetadataDebug",
                                        "MetadataPoller: Polled: ${result.artist} - ${result.title} (type=${result.type})"
                                )
                                _metadataUpdates.emit(result)
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            android.util.Log.e(
                                    "MetadataDebug",
                                    "MetadataPoller: Error polling ($type)",
                                    e
                            )
                        }
                        delay(5000)
                    }
                }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        currentPollingType = null
        currentPollingParam = null
    }
}
