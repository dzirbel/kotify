package com.dzirbel.kotify.ui.panel.debug

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.Logger
import com.dzirbel.kotify.cache.SpotifyImageCache
import com.dzirbel.kotify.ui.components.CheckboxWithLabel
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.formatByteSize

private data class ImageCacheSettings(
    val includeInMemory: Boolean = true,
    val includeOnDisk: Boolean = true,
    val includeMiss: Boolean = true,
)

private val imageCacheSettings = mutableStateOf(ImageCacheSettings())

@Composable
fun ImageCacheTab(scrollState: ScrollState) {
    Column(Modifier.fillMaxWidth().background(LocalColors.current.surface3).padding(Dimens.space3)) {
        val inMemoryCount = SpotifyImageCache.state.inMemoryCount
        val diskCount = SpotifyImageCache.state.diskCount
        val totalDiskSize = SpotifyImageCache.state.totalDiskSize
        val totalSizeFormatted = remember(totalDiskSize) { formatByteSize(totalDiskSize.toLong()) }

        Text(
            "$inMemoryCount images cached in memory; " +
                "$diskCount cached on disk for a total of $totalSizeFormatted on disk"
        )

        VerticalSpacer(Dimens.space2)

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { SpotifyImageCache.clear() }
        ) {
            Text("Clear image cache")
        }

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = imageCacheSettings.value.includeInMemory,
            onCheckedChange = { imageCacheSettings.mutate { copy(includeInMemory = it) } },
            label = { Text("Include IN-MEMORY events") }
        )

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = imageCacheSettings.value.includeOnDisk,
            onCheckedChange = { imageCacheSettings.mutate { copy(includeOnDisk = it) } },
            label = { Text("Include ON-DISK events") }
        )

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = imageCacheSettings.value.includeMiss,
            onCheckedChange = { imageCacheSettings.mutate { copy(includeMiss = it) } },
            label = { Text("Include MISS events") }
        )
    }

    EventList(log = Logger.ImageCache, key = imageCacheSettings.value, scrollState = scrollState) { event ->
        when (event.type) {
            Logger.Event.Type.SUCCESS -> imageCacheSettings.value.includeInMemory
            Logger.Event.Type.INFO -> imageCacheSettings.value.includeOnDisk
            Logger.Event.Type.WARNING -> imageCacheSettings.value.includeMiss
            else -> true
        }
    }
}
