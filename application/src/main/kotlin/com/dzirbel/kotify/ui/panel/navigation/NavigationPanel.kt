package com.dzirbel.kotify.ui.panel.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.VectorConverter
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
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dzirbel.kotify.Application
import com.dzirbel.kotify.Settings
import com.dzirbel.kotify.ui.components.ProjectGithubIcon
import com.dzirbel.kotify.ui.components.ThemeSwitcher
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.mutate
import kotlinx.collections.immutable.ImmutableList

private val titleAnimationSpec = TweenSpec<Float>(easing = LinearEasing)

@Composable
fun NavigationPanel(titleVisible: Boolean, titles: ImmutableList<() -> String?>, modifier: Modifier = Modifier) {
    val key = pageStack.value.currentIndex
    val targetElevation = if (titleVisible) Dimens.panelElevationLarge else 0.dp

    val visibilityState = remember(key) { MutableTransitionState(titleVisible) }
    val elevationAnimatable = remember(key) { Animatable(targetElevation, Dp.VectorConverter) }

    LaunchedEffect(key, titleVisible) {
        visibilityState.targetState = titleVisible
        elevationAnimatable.animateTo(targetElevation)
    }

    // use a high z-index to ensure the surface drop shadow is always on top of the content, even if the content has a
    // higher elevation
    Surface(modifier = modifier.zIndex(100f), elevation = elevationAnimatable.value) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavigationButtons(titles = titles)

            titles[pageStack.value.currentIndex].invoke()?.let { currentTitle ->
                AnimatedVisibility(
                    visibleState = visibilityState,
                    enter = fadeIn(titleAnimationSpec),
                    exit = fadeOut(titleAnimationSpec),
                ) {
                    Text(currentTitle)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space3)) {
                ThemeSwitcher(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    onSetColors = { Settings.colors = it },
                )

                ProjectGithubIcon(githubUrl = Application.github, modifier = Modifier.align(Alignment.CenterVertically))

                CurrentUser()
            }
        }
    }
}

@Composable
private fun NavigationButtons(titles: ImmutableList<() -> String?>) {
    Row(Modifier.padding(Dimens.space2)) {
        IconButton(
            enabled = pageStack.value.hasPrevious,
            onClick = { pageStack.mutate { toPrevious() } },
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowLeft,
                contentDescription = "Back",
                modifier = Modifier.size(Dimens.iconMedium),
            )
        }

        val historyExpanded = remember { mutableStateOf(false) }
        IconButton(
            enabled = pageStack.value.pages.size > 1,
            onClick = { historyExpanded.value = true },
        ) {
            Icon(
                imageVector = Icons.Filled.List,
                contentDescription = "History",
                modifier = Modifier.size(Dimens.iconMedium),
            )

            DropdownMenu(
                expanded = historyExpanded.value,
                onDismissRequest = { historyExpanded.value = false },
            ) {
                pageStack.value.pages.forEachIndexed { index, page ->
                    DropdownMenuItem(
                        onClick = {
                            historyExpanded.value = false
                            pageStack.mutate { toIndex(index) }
                        },
                        enabled = index != pageStack.value.currentIndex,
                    ) {
                        Text(titles[index].invoke() ?: page.toString())
                    }
                }
            }
        }

        IconButton(
            enabled = pageStack.value.hasNext,
            onClick = { pageStack.mutate { toNext() } },
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = "Next",
                modifier = Modifier.size(Dimens.iconMedium),
            )
        }
    }
}
