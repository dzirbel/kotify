package com.dzirbel.kotify.ui.panel.debug

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.Logger
import com.dzirbel.kotify.ui.components.CheckboxWithLabel
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.util.mutate

private data class UISettings(
    val includeStates: Boolean = true,
    val includeEvents: Boolean = true,
    val includeErrors: Boolean = true,
    val presenterRegex: String = "",
)

private val uiSettings = mutableStateOf(UISettings())

@Composable
fun UITab(scrollState: ScrollState) {
    Column(Modifier.fillMaxWidth().background(LocalColors.current.surface3).padding(Dimens.space3)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiSettings.value.presenterRegex,
            singleLine = true,
            onValueChange = {
                uiSettings.mutate { copy(presenterRegex = it) }
            },
            label = {
                Text("Presenter class regex (case insensitive)", style = MaterialTheme.typography.overline)
            }
        )

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = uiSettings.value.includeStates,
            onCheckedChange = { uiSettings.mutate { copy(includeStates = it) } },
            label = { Text("Include state changes") }
        )

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = uiSettings.value.includeEvents,
            onCheckedChange = { uiSettings.mutate { copy(includeEvents = it) } },
            label = { Text("Include events") }
        )

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = uiSettings.value.includeErrors,
            onCheckedChange = { uiSettings.mutate { copy(includeErrors = it) } },
            label = { Text("Include errors") }
        )
    }

    val presenterRegex = uiSettings.value.presenterRegex
        .takeIf { it.isNotEmpty() }
        ?.toRegex(RegexOption.IGNORE_CASE)
    EventList(log = Logger.UI, key = uiSettings.value, scrollState = scrollState) { event ->
        val includeEventType = when (event.data.type) {
            Logger.UI.EventType.STATE -> uiSettings.value.includeStates
            Logger.UI.EventType.EVENT -> uiSettings.value.includeEvents
            Logger.UI.EventType.ERROR -> uiSettings.value.includeErrors
        }

        if (includeEventType) {
            val presenterClass = event.data.presenterClass
            presenterClass != null && presenterRegex?.containsMatchIn(presenterClass) != false
        } else {
            false
        }
    }
}
