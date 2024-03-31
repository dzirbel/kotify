package com.dzirbel.kotify.network.oauth

import com.dzirbel.kotify.log.Log
import com.dzirbel.kotify.log.MutableLog
import com.dzirbel.kotify.log.asLog
import com.dzirbel.kotify.log.info
import com.dzirbel.kotify.log.success
import com.dzirbel.kotify.log.warn
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.util.await
import com.dzirbel.kotify.network.util.bodyFromJson
import com.dzirbel.kotify.util.CurrentTime
import com.dzirbel.kotify.util.coroutines.Computation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.plus
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.time.Instant
import java.util.EnumSet
import java.util.concurrent.TimeUnit

/**
 * Represents an access token which may be used to access the Spotify API.
 *
 * The [accessToken] itself can be used as an authorization header on requests to the Spotify API, alongside [tokenType]
 * (always "Bearer").
 *
 * Access tokens have an expiration time, namely [expiresIn] seconds after they are granted. [isExpired] checks the
 * expiration status of this token, based on the [received] time.
 *
 * Some access tokens (those granted via Authorization Code Flows) allow the client to access the Spotify API on behalf
 * of a particular user; these have a [scope] which specifies what API access the user has granted.
 *
 * Some access tokens (also those granted via an Authorization Code Flow) use a [refreshToken] to get a new token when
 * they have expired.
 */
