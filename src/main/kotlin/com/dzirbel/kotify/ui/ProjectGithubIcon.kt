package com.dzirbel.kotify.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.svgResource
import com.dzirbel.kotify.Application
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.openInBrowser

@Composable
fun ProjectGithubIcon(modifier: Modifier = Modifier) {
    IconButton(modifier = modifier, onClick = { openInBrowser(Application.github) }) {
        Icon(
            painter = svgResource("github.svg"),
            contentDescription = "GitHub",
            modifier = Modifier.size(Dimens.iconMedium)
        )
    }
}
