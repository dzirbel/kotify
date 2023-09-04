package com.dzirbel.kotify.network.oauth

import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.util.await
import com.dzirbel.kotify.network.util.bodyFromJson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

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
    enum class Scope(val officialDescription: String, val requestByDefault: Boolean = true) {
        APP_REMOTE_CONTROL("Communicate with the Spotify app on your device.", requestByDefault = false),
        PLAYLIST_MODIFY_PRIVATE("Manage your private playlists."),
        PLAYLIST_MODIFY_PUBLIC("Manage your public playlists."),
        PLAYLIST_READ_COLLABORATIVE("Access your collaborative playlists."),
        PLAYLIST_READ_PRIVATE("Access your private playlists."),
        STREAMING("Play content and control playback on your other devices.", requestByDefault = false),
        UGC_IMAGE_UPLOAD("Upload images to Spotify on your behalf.", requestByDefault = false),
        USER_CREATE_PARTNER("Create new partners, platform partners only.", requestByDefault = false),
        USER_FOLLOW_MODIFY("Manage who you are following."),
        USER_FOLLOW_READ("Access your followers and who you are following."),
        USER_LIBRARY_MODIFY("Manage your saved content."),
        USER_LIBRARY_READ("Access your saved content."),
        USER_MANAGE_ENTITLEMENTS("Modify entitlements for linked users.", requestByDefault = false),
        USER_MANAGE_PARTNER("Update partner information.", requestByDefault = false),
        USER_MODIFY_PLAYBACK_STATE("Control playback on your Spotify clients and Spotify Connect devices."),
        USER_READ_CURRENTLY_PLAYING("Read your currently playing content."),
        USER_READ_EMAIL("Get your real email address.", requestByDefault = false),
        USER_READ_PLAYBACK_POSITION("Read your position in content you have played."),
        USER_READ_PLAYBACK_STATE("Read your currently playing content and Spotify Connect devices information."),
        USER_READ_PRIVATE("Access your subscription details."),
        USER_READ_RECENTLY_PLAYED("Access your recently played items."),
        USER_SOA_LINK("Link a partner user account to a Spotify user account.", requestByDefault = false),
        USER_SOA_UNLINK("Unlink a partner user account from a Spotify account.", requestByDefault = false),
        USER_TOP_READ("Read your top artists and content."),
        ;

        val scope: String by lazy { name.lowercase(Locale.US).replace('_', '-') }

        companion object {
            val DEFAULT_SCOPES: Set<Scope> = entries.filterTo(mutableSetOf()) { it.requestByDefault }

            fun of(scope: String): Scope? = entries.find { it.scope.equals(scope, ignoreCase = true) }
        }
    }

    private val _errorFlow = MutableStateFlow<Throwable?>(null)
    val errorFlow: StateFlow<Throwable?>
        get() = _errorFlow.asStateFlow()

    private val _resultFlow = MutableStateFlow<LocalOAuthServer.Result?>(null)
    val resultFlow: StateFlow<LocalOAuthServer.Result?>
        get() = _resultFlow.asStateFlow()

    private val stopped = AtomicBoolean(false)

    private val server: LocalOAuthServer = LocalOAuthServer(
        state = state,
        callback = { result ->
            _resultFlow.value = result
            if (result is LocalOAuthServer.Result.Success) {
                try {
                    onSuccess(code = result.code)
                    finish()
                } catch (ex: Throwable) {
                    _errorFlow.value = ex
                }
            }
        },
    ).start()

    /**
     * Marks this [OAuth] flow as complete and stops its [server], throwing an [IllegalStateException] if it was already
     * consumed.
     */
    private fun finish() {
        if (!stopped.getAndSet(true)) {
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
            scopes: Set<Scope> = Scope.DEFAULT_SCOPES,
            port: Int = LocalOAuthServer.DEFAULT_PORT,
            openAuthorizationUrl: (HttpUrl) -> Unit,
        ): OAuth {
            val state = generateState()
            val codeChallenge = CodeChallenge.generate()

            val redirectUri = LocalOAuthServer.redirectUrl(port = port)

            val authorizationUrl = authorizationUrl(
                clientId = clientId,
                scopes = scopes.map { it.scope },
                redirectUri = redirectUri,
                codeChallenge = codeChallenge,
                state = state,
            )

            openAuthorizationUrl(authorizationUrl)

            return OAuth(
                state = state,
                codeVerifier = codeChallenge.verifier,
                clientId = clientId,
                redirectUri = redirectUri,
                authorizationUrl = authorizationUrl,
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
            scopes: Iterable<String>,
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
