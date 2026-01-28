package org.guakamole.onair.metadata

/** Type of content being streamed */
enum class MetadataType {
    SONG,
    PROGRAM,
    UNKNOWN
}

/** Result of a metadata fetch operation */
data class MetadataResult(
        val artist: String?,
        val title: String?,
        val artworkUrl: String? = null,
        val type: MetadataType = MetadataType.UNKNOWN
)

/** Interface for custom metadata providers (e.g., API polling) */
interface MetadataProvider {
    /**
     * Fetches metadata for the given parameter
     * @param param The provider-specific parameter (e.g., station ID or API endpoint)
     * @return The fetched metadata or null if not available/error
     */
    suspend fun fetchMetadata(param: String): MetadataResult?
}
