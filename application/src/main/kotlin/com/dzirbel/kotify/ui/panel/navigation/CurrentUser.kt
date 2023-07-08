package com.dzirbel.kotify.ui.panel.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.SpotifyEntity
import com.dzirbel.kotify.db.model.User
import com.dzirbel.kotify.network.oauth.AccessToken
import com.dzirbel.kotify.repository2.CacheState
import com.dzirbel.kotify.repository2.user.UserRepository
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.components.liveRelativeDateText
import com.dzirbel.kotify.ui.theme.Dimens

private val CURRENT_USER_DROPDOWN_MAX_WIDTH = 500.dp

// TODO extract and reuse
@Composable
fun <E : SpotifyEntity, T : Any> E.produceTransactionState(
    transactionName: String,
    statement: suspend E.() -> T?,
): State<T?> {
    return produceState<T?>(initialValue = null, key1 = this.id.value) {
        value = KotifyDatabase.transaction(name = transactionName) { statement() }
    }
}

@Composable
fun CurrentUser() {
    val currentUserCacheState = UserRepository.currentUser.collectAsState().value

    val expandedState = remember { mutableStateOf(false) }
    SimpleTextButton(onClick = { expandedState.value = !expandedState.value }) {
        val currentUser = currentUserCacheState?.cachedValue

        val thumbnail = currentUser
            ?.produceTransactionState("load current user image thumbnail") { thumbnailImage.live }
            ?.value

        LoadedImage(url = thumbnail?.url, modifier = Modifier.size(Dimens.iconMedium))

        HorizontalSpacer(Dimens.space2)

        val username = when (currentUserCacheState) {
            is CacheState.Refreshing, null -> "<loading>"
            is CacheState.Loaded -> currentUserCacheState.cachedValue.name
            else -> "<ERROR>" // TODO expose error information
        }
        Text(
            text = username,
            maxLines = 1,
            modifier = Modifier.align(Alignment.CenterVertically),
        )

        HorizontalSpacer(Dimens.space2)

        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = "Expand",
            modifier = Modifier.requiredSize(Dimens.iconMedium).align(Alignment.CenterVertically),
        )

        DropdownMenu(
            expanded = expandedState.value,
            onDismissRequest = { expandedState.value = false },
        ) {
            CurrentUserDropdownContent(user = currentUser)
        }
    }
}

@Composable
private fun CurrentUserDropdownContent(user: User?) {
    Column(
        modifier = Modifier.padding(Dimens.space3).widthIn(max = CURRENT_USER_DROPDOWN_MAX_WIDTH),
        verticalArrangement = Arrangement.spacedBy(Dimens.space2),
    ) {
        if (user == null) {
            Text("Not authenticated")
        } else {
            Text("Authenticated as ${user.name} [${user.id}]")
        }

        val token = AccessToken.Cache.tokenFlow.collectAsState().value
        if (token != null) {
            Text("Access token: ${token.accessToken}")

            Text("Refreshable: ${token.refreshToken != null}")

            Text("${token.scopes?.size} scopes: ${token.scope}")

            val receivedRelative = liveRelativeDateText(timestamp = token.receivedInstant.toEpochMilli())
            val expiresRelative = liveRelativeDateText(timestamp = token.expiresInstant.toEpochMilli())
            Text(
                "Received at ${token.receivedInstant} ($receivedRelative); " +
                    "expires at ${token.expiresInstant} ($expiresRelative)",
            )
        }

        VerticalSpacer(Dimens.space2)

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = UserRepository::signOut,
        ) {
            Text("Sign out")
        }
    }
}
