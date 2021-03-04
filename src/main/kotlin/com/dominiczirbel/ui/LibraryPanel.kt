package com.dominiczirbel.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens

@Composable
fun LibraryPanel() {
    Column {
        Text(
            text = "Library",
            color = Colors.current.text,
            fontSize = Dimens.fontTitle,
            modifier = Modifier.padding(Dimens.space3)
        )

        Spacer(Modifier.height(Dimens.space3))

        MaxWidthButton(
            text = "Artists",
            onClick = { }
        )

        MaxWidthButton(
            text = "Albums",
            onClick = { }
        )

        MaxWidthButton(
            text = "Songs",
            onClick = { }
        )
    }
}

@Composable
private fun MaxWidthButton(text: String, onClick: () -> Unit) {
    TextButton(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Dimens.space3),
        shape = RoundedCornerShape(0.dp),
        onClick = onClick
    ) {
        Text(text, color = Colors.current.text, fontSize = Dimens.fontBody, modifier = Modifier.fillMaxWidth())
    }
}
