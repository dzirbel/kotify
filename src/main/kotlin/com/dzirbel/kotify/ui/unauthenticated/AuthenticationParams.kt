package com.dzirbel.kotify.ui.unauthenticated

import com.dzirbel.kotify.network.oauth.LocalOAuthServer
import com.dzirbel.kotify.network.oauth.OAuth
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toPersistentSet

data class AuthenticationParams(
    val clientId: String = OAuth.DEFAULT_CLIENT_ID,
    val port: Int = LocalOAuthServer.DEFAULT_PORT,
    val scopes: PersistentSet<String> = OAuth.DEFAULT_SCOPES.toPersistentSet(),
    val manualRedirectUrl: String = "",
)
