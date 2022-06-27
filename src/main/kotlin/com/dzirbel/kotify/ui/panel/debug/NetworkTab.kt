package com.dzirbel.kotify.ui.panel.debug

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.Logger
import com.dzirbel.kotify.network.DelayInterceptor
import com.dzirbel.kotify.ui.components.AppliedTextField
import com.dzirbel.kotify.ui.components.CheckboxWithLabel
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.mutate

private data class NetworkSettings(
    val filterApi: Boolean = false,
    val filterIncoming: Boolean = false,
    val filterOutgoing: Boolean = false,
)

private val networkSettings = mutableStateOf(NetworkSettings())

@Composable
fun NetworkTab(scrollState: ScrollState) {
    Column(Modifier.fillMaxWidth().padding(Dimens.space3)) {
        AppliedTextField(
            value = DelayInterceptor.delayMs.toString(),
            label = "Network delay (ms)",
            modifier = Modifier.fillMaxWidth(),
            applyValue = { value ->
                val valueLong = value.toLongOrNull()
                valueLong?.let { DelayInterceptor.delayMs = it }
                valueLong != null
            },
        )

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = networkSettings.value.filterApi,
            onCheckedChange = { networkSettings.mutate { copy(filterApi = it) } },
            label = { Text("Spotify API calls only") },
        )

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = networkSettings.value.filterIncoming,
            onCheckedChange = { networkSettings.mutate { copy(filterIncoming = it) } },
            label = { Text("Incoming responses only") },
        )

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = networkSettings.value.filterOutgoing,
            onCheckedChange = { networkSettings.mutate { copy(filterOutgoing = it) } },
            label = { Text("Outgoing requests only") },
        )
    }

    EventList(log = Logger.Network, key = networkSettings.value, scrollState = scrollState) { event ->
        (!networkSettings.value.filterApi || event.data.isSpotifyApi) &&
            (!networkSettings.value.filterIncoming || event.data.isResponse) &&
            (!networkSettings.value.filterOutgoing || event.data.isRequest)
    }
}
