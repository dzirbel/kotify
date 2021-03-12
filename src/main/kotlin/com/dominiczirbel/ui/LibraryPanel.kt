package com.dominiczirbel.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.mutate

@Composable
fun LibraryPanel(pageStack: MutableState<PageStack>) {
    Column {
        Text(
            text = "Library",
            fontSize = Dimens.fontTitle,
            modifier = Modifier.padding(Dimens.space3)
        )

        Spacer(Modifier.height(Dimens.space3))

        MaxWidthButton(
            text = "Artists",
            selected = pageStack.value.current == ArtistsPage,
            onClick = { pageStack.mutate { to(ArtistsPage) } }
        )

        MaxWidthButton(
            text = "Albums",
            selected = pageStack.value.current == AlbumsPage,
            onClick = { pageStack.mutate { to(AlbumsPage) } }
        )

        MaxWidthButton(
            text = "Songs",
            selected = pageStack.value.current == TracksPage,
            onClick = { pageStack.mutate { to(TracksPage) } }
        )
    }
}

@Composable
private fun MaxWidthButton(text: String, selected: Boolean, onClick: () -> Unit) {
    SimpleTextButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Text(
            text = text,
            fontSize = Dimens.fontBody,
            modifier = Modifier.fillMaxWidth(),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
