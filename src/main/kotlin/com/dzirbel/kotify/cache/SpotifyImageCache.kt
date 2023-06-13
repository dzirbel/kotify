package com.dzirbel.kotify.cache

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.dzirbel.kotify.Application
import com.dzirbel.kotify.Logger
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.await
import com.dzirbel.kotify.ui.util.assertNotOnUIThread
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.skia.Image
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.TimeSource

sealed class ImageCacheEvent {
    data class InMemory(val url: String) : ImageCacheEvent()
    data class OnDisk(val url: String, val duration: Duration, val cacheFile: File) : ImageCacheEvent()
    data class Fetch(val url: String, val duration: Duration, val cacheFile: File?) : ImageCacheEvent()
}

/**
 * A simple disk cache for images loaded from Spotify's image CDN.
 */
object SpotifyImageCache {
    private const val SPOTIFY_IMAGE_URL_PREFIX = "https://i.scdn.co/image/"
    private val IMAGES_DIR by lazy {
        Application.cacheDir.resolve("images")
            .also { it.mkdirs() }
            .also { require(it.isDirectory) { "could not create image cache directory $it" } }
    }

    private val imageJobs: ConcurrentMap<String, Deferred<ImageBitmap?>> = ConcurrentHashMap()

    private val totalCompleted = AtomicInteger()

    /**
     * The current [State] of the cache.
     */
    var state by mutableStateOf(State())
        private set

    /**
     * Clears the in-memory and disk cache.
     */
    fun clear(scope: CoroutineScope = GlobalScope, deleteFileCache: Boolean = true) {
        scope.launch(Dispatchers.IO) {
            imageJobs.clear()
            totalCompleted.set(0)
            if (deleteFileCache) {
                IMAGES_DIR.deleteRecursively()
            }
            state = State()
        }
    }

    /**
     * Immediately returns the in-memory cached [ImageBitmap] for [url], if there is one.
     */
    fun getInMemory(url: String): ImageBitmap? {
        val job = imageJobs[url]
        val bitmap = runCatching { job?.getCompleted() }.getOrNull()
        Logger.ImageCache.handleImageCacheEvent(ImageCacheEvent.InMemory(url = url))
        return bitmap
    }

    /**
     * Synchronously loads all the given [urls] from the file cache, if they are not currently in the in-memory cache or
     * already being loaded. This is useful for batch loading a set of images all at once.
     */
    suspend fun loadFromFileCache(urls: List<String>, scope: CoroutineScope) {
        val jobs = mutableSetOf<Job>()
        for (url in urls) {
            if (!imageJobs.containsKey(url)) {
                jobs.add(
                    scope.launch(Dispatchers.IO) {
                        assertNotOnUIThread()
                        val (_, image) = fromFileCache(url)

                        // only add to imageJobs if the image was in the file cache
                        if (image != null && !imageJobs.containsKey(url)) {
                            imageJobs[url] = CompletableDeferred(image)
                        }
                    },
                )
            }
        }

        jobs.joinAll()
    }

    /**
     * Fetches the [ImageBitmap] from the given [url] or cache.
     */
    suspend fun get(
        url: String,
        scope: CoroutineScope,
        context: CoroutineContext = EmptyCoroutineContext,
        client: OkHttpClient = Spotify.configuration.okHttpClient,
    ): ImageBitmap? {
        val deferred = imageJobs.computeIfAbsent(url) {
            scope.async(context = context) {
                assertNotOnUIThread()

                val (cacheFile, image) = fromFileCache(url)
                image ?: fromRemote(url = url, cacheFile = cacheFile, client = client)
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
            return deferred.getCompleted()
        }

        return deferred.await()
    }

    private fun fromFileCache(url: String): Pair<File?, ImageBitmap?> {
        val start = TimeSource.Monotonic.markNow()
        var cacheFile: File? = null
        if (url.startsWith(SPOTIFY_IMAGE_URL_PREFIX)) {
            val imageHash = url.substring(SPOTIFY_IMAGE_URL_PREFIX.length)
            cacheFile = IMAGES_DIR.resolve(imageHash)

            if (cacheFile.isFile) {
                val image = Image.makeFromEncoded(cacheFile.readBytes()).toComposeImageBitmap()

                totalCompleted.incrementAndGet()
                Logger.ImageCache.handleImageCacheEvent(
                    ImageCacheEvent.OnDisk(url = url, duration = start.elapsedNow(), cacheFile = cacheFile),
                )

                return Pair(cacheFile, image)
            }
        }

        return Pair(cacheFile, null)
    }

    private suspend fun fromRemote(url: String, cacheFile: File?, client: OkHttpClient): ImageBitmap? {
        val start = TimeSource.Monotonic.markNow()
        val request = Request.Builder().url(url).build()

        return client.newCall(request).await()
            .use { response ->
                response.body?.bytes()
            }
            ?.takeIf { bytes -> bytes.isNotEmpty() }
            ?.let { bytes ->
                val image = Image.makeFromEncoded(bytes).toComposeImageBitmap()

                if (cacheFile != null) {
                    IMAGES_DIR.mkdirs()
                    cacheFile.writeBytes(bytes)
                }

                totalCompleted.incrementAndGet()
                Logger.ImageCache.handleImageCacheEvent(
                    ImageCacheEvent.Fetch(url = url, duration = start.elapsedNow(), cacheFile = cacheFile),
                )

                image
            }
    }

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
        val totalDiskSize: Int = IMAGES_DIR.listFiles()?.sumOf { it.length().toInt() } ?: 0,
    )
}
