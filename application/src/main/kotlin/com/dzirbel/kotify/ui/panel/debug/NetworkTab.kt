package com.dzirbel.kotify.ui.panel.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.NetworkLogger
import com.dzirbel.kotify.log.Log
import com.dzirbel.kotify.network.DelayInterceptor
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.AppliedTextField
import com.dzirbel.kotify.ui.components.CheckboxWithLabel
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.consumeKeyEvents

@Composable
fun NetworkTab() {
    val filterApi = remember { mutableStateOf(false) }
    val filterIncoming = remember { mutableStateOf(false) }
    val filterOutgoing = remember { mutableStateOf(false) }

    LogList(
        log = NetworkLogger.log,
        display = NetworkLogEventDisplay,
        filter = { (_, event) ->
            (!filterApi.value || event.data.isSpotifyApi) &&
                (!filterIncoming.value || event.data.isResponse) &&
                (!filterOutgoing.value || event.data.isRequest)
        },
        filterKey = Triple(filterApi.value, filterIncoming.value, filterOutgoing.value),
        onResetFilter = {
            filterApi.value = false
            filterIncoming.value = false
            filterOutgoing.value = false
        },
        canResetFilter = filterApi.value || filterIncoming.value || filterOutgoing.value,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Dimens.space3),
            verticalArrangement = Arrangement.spacedBy(Dimens.space2),
        ) {
            AppliedTextField(
                value = DelayInterceptor.delayMs.toString(),
                label = "Network delay (ms)",
                modifier = Modifier.fillMaxWidth().consumeKeyEvents(),
                applyValue = { value ->
                    val valueLong = value.toLongOrNull()
                    valueLong?.let { DelayInterceptor.delayMs = it }
                    valueLong != null
                },
            )

            CheckboxWithLabel(
                modifier = Modifier.fillMaxWidth(),
                checked = filterApi.value,
                onCheckedChange = { filterApi.value = it },
                label = { Text("Spotify API calls only") },
            )

            CheckboxWithLabel(
                modifier = Modifier.fillMaxWidth(),
                checked = filterIncoming.value,
                onCheckedChange = { checked ->
                    filterIncoming.value = checked
                    if (checked) filterOutgoing.value = false
                },
                label = {
                    CachedIcon(name = "download", size = Dimens.iconTiny)
                    HorizontalSpacer(Dimens.space1)
                    Text("Incoming responses only")
                },
            )

            CheckboxWithLabel(
                modifier = Modifier.fillMaxWidth(),
                checked = filterOutgoing.value,
                onCheckedChange = { checked ->
                    filterOutgoing.value = checked
                    if (checked) filterIncoming.value = false
                },
                label = {
                    CachedIcon(name = "upload", size = Dimens.iconTiny)
                    HorizontalSpacer(Dimens.space1)
                    Text("Outgoing requests only")
                },
            )
        }
    }
}

private object NetworkLogEventDisplay : LogEventDisplay<NetworkLogger.LogData> {
    @Composable
    override fun Icon(event: Log.Event<NetworkLogger.LogData>, modifier: Modifier) {
        CachedIcon(
            name = if (event.data.isRequest) "upload" else "download",
            modifier = modifier,
            tint = event.type.iconColor,
        )
    }
}
