package com.dzirbel.kotify.ui.panel.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.network.oauth.AccessToken
import com.dzirbel.kotify.network.oauth.OAuth
import com.dzirbel.kotify.repository.CacheState
import com.dzirbel.kotify.repository.user.UserRepository
import com.dzirbel.kotify.repository.user.UserViewModel
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.CopyButton
import com.dzirbel.kotify.ui.components.HelpTooltip
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.components.liveRelativeDateText
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.util.maxIntrinsicWidth
import com.dzirbel.kotify.ui.util.openInBrowser
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val CURRENT_USER_DROPDOWN_MAX_WIDTH = 500.dp

@Composable
fun CurrentUser() {
    val currentUserCacheState: CacheState<UserViewModel>? = UserRepository.currentUser.collectAsState().value
    val currentUser = currentUserCacheState?.cachedValue

    val expandedState = remember { mutableStateOf(false) }

    SimpleTextButton(onClick = { expandedState.value = !expandedState.value }) {
        LoadedImage(modifier = Modifier.size(Dimens.iconMedium), key = currentUser?.id) { size ->
            currentUser?.imageUrlFor(size)
        }

        HorizontalSpacer(Dimens.space2)

        when (currentUserCacheState) {
            is CacheState.Loaded -> {
                Text(
                    text = currentUserCacheState.cachedValue.name,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }

            is CacheState.Refreshing, null -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.iconMedium).align(Alignment.CenterVertically),
                )
            }

            is CacheState.NotFound -> {
                val currentUserId = UserRepository.currentUserId.collectAsState().value
                Text(
                    text = currentUserId ?: "<user not found>",
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }

            is CacheState.Error -> {
                val currentUserId = UserRepository.currentUserId.collectAsState().value
                Text(
                    text = currentUserId ?: "<error>",
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
        }

        HorizontalSpacer(Dimens.space2)

        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = "Expand",
            modifier = Modifier.size(Dimens.iconMedium).align(Alignment.CenterVertically),
        )

        CurrentUserDropdown(
            currentUserCacheState = currentUserCacheState,
            expanded = expandedState.value,
            onDismissRequest = { expandedState.value = false },
        )
    }
}

@Composable
private fun CurrentUserDropdown(
    currentUserCacheState: CacheState<UserViewModel>?,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
) {
    var showDetails by remember { mutableStateOf(false) }

    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(Dimens.space3)
                .width(IntrinsicSize.Max)
                .widthIn(max = CURRENT_USER_DROPDOWN_MAX_WIDTH),
            verticalArrangement = Arrangement.spacedBy(Dimens.space2),
        ) {
            val currentUserId = UserRepository.currentUserId.collectAsState().value

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                when (currentUserCacheState) {
                    is CacheState.Loaded ->
                        Text(
                            text = "Logged in as ${currentUserCacheState.cachedValue.name} [$currentUserId]",
                            modifier = Modifier.weight(1f),
                        )

                    is CacheState.Refreshing, null ->
                        Text("Loading data for current user; ID: $currentUserId", modifier = Modifier.weight(1f))

                    is CacheState.NotFound ->
                        Text("Current user data not found; ID: $currentUserId", modifier = Modifier.weight(1f))

                    is CacheState.Error ->
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Error loading current user data; ID: $currentUserId")
                            Text(
                                text = currentUserCacheState.throwable.stackTraceToString(),
                                color = LocalColors.current.error,
                            )
                        }
                }

                HorizontalSpacer(Dimens.space5)

                Button(onClick = UserRepository::signOut) {
                    Text("Sign out", maxLines = 1)
                }
            }

            SimpleTextButton(
                onClick = { showDetails = !showDetails },
            ) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Technical details")

                    Icon(
                        imageVector = if (showDetails) {
                            Icons.Default.KeyboardArrowDown
                        } else {
                            Icons.Default.KeyboardArrowUp
                        },
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconSmall),
                    )
                }
            }

            if (showDetails) {
                AccessTokenDetails(token = AccessToken.Cache.tokenFlow.collectAsState().value)
            }
        }
    }
}

@Composable
private fun AccessTokenDetails(token: AccessToken?) {
    Column(Modifier.maxIntrinsicWidth(0.dp)) {
        if (token != null) {
            Text("Access token:")
            VerticalSpacer(Dimens.space2)
            OutlinedTextField(
                value = token.accessToken,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { CopyButton(token.accessToken, iconSize = Dimens.iconSmall) },
            )

            VerticalSpacer(Dimens.space4)

            val received = remember(token.receivedInstant) {
                accessTokenDateFormat.format(token.receivedInstant.atZone(ZoneId.systemDefault()))
            }
            val expires = remember(token.expiresInstant) {
                accessTokenDateFormat.format(token.expiresInstant.atZone(ZoneId.systemDefault()))
            }
            val receivedRelative = liveRelativeDateText(timestamp = token.receivedInstant.toEpochMilli())
            val expiresRelative = liveRelativeDateText(timestamp = token.expiresInstant.toEpochMilli())
            Text("Received $received ($receivedRelative)")
            VerticalSpacer(Dimens.space2)
            Text("Expires $expires ($expiresRelative)")

            VerticalSpacer(Dimens.space4)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Refreshable: ${token.refreshToken != null}")

                if (token.refreshToken != null) {
                    var refreshing by remember { mutableStateOf(false) }
                    SimpleTextButton(
                        enabled = !refreshing,
                        onClick = {
                            refreshing = true
                            AccessToken.Cache.refresh()
                                ?.invokeOnCompletion { refreshing = false }
                        },
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space2)) {
                            if (refreshing) {
                                CircularProgressIndicator(Modifier.size(Dimens.iconSmall))
                            } else {
                                Icon(Icons.Default.Refresh, null, Modifier.size(Dimens.iconSmall))
                            }

                            Text("Refresh now")
                        }
                    }
                }
            }

            val scopes = token.scopes
            if (scopes != null) {
                VerticalSpacer(Dimens.space4)

                Text("${scopes.size} scopes granted:")

                Column(Modifier.padding(start = Dimens.space2)) {
                    Text("Default scopes")
                    for (scope in OAuth.Scope.entries) {
                        if (scope.requestByDefault) {
                            ScopeStatus(token, scope, modifier = Modifier.padding(start = Dimens.space2))
                        }
                    }

                    Text("Other scopes")
                    for (scope in OAuth.Scope.entries) {
                        if (!scope.requestByDefault) {
                            ScopeStatus(token, scope, modifier = Modifier.padding(start = Dimens.space2))
                        }
                    }
                }
            }

            VerticalSpacer(Dimens.space4)

            SimpleTextButton(onClick = { openInBrowser(OAuth.SPOTIFY_APPS_URL) }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.space2)) {
                    CachedIcon("open-in-new", size = Dimens.iconSmall)
                    Text("Manage apps in Spotify")
                }
            }
        } else {
            Row(Modifier.padding(Dimens.space2).fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(Modifier.size(Dimens.iconMedium))
            }
        }
    }
}

private val accessTokenDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

@Composable
private fun ScopeStatus(token: AccessToken, scope: OAuth.Scope, modifier: Modifier = Modifier) {
    val hasScope = token.hasScope(scope)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
    ) {
        CachedIcon(
            name = if (hasScope) "check-circle" else "cancel",
            size = Dimens.iconTiny,
            tint = if (hasScope) Color.Green else Color.Red,
        )

        Text(scope.scope)

        HelpTooltip(tooltip = scope.officialDescription, size = Dimens.iconTiny)
    }
}
