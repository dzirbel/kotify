package com.dzirbel.kotify.ui.page.library.tracks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.theme.Dimens
import kotlinx.coroutines.Dispatchers

@Composable
fun TracksLibraryState() {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember { TracksLibraryStatePresenter(scope) }

    presenter.state().stateOrThrow?.let { state ->
        val tracks = state.tracks

        if (tracks == null) {
            InvalidateButton(
                refreshing = state.refreshingSavedTracks,
                updated = state.tracksUpdated,
                updatedFallback = "Tracks never synced",
            ) {
                presenter.emitAsync(TracksLibraryStatePresenter.Event.RefreshSavedTracks)
            }

            return
        }

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val totalSaved = tracks.size
                val totalCached = tracks.count { it.second != null }
                val simplified = tracks.count { it.second != null && it.second?.fullUpdatedTime == null }
                val full = tracks.count { it.second?.fullUpdatedTime != null }

                Text("$totalSaved Saved Tracks", modifier = Modifier.padding(end = Dimens.space3))

                InvalidateButton(
                    refreshing = state.refreshingSavedTracks,
                    updated = state.tracksUpdated,
                ) {
                    presenter.emitAsync(TracksLibraryStatePresenter.Event.RefreshSavedTracks)
                }

                val inCacheExpanded = remember { mutableStateOf(false) }
                SimpleTextButton(onClick = { inCacheExpanded.value = true }) {
                    val allInCache = full == totalSaved
                    CachedIcon(
                        name = if (allInCache) "check-circle" else "cancel",
                        size = Dimens.iconSmall,
                        tint = if (allInCache) Color.Green else Color.Red
                    )

                    HorizontalSpacer(Dimens.space1)

                    Text(
                        "$totalCached/$totalSaved in cache" +
                            simplified.takeIf { it > 0 }?.let { " ($it simplified)" }.orEmpty()
                    )

                    DropdownMenu(
                        expanded = inCacheExpanded.value,
                        onDismissRequest = { inCacheExpanded.value = false }
                    ) {
                        DropdownMenuItem(
                            enabled = full < totalSaved,
                            onClick = {
                                presenter.emitAsync(TracksLibraryStatePresenter.Event.FetchMissingTracks)
                                inCacheExpanded.value = false
                            }
                        ) {
                            Text("Fetch missing")
                        }

                        DropdownMenuItem(
                            onClick = {
                                presenter.emitAsync(TracksLibraryStatePresenter.Event.InvalidateTracks)
                                inCacheExpanded.value = false
                            }
                        ) {
                            Text("Invalidate all")
                        }
                    }
                }
            }
        }
    }
}
