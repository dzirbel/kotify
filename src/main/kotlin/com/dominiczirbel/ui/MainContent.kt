package com.dominiczirbel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val PADDING = 10.dp

@Composable
fun MainContent(authenticating: Boolean?, onAuthenticate: () -> Unit) {
    Column(modifier = Modifier.padding(PADDING), verticalArrangement = Arrangement.spacedBy(PADDING, Alignment.Top)) {
        Text(if (authenticating == false) "Authenticated!" else "Canceled authentication")
        Button(
            onClick = onAuthenticate
        ) {
            Text("Authenticate again?")
        }

        Spacer(Modifier.height(PADDING))

        TrackLookup()
    }
}
