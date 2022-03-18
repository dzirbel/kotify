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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.framework.rememberPresenter
import com.dzirbel.kotify.ui.theme.Dimens

@Composable
fun TracksLibraryState() {
    val presenter = rememberPresenter(::TracksLibraryStatePresenter)

    presenter.state().stateOrThrow?.let { state ->
        if (state.savedTrackIds == null) {
            InvalidateButton(
                refreshing = state.refreshingSavedTracks,
                updated = state.tracksUpdated,
                updatedFallback = "Tracks never synced",
            ) {
                presenter.emitAsync(TracksLibraryStatePresenter.Event.Load(fromCache = false))
            }

            return
        }

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val totalSaved = state.tracks.size
                val totalCached = state.tracks.count { it != null }
                val full = state.tracks.count { it?.fullUpdatedTime != null }
                val simplified = totalCached - full

                Text("$totalSaved Saved Tracks", modifier = Modifier.padding(end = Dimens.space3))

                InvalidateButton(
                    refreshing = state.refreshingSavedTracks,
                    updated = state.tracksUpdated,
                ) {
                    presenter.emitAsync(TracksLibraryStatePresenter.Event.Load(fromCache = false))
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
