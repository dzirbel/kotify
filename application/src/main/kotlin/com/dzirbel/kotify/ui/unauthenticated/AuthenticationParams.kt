package com.dzirbel.kotify.ui.unauthenticated

import com.dzirbel.kotify.network.oauth.LocalOAuthServer
import com.dzirbel.kotify.network.oauth.OAuth
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toPersistentSet

data class AuthenticationParams(
    val clientId: String = OAuth.DEFAULT_CLIENT_ID,
    val port: Int = LocalOAuthServer.DEFAULT_PORT,
    val runLocalServer: Boolean = true,
    val scopes: PersistentSet<OAuth.Scope> = OAuth.Scope.DEFAULT_SCOPES.toPersistentSet(),
)
