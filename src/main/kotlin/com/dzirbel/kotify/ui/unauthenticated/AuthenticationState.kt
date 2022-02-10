package com.dzirbel.kotify.ui.unauthenticated

import com.dzirbel.kotify.network.oauth.LocalOAuthServer
import com.dzirbel.kotify.network.oauth.OAuth

data class AuthenticationState(
    val oauth: OAuth? = null,
    val clientId: String = OAuth.DEFAULT_CLIENT_ID,
    val port: Int = LocalOAuthServer.DEFAULT_PORT,
    val scopes: Set<String> = OAuth.DEFAULT_SCOPES,
    val manualRedirectUrl: String = "",
)
