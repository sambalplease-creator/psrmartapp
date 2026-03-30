package com.psrmart.app

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache

class PSRApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Configure Coil with sensible caching limits
        // Without this, Coil uses unlimited memory which causes jank with many product photos
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20) // Use at most 20% of available RAM for image cache
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50MB disk cache for product photos
                    .build()
            }
            .crossfade(true)      // Smooth image loads
            .respectCacheHeaders(false)
            .build()

        Coil.setImageLoader(imageLoader)
    }
}
