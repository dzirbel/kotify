package com.dzirbel.kotify.ui.unauthenticated

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.ui.components.ProjectGithubIcon
import com.dzirbel.kotify.ui.components.ThemeSwitcher
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.theme.surfaceBackground

private val MIN_WIDTH = 500.dp
private const val WIDTH_FRACTION = 0.5f

/**
 * Displays the un-authenticated landing page and authentication flow.
 */
@Composable
fun Unauthenticated() {
    val state = remember { mutableStateOf(AuthenticationState()) }
    val scrollState: ScrollState = rememberScrollState(0)

    LocalColors.current.withSurface {
        Box(Modifier.fillMaxSize().surfaceBackground()) {
            Box(Modifier.verticalScroll(scrollState).fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .padding(vertical = Dimens.space5)
                        .defaultMinSize(minWidth = MIN_WIDTH)
                        .fillMaxWidth(fraction = WIDTH_FRACTION)
                        .fillMaxHeight()
                        .align(Alignment.TopCenter),
                    verticalArrangement = Arrangement.spacedBy(Dimens.space3, Alignment.Top),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        ThemeSwitcher()
                        ProjectGithubIcon()
                    }

                    val oauth = state.value.oauth
                    if (oauth == null) {
                        LandingPage(state)
                    } else {
                        FlowInProgress(state, oauth)
                    }
                }
            }

            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd),
                adapter = rememberScrollbarAdapter(scrollState),
            )
        }
    }
}
