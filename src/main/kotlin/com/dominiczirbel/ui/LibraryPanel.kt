package com.dominiczirbel.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dominiczirbel.ui.constants.Dimens
import com.dominiczirbel.ui.constants.Theme

@Composable
fun LibraryPanel() {
    Column {
        Text("Library", color = Theme.current.text, fontSize = Dimens.fontTitle)
        Spacer(Modifier.height(Dimens.space4))

        Text("Artists", color = Theme.current.text, fontSize = Dimens.fontBody)
        Spacer(Modifier.height(Dimens.space3))
        Text("Albums", color = Theme.current.text, fontSize = Dimens.fontBody)
        Spacer(Modifier.height(Dimens.space3))
        Text("Songs", color = Theme.current.text, fontSize = Dimens.fontBody)
    }
}
