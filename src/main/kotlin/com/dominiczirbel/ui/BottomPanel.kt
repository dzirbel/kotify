package com.dominiczirbel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dominiczirbel.ui.constants.Dimens
import com.dominiczirbel.ui.constants.Theme

val ALBUM_ART_SIZE = 150.dp

@Composable
fun BottomPanel() {
    Column(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Box(Modifier.fillMaxWidth().height(Dimens.divider).background(Theme.current.dividerColor))

        Row(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(Theme.current.panelBackground)
                .padding(Dimens.space3)
        ) {
            Box(Modifier.background(Color.Blue).size(ALBUM_ART_SIZE))

            Spacer(Modifier.size(Dimens.space3))

            Column {
                Text("Song name", color = Theme.current.text, fontSize = Dimens.fontBody)
                Spacer(Modifier.size(Dimens.space2))
                Text("Artist name", color = Theme.current.text, fontSize = Dimens.fontBody)
                Spacer(Modifier.size(Dimens.space2))
                Text("Album name", color = Theme.current.text, fontSize = Dimens.fontBody)
            }
        }
    }

}
