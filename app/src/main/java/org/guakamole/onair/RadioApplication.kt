package org.guakamole.onair

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import org.guakamole.onair.data.RadioRepository

class RadioApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        RadioRepository.initialize(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
                .components { add(SvgDecoder.Factory()) }
                .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
                .diskCache {
                    DiskCache.Builder()
                            .directory(this.cacheDir.resolve("image_cache"))
                            .maxSizePercent(0.02)
                            .build()
                }
                .respectCacheHeaders(false) // Radio logos don't change often, keep them cached
                .build()
    }
}
