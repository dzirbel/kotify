package com.dzirbel.kotify.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.dzirbel.kotify.Logger
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.util.await
import com.dzirbel.kotify.repository.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.yield
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.skia.Image
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.TimeSource

sealed class ImageCacheEvent {
    data class InMemory(val url: String) : ImageCacheEvent()
    data class OnDisk(val url: String, val duration: Duration, val cacheFile: File) : ImageCacheEvent()
    data class Fetch(val url: String, val duration: Duration, val cacheFile: File?) : ImageCacheEvent()
}

private const val SPOTIFY_IMAGE_URL_PREFIX = "https://i.scdn.co/image/"

/**
 * A simple disk cache for images loaded from Spotify's image CDN.
 */
open class SpotifyImageCache internal constructor(
    /**
     * The [CoroutineScope] (and its context) used to load images both from disk and remotely.
     */
    private val scope: CoroutineScope,

    /**
     * Whether to execute remote calls synchronously; this is disabled in tests since OkHTTP does not natively use
     * coroutines, so providing a TestDispatcher will not advance an asynchronous call as expected.
     */
    private val synchronousCalls: Boolean,
) {
    private var imagesDir: File? = null

    // TODO use a LRU or similar cache to avoid keeping all loaded images in memory indefinitely
    private val images: ConcurrentMap<String, StateFlow<ImageBitmap?>> = ConcurrentHashMap()

    private val totalCompleted = AtomicInteger()

    private val _metricsFlow = MutableStateFlow<Metrics?>(null)

    /**
     * The current [Metrics] of the cache.
     */
    val metricsFlow: StateFlow<Metrics?>
        get() = _metricsFlow.asStateFlow()

    fun init(imagesDir: File) {
        this.imagesDir = imagesDir
            .also { it.mkdirs() }
            .also { check(it.isDirectory) { "could not create image cache directory $it" } }

        GlobalScope.launch(scope.coroutineContext) {
            _metricsFlow.value = loadMetrics()
        }
    }

    internal fun withImagesDir(imagesDir: File, block: () -> Unit) {
        val previousImagesDir = this.imagesDir
        init(imagesDir)
        block()
        this.imagesDir = previousImagesDir
    }

    /**
     * Clears the in-memory and disk cache.
     */
    fun clear(scope: CoroutineScope = GlobalScope, deleteFileCache: Boolean = true) {
        scope.launch {
            images.clear()
            totalCompleted.set(0)
            if (deleteFileCache) {
                imagesDir?.deleteRecursively()
            }
            _metricsFlow.value = Metrics(inMemoryCount = 0, diskCount = 0, totalDiskSize = 0)
        }
    }

    /**
     * Returns the [ImageBitmap] from the given [url] if it is immediately available in memory.
     */
    fun getFromMemory(url: String): ImageBitmap? {
        return images[url]?.value
            ?.also { Logger.ImageCache.handleImageCacheEvent(ImageCacheEvent.InMemory(url = url)) }
    }

    /**
     * Returns a [StateFlow] reflecting the live state of the image fetched from the given [url].
     */
    fun get(url: String, client: OkHttpClient = Spotify.configuration.okHttpClient): StateFlow<ImageBitmap?> {
        return images.compute(url) { _, existingFlow ->
            if (existingFlow == null) {
                val flow = MutableStateFlow<ImageBitmap?>(null)

                scope.launch {
                    val (cacheFile, image) = fromFileCache(url)
                    if (image != null) {
                        flow.value = image
                    } else {
                        flow.value = fromRemote(url = url, cacheFile = cacheFile, client = client)
                    }
                    _metricsFlow.value = loadMetrics()
                }

                flow
            } else {
                Logger.ImageCache.handleImageCacheEvent(ImageCacheEvent.InMemory(url = url))
                existingFlow
            }
        }
            .let { requireNotNull(it) }
    }

    private suspend fun fromFileCache(url: String): Pair<File?, ImageBitmap?> {
        val start = TimeSource.Monotonic.markNow()
        var cacheFile: File? = null
        if (url.startsWith(SPOTIFY_IMAGE_URL_PREFIX)) {
            val imageHash = url.substring(SPOTIFY_IMAGE_URL_PREFIX.length)
            cacheFile = imagesDir?.resolve(imageHash)

            if (cacheFile?.isFile == true) {
                val bytes = cacheFile.readBytes()
                yield()
                val image = Image.makeFromEncoded(bytes)
                yield()
                val imageBitmap = image.toComposeImageBitmap()
                yield()

                totalCompleted.incrementAndGet()
                Logger.ImageCache.handleImageCacheEvent(
                    ImageCacheEvent.OnDisk(url = url, duration = start.elapsedNow(), cacheFile = cacheFile),
                )

                return Pair(cacheFile, imageBitmap)
            }
        }

        return Pair(cacheFile, null)
    }

    private suspend fun fromRemote(url: String, cacheFile: File?, client: OkHttpClient): ImageBitmap? {
        val start = TimeSource.Monotonic.markNow()
        val request = Request.Builder().url(url).build()

        return client.newCall(request)
            .run { if (synchronousCalls) execute() else await() }
            .use { response ->
                response.body?.bytes()
            }
            ?.takeIf { bytes -> bytes.isNotEmpty() }
            ?.let { bytes ->
                yield()
                val image = Image.makeFromEncoded(bytes).toComposeImageBitmap()
                yield()

                if (cacheFile != null) {
                    imagesDir?.mkdirs()
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
     * Loads the [Metrics] from the file system. This is a fairly expensive operation but avoids concurrency
     * issues when loading multiple images in parallel.
     */
    private fun loadMetrics(): Metrics {
        val files = imagesDir?.listFiles()?.filter { it.isFile }
        return Metrics(
            inMemoryCount = totalCompleted.get(),
            diskCount = files?.size ?: 0,
            totalDiskSize = files?.sumOf { it.length() } ?: 0,
        )
    }

    /**
     * Holds metrics about the current state of the image cache.
     */
    data class Metrics(
        val inMemoryCount: Int,
        val diskCount: Int,
        val totalDiskSize: Long,
    )

    companion object : SpotifyImageCache(
        // use a custom dispatcher rather than the default IO dispatcher as it ca easily be exhausted, which appears to
        // cause blocking UI interference, perhaps due to internal Compose usage
        scope = Repository.applicationScope.plus(Executors.newCachedThreadPool().asCoroutineDispatcher()),
        synchronousCalls = false,
    )
}