@Serializable
data class AccessToken(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    val scope: String? = null,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val received: Long = CurrentTime.millis,
) {
    val isExpired
        get() = CurrentTime.millis >= received + TimeUnit.SECONDS.toMillis(expiresIn)

    /**
     * A parsed list of the scopes granted by this [AccessToken], or null if it was not granted with [scope].
     */
    val scopes: Set<OAuth.Scope>? by lazy {
        scope
            ?.split(' ')
            ?.mapNotNullTo(EnumSet.noneOf(OAuth.Scope::class.java)) { OAuth.Scope.of(it) }
    }

    /**
     * The [Instant] at which this [AccessToken] was received.
     */
    val receivedInstant: Instant by lazy { Instant.ofEpochMilli(received) }

    /**
     * The [Instant] at which this [AccessToken] expires.
     */
    val expiresInstant: Instant by lazy { receivedInstant.plusSeconds(expiresIn) }

    /**
     * Determines whether [scope] is granted by this [AccessToken].
     */
    fun hasScope(scope: OAuth.Scope): Boolean = scopes?.contains(scope) == true

    /**
     * A simple in-memory and filesystem cache for a single [AccessToken].
     *
     * This is used to manage access tokens, typically granted by [OAuth], and allows storing the current token and
     * refreshing it when it is expired.
     */
    object Cache {
        /**
         * The file at which the access token is saved, relative to the current working directory.
         *
         * Expected to be provided at application start, if null then the access token is not saved to or loaded from
         * disk but only cached in-memory.
         */
        var cacheFile: File? = null

        /**
         * [Json] used to serialize the access token to a cache file.
         *
         * Enables encoding defaults in order to include [AccessToken.received]. Does not change leniency for release
         * modes since this should not be necessary.
         */
        private val cacheFileJson = Json {
            encodeDefaults = true
            prettyPrint = true
        }

        private val scope = GlobalScope.plus(Dispatchers.Computation)

        private var refreshJob: Job? = null

        private val mutableLog = MutableLog<Unit>("AccessToken", scope)

        val log: Log<Unit> = mutableLog.asLog()

        private val _tokenFlow = MutableStateFlow<AccessToken?>(null)

        /**
         * A [StateFlow] reflecting the current [AccessToken] held in the cache, or null if there is none.
         */
        val tokenFlow: StateFlow<AccessToken?>
            get() = _tokenFlow.asStateFlow()

        /**
         * Requires that the currently cached token has a [AccessToken.refreshToken], i.e. that it came from an
         * authorization code flow. If there is a non-refreshable access token, it is [clear]ed.
         */
        fun requireRefreshable() {
            val token = _tokenFlow.value ?: load()
            if (token != null && token.refreshToken == null) {
                mutableLog.warn("Current access token is not refreshable, clearing")
                clear()
            }
        }

        /**
         * Gets the currently cached [AccessToken], or throw a [NoAccessTokenError] if there is no token cached.
         *
         * If the cached token has expired and has a [AccessToken.refreshToken], it is refreshed (i.e. a new
         * [AccessToken] is fetched based on the old [AccessToken.refreshToken]) and the new token is returned.
         */
        suspend fun getOrThrow(
            clientId: String = OAuth.DEFAULT_CLIENT_ID,
            client: OkHttpClient = Spotify.configuration.oauthOkHttpClient,
        ): AccessToken {
            return get(clientId = clientId, client = client) ?: throw NoAccessTokenError()
        }

        /**
         * Gets the currently cached [AccessToken] or null if there is no token cached.
         *
         * If the cached token has expired and has a [AccessToken.refreshToken], it is refreshed (i.e. a new
         * [AccessToken] is fetched based on the old [AccessToken.refreshToken]) and the new token is returned.
         */
        suspend fun get(
            clientId: String = OAuth.DEFAULT_CLIENT_ID,
            client: OkHttpClient = Spotify.configuration.oauthOkHttpClient,
        ): AccessToken? {
            val token = _tokenFlow.value ?: load() ?: return null

            if (token.isExpired) {
                refresh(clientId = clientId, client = client)?.join()
            }

            return _tokenFlow.value
        }

        /**
         * Attempts to refresh the in-memory token via its [AccessToken.refreshToken] (or does nothing if it has no
         * refresh token) to get a fresh [AccessToken].
         *
         * If successful, the new access token is immediately available in-memory and written to disk.
         */
        fun refresh(
            clientId: String = OAuth.DEFAULT_CLIENT_ID,
            client: OkHttpClient = Spotify.configuration.oauthOkHttpClient,
        ): Job? {
            val refreshToken: String = _tokenFlow.value?.refreshToken ?: return null

            return synchronized(this) {
                refreshJob ?: scope.async {
                    mutableLog.info("Current access token is expired; refreshing")

                    val start = CurrentTime.mark

                    val body = FormBody.Builder()
                        .add("grant_type", "refresh_token")
                        .add("refresh_token", refreshToken)
                        .add("client_id", clientId)
                        .build()

                    val request = Request.Builder()
                        .post(body)
                        .url("https://accounts.spotify.com/api/token")
                        .build()

                    val token = try {
                        client.newCall(request).await()
                            .use { response -> response.bodyFromJson<AccessToken>() }
                    } catch (throwable: Throwable) {
                        mutableLog.warn(throwable, "Error refreshing access token", duration = start.elapsedNow())
                        clear()
                        null
                    }

                    if (token != null) {
                        mutableLog.success("Got refreshed access token", duration = start.elapsedNow())
                        _tokenFlow.value = token
                        save(token)
                    }

                    refreshJob = null
                }.also { refreshJob = it }
            }
        }

        /**
         * Puts the given [AccessToken] in the cache, immediately writing it to disk.
         */
        internal fun put(accessToken: AccessToken) {
            mutableLog.info("Putting new access token in cache")
            _tokenFlow.value = accessToken
            save(accessToken)
        }

        /**
         * Clears the [AccessToken] cache, removing the currently cached token and deleting it on disk.
         */
        fun clear() {
            _tokenFlow.value = null
            cacheFile?.let { cacheFile ->
                try {
                    Files.deleteIfExists(cacheFile.toPath())
                } catch (ioException: IOException) {
                    mutableLog.warn(ioException, "Error deleting access token cache file")
                }
            }
            mutableLog.info("Cleared access token from cache")
        }

        /**
         * Clears in-memory state of the cache, in order to test loading the token from disk. Should only be used in
         * tests.
         */
        internal fun reset() {
            _tokenFlow.value = null
        }

        /**
         * Writes [token] to disk.
         */
        @OptIn(ExperimentalSerializationApi::class)
        private fun save(token: AccessToken) {
            val file = cacheFile
            if (file != null) {
                val start = CurrentTime.mark
                file.outputStream().use { outputStream ->
                    cacheFileJson.encodeToStream(token, outputStream)
                }
                mutableLog.success("Saved access token to $file", duration = start.elapsedNow())
            } else {
                mutableLog.warn("No cache file provided; did not save access token to disk")
            }
        }

        /**
         * Reads the token from disk and returns it, or null if there is no token file.
         */
        @OptIn(ExperimentalSerializationApi::class)
        private fun load(): AccessToken? {
            val file = cacheFile
            if (file == null) {
                mutableLog.warn("No cache file provided; cannot load access token from disk")
                return null
            }

            val start = CurrentTime.mark
            return try {
                file.inputStream()
                    .use { cacheFileJson.decodeFromStream<AccessToken>(it) }
                    .also { _tokenFlow.value = it }
                    .also { mutableLog.success("Loaded access token from $file", duration = start.elapsedNow()) }
            } catch (_: FileNotFoundException) {
                null.also { mutableLog.info("No saved access token at $file", duration = start.elapsedNow()) }
            }
        }

        class NoAccessTokenError : Throwable()
    }
}
