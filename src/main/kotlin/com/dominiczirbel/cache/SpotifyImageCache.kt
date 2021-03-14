package com.dominiczirbel.cache

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.dominiczirbel.Logger
import com.dominiczirbel.network.Spotify
import com.dominiczirbel.network.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.skija.Image
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.measureTimedValue

sealed class ImageCacheEvent {
    data class InMemory(val url: String) : ImageCacheEvent()
    data class OnDisk(val url: String, val duration: Duration, val cacheFile: File) : ImageCacheEvent()
    data class Fetch(val url: String, val duration: Duration, val cacheFile: File?) : ImageCacheEvent()
}

/**
 * A simple disk cache for images loaded from Spotify's image CDN.
 */
object SpotifyImageCache {
    /**
     * Represents the current state of the image cache.
     *
     * Creating a new object with the default values reflects the current state.
     *
     * We use a single object for this rather than many [androidx.compose.runtime.MutableState] instances so that
     * updates only trigger a single recomposition.
     */
    data class State(
        val inMemoryCount: Int = totalCompleted.get(),
        val diskCount: Int = IMAGES_DIR.list()?.size ?: 0,
        val totalDiskSize: Int = IMAGES_DIR.listFiles()?.sumBy { it.length().toInt() } ?: 0
    )

    private const val SPOTIFY_IMAGE_URL_PREFIX = "https://i.scdn.co/image/"
    private val IMAGES_DIR by lazy {
        SpotifyCache.CACHE_DIR.resolve("images")
            .also { it.mkdirs() }
            .also { require(it.isDirectory) { "could not create image cache directory $it" } }
    }

    private val imageJobs: MutableMap<String, Deferred<ImageBitmap?>> = ConcurrentHashMap()

    private var totalCompleted = AtomicInteger()

    var state by mutableStateOf(State())
        private set

    fun clear() {
        GlobalScope.launch {
            imageJobs.clear()
            totalCompleted.set(0)
            IMAGES_DIR.deleteRecursively()
            state = State()
        }
    }

    suspend fun get(
        url: String,
        scope: CoroutineScope = GlobalScope,
        client: OkHttpClient = Spotify.configuration.okHttpClient
    ): ImageBitmap? {
        val deferred = imageJobs.getOrPut(url) {
            scope.async {
                val (result, duration) = measureTimedValue { fromFileCache(url) }
                val (cacheFile, image) = result

                if (image != null) {
                    image.also {
                        Logger.ImageCache.handleImageCacheEvent(
                            ImageCacheEvent.OnDisk(url = url, duration = duration, cacheFile = cacheFile!!)
                        )
                    }
                } else {
                    val (image2, duration2) = measureTimedValue {
                        fromRemote(url = url, cacheFile = cacheFile, client = client)
                    }

                    image2?.also {
                        Logger.ImageCache.handleImageCacheEvent(
                            ImageCacheEvent.Fetch(url = url, duration = duration2, cacheFile = cacheFile)
                        )
                    }
                }
            }.also {
                it.invokeOnCompletion {
                    totalCompleted.incrementAndGet()
                    state = State()
                }
            }
        }

        if (deferred.isCompleted) {
            Logger.ImageCache.handleImageCacheEvent(ImageCacheEvent.InMemory(url = url))
        }

        return deferred.await()
    }

    private fun fromFileCache(url: String): Pair<File?, ImageBitmap?> {
        var cacheFile: File? = null
        if (url.startsWith(SPOTIFY_IMAGE_URL_PREFIX)) {
            val imageHash = url.substring(SPOTIFY_IMAGE_URL_PREFIX.length)
            cacheFile = IMAGES_DIR.resolve(imageHash)

            if (cacheFile.isFile) {
                return Pair(cacheFile, Image.makeFromEncoded(cacheFile.readBytes()).asImageBitmap())
            }
        }

        return Pair(cacheFile, null)
    }

    private suspend fun fromRemote(url: String, cacheFile: File?, client: OkHttpClient): ImageBitmap? {
        val request = Request.Builder().url(url).build()

        // TODO error handling
        return client.newCall(request).await()
            .use { response ->
                // TODO blocking method call
                response.body?.bytes()
            }
            ?.let { bytes ->
                val image = Image.makeFromEncoded(bytes).asImageBitmap()

                cacheFile?.let {
                    IMAGES_DIR.mkdirs()
                    it.writeBytes(bytes)
                }

                image
            }
    }
}
