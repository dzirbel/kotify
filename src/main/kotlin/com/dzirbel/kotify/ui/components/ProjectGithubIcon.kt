package com.dzirbel.kotify.ui.components

import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.Application
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.util.openInBrowser

@Composable
fun ProjectGithubIcon(modifier: Modifier = Modifier) {
    IconButton(modifier = modifier, onClick = { openInBrowser(Application.github) }) {
        CachedIcon(name = "github", contentDescription = "GitHub")
    }
}
