package com.dzirbel.kotify.network.oauth

import androidx.compose.runtime.mutableStateOf
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.await
import com.dzirbel.kotify.network.bodyFromJson
import com.dzirbel.kotify.ui.util.openInBrowser
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.security.SecureRandom
import java.util.Base64

/**
 * Represents an in-progress OAuth flow, and wraps a [LocalOAuthServer] which is automatically started and runs until
 * the flow is completed by [cancel]ing it from a redirect captured by the [LocalOAuthServer].
 *
 * See https://developer.spotify.com/documentation/general/guides/authorization-guide/
 */
class OAuth private constructor(
    state: String,
    private val clientId: String,
    private val codeVerifier: String,
    private val redirectUri: String,
    val authorizationUrl: HttpUrl,
) {
    val error = mutableStateOf<Throwable?>(null)
    val result = mutableStateOf<LocalOAuthServer.Result?>(null)
    private var stopped = false

    private val server: LocalOAuthServer = LocalOAuthServer(
        state = state,
        callback = { result ->
            this.result.value = result
            if (result is LocalOAuthServer.Result.Success) {
                try {
                    onSuccess(code = result.code)
                    finish()
                } catch (ex: Throwable) {
                    error.value = ex
                }
            }
        }
    ).start()

    /**
     * Marks this [OAuth] flow as complete and stops its [server], throwing an [IllegalStateException] if it was already
     * consumed.
     */
    private fun finish() {
        val wasFinished = synchronized(this) {
            stopped.also { stopped = true }
        }

        if (!wasFinished) {
            // run async to avoid deadlock (since in onSuccess() we're still processing a request)
            GlobalScope.launch {
                runCatching { server.stop() }
            }
        }
    }

    /**
     * Cancels this [OAuth] flow, preventing it from being completed and stopping its embedded [server].
     */
    fun cancel() {
        finish()
    }

    suspend fun onManualRedirect(url: HttpUrl): LocalOAuthServer.Result {
        return server.manualRedirectUrl(url = url)
    }

    /**
     * Invoked when a redirect was successfully captured by the [server], with a authorization [code].
     */
    private suspend fun onSuccess(code: String) {
        val body = FormBody.Builder()
            .add("client_id", clientId)
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", redirectUri)
            .add("code_verifier", codeVerifier)
            .build()

        val request = Request.Builder()
            .post(body)
            .url("https://accounts.spotify.com/api/token")
            .build()

        val accessToken = Spotify.configuration.oauthOkHttpClient.newCall(request).await()
            .use { response -> response.bodyFromJson<AccessToken>() }

        AccessToken.Cache.put(accessToken)
    }

    companion object {
        /**
         * The default client ID, for my personal client.
         */
        const val DEFAULT_CLIENT_ID = "0c303117a0624fb0adc4832dd286cf39"

        /**
         * All authorization scopes supported by Spotify.
         *
         * See https://developer.spotify.com/documentation/general/guides/scopes/
         */
        val ALL_SCOPES = setOf(
            "app-remote-control", // only for Android/iOS
            "playlist-modify-private",
            "playlist-modify-public",
            "playlist-read-collaborative",
            "playlist-read-private",
            "streaming", // only for web playback: https://developer.spotify.com/documentation/web-playback-sdk/
            "ugc-image-upload",
            "user-follow-modify",
            "user-follow-read",
            "user-library-modify",
            "user-library-read",
            "user-modify-playback-state",
            "user-read-currently-playing",
            "user-read-email",
            "user-read-playback-position",
            "user-read-playback-state",
            "user-read-private",
            "user-read-recently-played",
            "user-top-read",
        )

        /**
         * The default authorization scopes to request.
         *
         * See https://developer.spotify.com/documentation/general/guides/scopes/
         */
        val DEFAULT_SCOPES = ALL_SCOPES.minus("app-remote-control").minus("streaming").toSet()

        // number of bytes in the state buffer; 16 bytes -> 22 characters
        private const val STATE_BUFFER_SIZE = 16
        private val stateEncoder = Base64.getUrlEncoder().withoutPadding()

        /**
         * Starts a new OAuth flow for Authorization with Proof Key for Code Exchange (PKCE).
         *
         * This generates a [CodeChallenge] and state, which are used to create a [authorizationUrl], which is opened in
         * the user's web browser (failing silently if that operation is not supported). The returned [OAuth] object
         * contains information about the authorization request flow, in particular it starts a [LocalOAuthServer] to
         * capture redirects and has callbacks to complete the flow.
         */
        fun start(
            clientId: String = DEFAULT_CLIENT_ID,
            scopes: Set<String> = DEFAULT_SCOPES,
            port: Int = LocalOAuthServer.DEFAULT_PORT,
        ): OAuth {
            val state = generateState()
            val codeChallenge = CodeChallenge.generate()

            // TODO add a couple fallback ports in case the default one is taken? (must be whitelisted in the Spotify
            //  developer dashboard)
            val redirectUri = LocalOAuthServer.redirectUrl(port = port)

            val authorizationUrl = authorizationUrl(
                clientId = clientId,
                scopes = scopes,
                redirectUri = redirectUri,
                codeChallenge = codeChallenge,
                state = state
            )

            openInBrowser(authorizationUrl)

            return OAuth(
                state = state,
                codeVerifier = codeChallenge.verifier,
                clientId = clientId,
                redirectUri = redirectUri,
                authorizationUrl = authorizationUrl
            )
        }

        /**
         * Generates a new random string which can serve as state for the current OAuth flow, to prevent CSRF attacks.
         *
         * Only visible so it can be unit tested; do not reference directly.
         */
        internal fun generateState(random: SecureRandom = SecureRandom()): String {
            val buffer = ByteArray(STATE_BUFFER_SIZE)
            random.nextBytes(buffer)
            return stateEncoder.encodeToString(buffer)
        }

        /**
         * Returns the authorization URL for the Spotify API, which displays a permissions dialog to the user and then
         * redirects to [redirectUri] (with a authorization code if the user accepts or an error if the user declines as
         * query parameters).
         *
         * Only visible so it can be unit tested; do not reference directly.
         */
        internal fun authorizationUrl(
            clientId: String,
            scopes: Set<String>,
            redirectUri: String,
            codeChallenge: CodeChallenge,
            state: String,
        ): HttpUrl {
            return "https://accounts.spotify.com/authorize".toHttpUrl()
                .newBuilder()
                .addQueryParameter("client_id", clientId)
                .addEncodedQueryParameter("response_type", "code")
                .addQueryParameter("redirect_uri", redirectUri)
                .addEncodedQueryParameter("code_challenge_method", "S256")
                .addEncodedQueryParameter("code_challenge", codeChallenge.challenge)
                .addQueryParameter("state", state)
                .addQueryParameter("scope", scopes.joinToString(separator = " "))
                .build()
        }
    }
}
