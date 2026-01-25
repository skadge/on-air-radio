package org.guakamole.onair.metadata

/** Factory for retrieving MetadataProvider instances */
object MetadataProviderFactory {

    private val providers = mapOf<String, MetadataProvider>("bbc_rms" to BbcMetadataProvider())

    /** Returns the provider for the given type, or null if not supported */
    fun getProvider(type: String): MetadataProvider? {
        return providers[type]
    }
}
