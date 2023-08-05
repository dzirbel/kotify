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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.network.oauth.AccessToken
import com.dzirbel.kotify.repository.CacheState
import com.dzirbel.kotify.repository.user.UserRepository
import com.dzirbel.kotify.repository.user.UserViewModel
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.components.liveRelativeDateText
import com.dzirbel.kotify.ui.theme.Dimens

private val CURRENT_USER_DROPDOWN_MAX_WIDTH = 500.dp

@Composable
fun CurrentUser() {
    val currentUserCacheState = UserRepository.currentUser.collectAsState().value

    val expandedState = remember { mutableStateOf(false) }
    SimpleTextButton(onClick = { expandedState.value = !expandedState.value }) {
        val currentUser = currentUserCacheState?.cachedValue

        LoadedImage(currentUser?.thumbnailImageUrl, modifier = Modifier.size(Dimens.iconMedium))

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
private fun CurrentUserDropdownContent(user: UserViewModel?) {
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
