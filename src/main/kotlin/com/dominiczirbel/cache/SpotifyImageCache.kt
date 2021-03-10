package com.dominiczirbel.cache

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.dominiczirbel.network.Spotify
import com.dominiczirbel.network.await
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.skija.Image
import java.io.File

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
                return Image.makeFromEncoded(cacheFile.readBytes()).asImageBitmap()
            }
        }

        val request = Request.Builder().url(url).build()

        // TODO error handling
        return client.newCall(request).await()
            .use { response ->
                // TODO blocking method call
                response.body?.bytes()
            }
            ?.let { bytes ->
                cacheFile?.writeBytes(bytes)
                Image.makeFromEncoded(bytes).asImageBitmap()
            }
    }
}
