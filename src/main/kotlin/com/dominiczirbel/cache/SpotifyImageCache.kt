package com.dominiczirbel.cache

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.dominiczirbel.Logger
import com.dominiczirbel.network.Spotify
import com.dominiczirbel.network.await
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.skija.Image
import java.io.File
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

sealed class ImageCacheEvent {
    data class Hit(val url: String, val loadDuration: Duration, val cacheFile: File) : ImageCacheEvent()
    data class Miss(val url: String) : ImageCacheEvent()
    data class Fetch(val url: String, val fetchDuration: Duration, val writeDuration: Duration?, val cacheFile: File?) :
        ImageCacheEvent()
}

/**
 * A simple disk cache for images loaded from Spotify's image CDN.
 */
object SpotifyImageCache {
    private const val SPOTIFY_IMAGE_URL_PREFIX = "https://i.scdn.co/image/"
    private val IMAGES_DIR by lazy {
        SpotifyCache.CACHE_DIR.resolve("images")
            .also { it.mkdirs() }
            .also { require(it.isDirectory) { "could not create image cache directory $it" } }
    }

    /**
     * Retrieves the given [url] as an [ImageBitmap], loading it from a cached file if possible.
     *
     * TODO handle concurrent reads of the same url in a single network call
     */
    suspend fun get(url: String, client: OkHttpClient = Spotify.configuration.okHttpClient): ImageBitmap? {
        var cacheFile: File? = null
        if (url.startsWith(SPOTIFY_IMAGE_URL_PREFIX)) {
            val imageHash = url.substring(SPOTIFY_IMAGE_URL_PREFIX.length)
            cacheFile = IMAGES_DIR.resolve(imageHash)

            if (cacheFile.isFile) {
                val (image, duration) = measureTimedValue {
                    Image.makeFromEncoded(cacheFile.readBytes()).asImageBitmap()
                }

                Logger.ImageCache.handleImageCacheEvent(
                    ImageCacheEvent.Hit(url = url, loadDuration = duration, cacheFile = cacheFile)
                )
                return image
            }
        }

        Logger.ImageCache.handleImageCacheEvent(ImageCacheEvent.Miss(url = url))
        val fetchStart = TimeSource.Monotonic.markNow()

        val request = Request.Builder().url(url).build()

        // TODO error handling
        return client.newCall(request).await()
            .use { response ->
                // TODO blocking method call
                response.body?.bytes()
            }
            ?.let { bytes ->
                val image = Image.makeFromEncoded(bytes).asImageBitmap()
                val fetchDuration = fetchStart.elapsedNow()

                val writeDuration = cacheFile?.let { measureTime { cacheFile.writeBytes(bytes) } }

                Logger.ImageCache.handleImageCacheEvent(
                    ImageCacheEvent.Fetch(
                        url = url,
                        fetchDuration = fetchDuration,
                        writeDuration = writeDuration,
                        cacheFile = cacheFile
                    )
                )

                image
            }
    }
}
