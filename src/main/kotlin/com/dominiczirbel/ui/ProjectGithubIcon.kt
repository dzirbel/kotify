package com.dominiczirbel.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.svgResource
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.openInBrowser

private const val PROJECT_GITHUB = "https://github.com/dzirbel/spotify"

@Composable
fun ProjectGithubIcon(modifier: Modifier = Modifier) {
    IconButton(modifier = modifier, onClick = { openInBrowser(PROJECT_GITHUB) }) {
        Icon(
            painter = svgResource("github.svg"),
            contentDescription = "GitHub",
            modifier = Modifier.size(Dimens.iconMedium)
        )
    }
}
