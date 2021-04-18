package com.dzirbel.kotify.cache

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.dzirbel.kotify.Logger
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.await
import com.dzirbel.kotify.ui.util.assertNotOnUIThread
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
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
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

    /**
     * The current [State] of the cache.
     */
    var state by mutableStateOf(State())
        private set

    /**
     * Clears the in-memory and disk cache.
     */
    fun clear(scope: CoroutineScope = GlobalScope) {
        scope.launch {
            imageJobs.clear()
            totalCompleted.set(0)
            IMAGES_DIR.deleteRecursively()
            state = State()
        }
    }

    /**
     * Resets the in-memory cache, for use from unit tests.
     */
    internal fun testReset() {
        imageJobs.clear()
        totalCompleted.set(0)
        state = State()
    }

    /**
     * Immediately returns the in-memory cached [ImageBitmap] for [url], if these is one.
     */
    fun getInMemory(url: String): ImageBitmap? {
        return imageJobs[url]?.getCompleted()
    }

    /**
     * Fetches the [ImageBitmap] from the given [url] or cache.
     */
    suspend fun get(
        url: String,
        scope: CoroutineScope,
        cacheDir: File = IMAGES_DIR,
        context: CoroutineContext = EmptyCoroutineContext,
        client: OkHttpClient = Spotify.configuration.okHttpClient
    ): ImageBitmap? {
        val deferred = imageJobs.getOrPut(url) {
            scope.async(context = context) {
                assertNotOnUIThread()

                val (result, duration) = measureTimedValue { fromFileCache(url, cacheDir = cacheDir) }
                val (cacheFile, image) = result

                if (image != null) {
                    totalCompleted.incrementAndGet()
                    Logger.ImageCache.handleImageCacheEvent(
                        ImageCacheEvent.OnDisk(url = url, duration = duration, cacheFile = cacheFile!!)
                    )

                    image
                } else {
                    val (image2, duration2) = measureTimedValue {
                        fromRemote(url = url, cacheFile = cacheFile, client = client, cacheDir = cacheDir)
                    }

                    image2?.also {
                        totalCompleted.incrementAndGet()
                        Logger.ImageCache.handleImageCacheEvent(
                            ImageCacheEvent.Fetch(url = url, duration = duration2, cacheFile = cacheFile)
                        )
                    }
                }
            }.also { deferred ->
                deferred.invokeOnCompletion { error ->
                    if (error == null) {
                        state = State()
                    }
                }
            }
        }

        if (deferred.isCompleted) {
            Logger.ImageCache.handleImageCacheEvent(ImageCacheEvent.InMemory(url = url))
        }

        return deferred.await()
    }

    private fun fromFileCache(url: String, cacheDir: File = IMAGES_DIR): Pair<File?, ImageBitmap?> {
        var cacheFile: File? = null
        if (url.startsWith(SPOTIFY_IMAGE_URL_PREFIX)) {
            val imageHash = url.substring(SPOTIFY_IMAGE_URL_PREFIX.length)
            cacheFile = cacheDir.resolve(imageHash)

            if (cacheFile.isFile) {
                return Pair(cacheFile, Image.makeFromEncoded(cacheFile.readBytes()).asImageBitmap())
            }
        }

        return Pair(cacheFile, null)
    }

    private suspend fun fromRemote(
        url: String,
        cacheFile: File?,
        client: OkHttpClient,
        cacheDir: File = IMAGES_DIR
    ): ImageBitmap? {
        val request = Request.Builder().url(url).build()

        return client.newCall(request).await()
            .use { response ->
                @Suppress("BlockingMethodInNonBlockingContext")
                response.body?.bytes()
            }
            ?.takeIf { bytes -> bytes.isNotEmpty() }
            ?.let { bytes ->
                val image = Image.makeFromEncoded(bytes).asImageBitmap()

                cacheFile?.let {
                    cacheDir.mkdirs()
                    it.writeBytes(bytes)
                }

                image
            }
    }
}
