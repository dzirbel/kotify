package com.dzirbel.kotify.cache

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNotSameAs
import assertk.assertions.isNull
import assertk.assertions.isSameAs
import com.dzirbel.kotify.network.MockRequestInterceptor
import com.dzirbel.kotify.ui.SpotifyImageCache
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

internal class SpotifyImageCacheTest {
    private val interceptor = MockRequestInterceptor()
    private val client = interceptor.client

    @AfterEach
    fun cleanup() {
        runBlocking { SpotifyImageCache.clear(scope = this) }
        testImageDir.deleteRecursively()
        interceptor.requests.clear()
    }

    @RepeatedTest(5)
    fun testRemoteSuccess() {
        interceptor.responseBody = testImageBytes.toResponseBody(contentType = "image/jpeg".toMediaType())

        val image = getImage()

        assertThat(image).isNotNull()
        assertThat(interceptor.requests).hasSize(1)
        assertThat(SpotifyImageCache.metricsFlow.value?.inMemoryCount).isEqualTo(1)
    }

    @RepeatedTest(5)
    fun testRemoteConcurrent() {
        interceptor.responseBody = testImageBytes.toResponseBody(contentType = "image/jpeg".toMediaType())
        interceptor.delayMs = 100

        val image1: ImageBitmap?
        val image2: ImageBitmap?

        runBlocking {
            val image1Deferred = async { getImage() }
            val image2Deferred = async {
                delay(50)
                getImage()
            }

            image1 = image1Deferred.await()
            image2 = image2Deferred.await()
        }

        assertThat(image1).isNotNull()
        assertThat(image2).isNotNull()
        assertThat(image1).isSameAs(image2)
        assertThat(interceptor.requests).hasSize(1)
        assertThat(SpotifyImageCache.metricsFlow.value?.inMemoryCount).isEqualTo(1)
    }

    @Test
    fun testRemoteEmptyResponse() {
        interceptor.responseBody = "".toResponseBody("text/plain".toMediaType())

        val image = getImage()

        assertThat(image).isNull()
        assertThat(interceptor.requests).hasSize(1)
        assertThat(SpotifyImageCache.metricsFlow.value?.inMemoryCount).isEqualTo(0)
    }

    @Test
    fun testRemoteNotFound() {
        interceptor.responseCode = 404
        interceptor.responseMessage = "Not Found"

        val image = getImage()

        assertThat(image).isNull()
        assertThat(interceptor.requests).hasSize(1)
        assertThat(SpotifyImageCache.metricsFlow.value?.inMemoryCount).isEqualTo(0)
    }

    @Test
    fun testInMemoryCache() {
        interceptor.responseBody = testImageBytes.toResponseBody(contentType = "image/jpeg".toMediaType())

        val image1 = getImage()

        assertThat(image1).isNotNull()
        assertThat(interceptor.requests).hasSize(1)
        assertThat(SpotifyImageCache.metricsFlow.value?.inMemoryCount).isEqualTo(1)

        val image2 = getImage()

        assertThat(image2).isNotNull()
        assertThat(image2).isSameAs(image1)
        assertThat(interceptor.requests).hasSize(1)
        assertThat(SpotifyImageCache.metricsFlow.value?.inMemoryCount).isEqualTo(1)
    }

    @Test
    fun testDiskCache() {
        SpotifyImageCache.withImagesDir(testImageDir) {
            // only spotify images are cached on disk
            val url = "https://i.scdn.co/image/0ef1abc88dcd2f7131ba4d21c6dc56fcc027ef24"

            interceptor.responseBody = testImageBytes.toResponseBody(contentType = "image/jpeg".toMediaType())

            val image1 = requireNotNull(getImage(url = url))

            assertThat(interceptor.requests).hasSize(1)
            assertThat(SpotifyImageCache.metricsFlow.value?.inMemoryCount).isEqualTo(1)

            runBlocking { SpotifyImageCache.clear(scope = this, deleteFileCache = false) }
            assertThat(SpotifyImageCache.getInMemory(url)).isNull()

            val image2 = requireNotNull(getImage(url = url))

            assertThat(image2).isNotSameAs(image1)
            assertThat(image2.asSkiaBitmap().readPixels())
                .isNotNull()
                .isEqualTo(requireNotNull(image1.asSkiaBitmap().readPixels()))
            assertThat(interceptor.requests).hasSize(1)
            assertThat(SpotifyImageCache.metricsFlow.value?.inMemoryCount).isEqualTo(1)
        }
    }

    private fun getImage(url: String = DEFAULT_IMAGE_URL): ImageBitmap? {
        return runBlocking {
            SpotifyImageCache.get(url = url, scope = this, client = client)
        }
    }

    companion object {
        private const val DEFAULT_IMAGE_URL = "https://example.com/image"

        private val testImageDir = File(".kotify/test-cache")

        private val testImageBytes by lazy { Files.readAllBytes(Path.of("src/test/resources/test-image.jpg")) }
    }
}
