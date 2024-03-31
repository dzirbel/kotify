package com.dzirbel.kotify.cache

import androidx.compose.ui.graphics.asSkiaBitmap
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import com.dzirbel.kotify.network.MockOkHttpClient
import com.dzirbel.kotify.ui.SpotifyImageCache
import com.dzirbel.kotify.util.MockedTimeExtension
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

@ExtendWith(MockedTimeExtension::class)
internal class SpotifyImageCacheTest {
    @AfterEach
    fun cleanup() {
        runBlocking { SpotifyImageCache.clear(scope = this) }
        testImageDir.deleteRecursively()
    }

    @Test
    fun testRemoteSuccess() {
        val client = MockOkHttpClient(
            responseBody = testImageBytes.toResponseBody(contentType = "image/jpeg".toMediaType()),
        )

        runTest {
            val imageCache = SpotifyImageCache(scope = this, synchronousCalls = true)

            val imageFlow = imageCache.get(url = DEFAULT_IMAGE_URL, client = client)
            assertThat(imageFlow.value).isNull()
            assertThat(imageCache.getFromMemory(DEFAULT_IMAGE_URL)).isNull()

            runCurrent()

            assertThat(imageFlow.value).isNotNull()
            assertThat(imageCache.getFromMemory(DEFAULT_IMAGE_URL)).isNotNull()
            assertThat(client.requests).hasSize(1)
            assertThat(imageCache.metricsFlow.value?.inMemoryCount).isEqualTo(1)
        }
    }

    @RepeatedTest(5)
    fun testRemoteConcurrent() {
        val client = MockOkHttpClient(
            responseBody = testImageBytes.toResponseBody(contentType = "image/jpeg".toMediaType()),
        )

        runTest {
            val imageCache = SpotifyImageCache(scope = this, synchronousCalls = true)

            val imageFlow1Deferred = async { imageCache.get(url = DEFAULT_IMAGE_URL, client = client) }
            val imageFlow2Deferred = async {
                delay(50)
                imageCache.get(url = DEFAULT_IMAGE_URL, client = client)
            }

            val imageFlow1 = imageFlow1Deferred.await()
            val imageFlow2 = imageFlow2Deferred.await()

            assertThat(imageFlow1).isNotNull()
            assertThat(imageFlow1).isNotNull()
            assertThat(imageFlow1).isSameInstanceAs(imageFlow2)

            runCurrent()

            assertThat(client.requests).hasSize(1)
            assertThat(imageCache.metricsFlow.value?.inMemoryCount).isEqualTo(1)
        }
    }

    @Test
    fun testRemoteEmptyResponse() {
        val client = MockOkHttpClient(
            responseBody = "".toResponseBody("text/plain".toMediaType()),
        )

        runTest {
            val imageCache = SpotifyImageCache(scope = this, synchronousCalls = true)

            val imageFlow = imageCache.get(url = DEFAULT_IMAGE_URL, client = client)

            runCurrent()

            assertThat(imageFlow.value).isNull()
            assertThat(client.requests).hasSize(1)
            assertThat(SpotifyImageCache.metricsFlow.value?.inMemoryCount).isEqualTo(0)
        }
    }

    @Test
    fun testRemoteNotFound() {
        val client = MockOkHttpClient(
            responseCode = 404,
            responseMessage = "Not Found",
        )

        runTest {
            val imageCache = SpotifyImageCache(scope = this, synchronousCalls = true)

            val imageFlow = imageCache.get(url = DEFAULT_IMAGE_URL, client = client)

            runCurrent()

            assertThat(imageFlow.value).isNull()
            assertThat(client.requests).hasSize(1)
            assertThat(SpotifyImageCache.metricsFlow.value?.inMemoryCount).isEqualTo(0)
        }
    }

    @Test
    fun testInMemoryCache() {
        val client = MockOkHttpClient(
            responseBody = testImageBytes.toResponseBody(contentType = "image/jpeg".toMediaType()),
        )

        runTest {
            val imageCache = SpotifyImageCache(scope = this, synchronousCalls = true)

            val imageFlow1 = imageCache.get(url = DEFAULT_IMAGE_URL, client = client)

            runCurrent()

            assertThat(imageFlow1.value).isNotNull()
            assertThat(client.requests).hasSize(1)
            assertThat(imageCache.metricsFlow.value?.inMemoryCount).isEqualTo(1)

            val imageFlow2 = imageCache.get(url = DEFAULT_IMAGE_URL, client = client)

            runCurrent()

            assertThat(imageFlow2).isNotNull()
            assertThat(imageFlow2).isSameInstanceAs(imageFlow1)
            assertThat(client.requests).hasSize(1)
            assertThat(imageCache.metricsFlow.value?.inMemoryCount).isEqualTo(1)
        }
    }

    @Test
    fun testDiskCache() {
        // only spotify images are cached on disk
        val url = "https://i.scdn.co/image/0ef1abc88dcd2f7131ba4d21c6dc56fcc027ef24"

        val client = MockOkHttpClient(
            responseBody = testImageBytes.toResponseBody(contentType = "image/jpeg".toMediaType()),
        )

        runTest {
            val imageCache = SpotifyImageCache(scope = this, synchronousCalls = true)

            imageCache.withImagesDir(testImageDir) {
                val imageFlow1 = imageCache.get(url = url, client = client)
                runCurrent()

                assertThat(imageCache.getFromMemory(url)).isNotNull()
                assertThat(client.requests).hasSize(1)
                assertThat(imageCache.metricsFlow.value?.inMemoryCount).isEqualTo(1)

                imageCache.clear(scope = this, deleteFileCache = false)
                runCurrent()

                assertThat(imageCache.getFromMemory(url)).isNull()

                val imageFlow2 = imageCache.get(url = url, client = client)
                runCurrent()

                assertThat(imageCache.getFromMemory(url)).isNotNull()
                assertThat(imageFlow2).isNotSameInstanceAs(imageFlow1)
                assertThat(requireNotNull(imageFlow2.value).asSkiaBitmap().readPixels())
                    .isNotNull()
                    .isEqualTo(requireNotNull(requireNotNull(imageFlow1.value).asSkiaBitmap().readPixels()))
                assertThat(client.requests).hasSize(1)
                assertThat(imageCache.metricsFlow.value?.inMemoryCount).isEqualTo(1)
            }
        }
    }

    companion object {
        private const val DEFAULT_IMAGE_URL = "https://example.com/image"

        private val testImageDir = File(".kotify/test-cache")

        private val testImageBytes by lazy { Files.readAllBytes(Path.of("src/test/resources/test-image.jpg")) }
    }
}
