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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.User
import com.dzirbel.kotify.db.model.UserRepository
import com.dzirbel.kotify.network.oauth.AccessToken
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.components.liveRelativeDateText
import com.dzirbel.kotify.ui.framework.Presenter
import com.dzirbel.kotify.ui.theme.Dimens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

private val CURRENT_USER_DROPDOWN_MAX_WIDTH = 500.dp

private class CurrentUserPresenter(scope: CoroutineScope) : Presenter<User?, CurrentUserPresenter.Event>(
    scope = scope,
    startingEvents = listOf(Event.Load),
    initialState = null,
) {

    sealed class Event {
        object Load : Event()
        object SignOut : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                val user = UserRepository.getCurrentUser()
                user?.let {
                    KotifyDatabase.transaction("load user image") { user.thumbnailImage.loadToCache() }
                }

                mutateState { user }
            }

            is Event.SignOut -> {
                KotifyDatabase.clearSaved()
                AccessToken.Cache.clear()
            }
        }
    }
}

@Composable
fun CurrentUser() {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember { CurrentUserPresenter(scope = scope) }

    val currentUser = presenter.state().safeState
    val userError = presenter.state() is Presenter.StateOrError.Error

    val username = if (userError) "<ERROR>" else currentUser?.name ?: "<loading>"
    val expandedState = remember { mutableStateOf(false) }

    SimpleTextButton(
        enabled = currentUser != null || userError,
        onClick = { expandedState.value = !expandedState.value }
    ) {
        LoadedImage(
            url = currentUser?.thumbnailImage?.cached?.url,
            modifier = Modifier.size(Dimens.iconMedium)
        )

        HorizontalSpacer(Dimens.space2)

        Text(
            text = username,
            maxLines = 1,
            modifier = Modifier.align(Alignment.CenterVertically)
        )

        HorizontalSpacer(Dimens.space2)

        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = "Expand",
            modifier = Modifier.requiredSize(Dimens.iconMedium).align(Alignment.CenterVertically)
        )

        DropdownMenu(
            expanded = expandedState.value,
            onDismissRequest = { expandedState.value = false }
        ) {
            CurrentUserDropdownContent(presenter = presenter, user = currentUser)
        }
    }
}

@Composable
private fun CurrentUserDropdownContent(presenter: CurrentUserPresenter, user: User?) {
    Column(
        modifier = Modifier.padding(Dimens.space3).widthIn(max = CURRENT_USER_DROPDOWN_MAX_WIDTH),
        verticalArrangement = Arrangement.spacedBy(Dimens.space2)
    ) {
        if (user == null) {
            Text("Not authenticated")
        } else {
            Text("Authenticated as ${user.name} [${user.id}]")
        }

        AccessToken.Cache.token?.let { token ->
            Text("Access token: ${token.accessToken}")

            Text("Refreshable: ${token.refreshToken != null}")

            Text("${token.scopes?.size} scopes: ${token.scope}")

            val receivedRelative = liveRelativeDateText(timestamp = token.receivedInstant.toEpochMilli())
            val expiresRelative = liveRelativeDateText(timestamp = token.expiresInstant.toEpochMilli())
            Text(
                "Received at ${token.receivedInstant} ($receivedRelative); " +
                    "expires at ${token.expiresInstant} ($expiresRelative)"
            )
        }

        VerticalSpacer(Dimens.space2)

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = { presenter.emitAsync(CurrentUserPresenter.Event.SignOut) },
        ) {
            Text("Sign out")
        }
    }
}
