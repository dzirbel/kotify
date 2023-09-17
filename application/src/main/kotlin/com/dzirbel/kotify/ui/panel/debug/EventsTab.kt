package com.dzirbel.kotify.ui.panel.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.EventLog
import com.dzirbel.kotify.Runtime
import com.dzirbel.kotify.Settings
import com.dzirbel.kotify.network.oauth.AccessToken
import com.dzirbel.kotify.ui.components.CheckboxWithLabel
import com.dzirbel.kotify.ui.invalidateRootComposable
import com.dzirbel.kotify.ui.theme.Dimens

@Composable
fun EventsTab() {
    LogList(listOf(EventLog, AccessToken.Cache.log), annotateTitlesByLog = false) {
        Column(Modifier.padding(Dimens.space3), verticalArrangement = Arrangement.spacedBy(Dimens.space2)) {
            Text("UI Instrumentation")

            CheckboxWithLabel(
                modifier = Modifier.fillMaxWidth(),
                checked = Settings.instrumentationHighlightCompositions,
                onCheckedChange = { Settings.instrumentationHighlightCompositions = it },
                enabled = Runtime.debug,
                label = { Text("Highlight compositions") },
            )

            CheckboxWithLabel(
                modifier = Modifier.fillMaxWidth(),
                checked = Settings.instrumentationMetricsPanels,
                onCheckedChange = { Settings.instrumentationMetricsPanels = it },
                enabled = Runtime.debug,
                label = { Text("Show composition metrics panels") },
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { invalidateRootComposable() },
            ) {
                Text("Recompose window")
            }
        }
    }
}
