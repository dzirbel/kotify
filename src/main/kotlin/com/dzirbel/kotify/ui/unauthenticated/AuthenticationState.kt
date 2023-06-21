package com.dzirbel.kotify.ui.unauthenticated

import com.dzirbel.kotify.network.oauth.LocalOAuthServer
import com.dzirbel.kotify.network.oauth.OAuth
import kotlinx.collections.immutable.PersistentSet

data class AuthenticationState(
    val oauth: OAuth? = null,
    val clientId: String = OAuth.DEFAULT_CLIENT_ID,
    val port: Int = LocalOAuthServer.DEFAULT_PORT,
    val scopes: PersistentSet<String> = OAuth.DEFAULT_SCOPES,
    val manualRedirectUrl: String = "",
)
