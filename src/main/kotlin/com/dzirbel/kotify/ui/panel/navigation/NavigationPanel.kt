package com.dzirbel.kotify.ui.panel.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.ui.components.ProjectGithubIcon
import com.dzirbel.kotify.ui.components.ThemeSwitcher
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.theme.animatedSurfaceBackground
import com.dzirbel.kotify.ui.util.mutate

@Composable
fun NavigationPanel(
    headerVisibleState: MutableTransitionState<Boolean>,
    titles: List<String?>,
) {
    LocalColors.current.withSurface(increment = if (headerVisibleState.targetState) Colors.INCREMENT_SMALL else 0) {
        // TODO avoid animation when returning to a scrolled-down page
        Row(
            modifier = Modifier.fillMaxWidth().animatedSurfaceBackground(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavigationButtons(titles = titles)

            titles[pageStack.value.currentIndex]?.let { title ->
                AnimatedVisibility(visibleState = headerVisibleState, enter = fadeIn(), exit = fadeOut()) {
                    Text(title)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space3)) {
                ThemeSwitcher(modifier = Modifier.align(Alignment.CenterVertically))

                ProjectGithubIcon(modifier = Modifier.align(Alignment.CenterVertically))

                CurrentUser()
            }
        }
    }
}

@Composable
private fun NavigationButtons(titles: List<String?>) {
    Row(Modifier.padding(Dimens.space2)) {
        IconButton(
            enabled = pageStack.value.hasPrevious,
            onClick = { pageStack.mutate { toPrevious() } }
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowLeft,
                contentDescription = "Back",
                modifier = Modifier.size(Dimens.iconMedium)
            )
        }

        val historyExpanded = remember { mutableStateOf(false) }
        IconButton(
            enabled = pageStack.value.pages.size > 1,
            onClick = { historyExpanded.value = true }
        ) {
            Icon(
                imageVector = Icons.Filled.List,
                contentDescription = "History",
                modifier = Modifier.size(Dimens.iconMedium)
            )

            DropdownMenu(
                expanded = historyExpanded.value,
                onDismissRequest = { historyExpanded.value = false }
            ) {
                pageStack.value.pages.forEachIndexed { index, page ->
                    DropdownMenuItem(
                        onClick = {
                            historyExpanded.value = false
                            pageStack.mutate { toIndex(index) }
                        },
                        enabled = index != pageStack.value.currentIndex
                    ) {
                        Text(titles[index] ?: page.toString())
                    }
                }
            }
        }

        IconButton(
            enabled = pageStack.value.hasNext,
            onClick = { pageStack.mutate { toNext() } }
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = "Next",
                modifier = Modifier.size(Dimens.iconMedium)
            )
        }
    }
}
