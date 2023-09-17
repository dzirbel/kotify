package com.dzirbel.kotify.ui.panel.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import com.dzirbel.kotify.log.Log
import com.dzirbel.kotify.repository.DataSource
import com.dzirbel.kotify.repository.Repository
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.LocalAlbumRepository
import com.dzirbel.kotify.ui.LocalAlbumTracksRepository
import com.dzirbel.kotify.ui.LocalArtistAlbumsRepository
import com.dzirbel.kotify.ui.LocalArtistRepository
import com.dzirbel.kotify.ui.LocalPlayer
import com.dzirbel.kotify.ui.LocalPlaylistRepository
import com.dzirbel.kotify.ui.LocalPlaylistTracksRepository
import com.dzirbel.kotify.ui.LocalRatingRepository
import com.dzirbel.kotify.ui.LocalSavedAlbumRepository
import com.dzirbel.kotify.ui.LocalSavedArtistRepository
import com.dzirbel.kotify.ui.LocalSavedPlaylistRepository
import com.dzirbel.kotify.ui.LocalSavedTrackRepository
import com.dzirbel.kotify.ui.LocalTrackRepository
import com.dzirbel.kotify.ui.LocalUserRepository
import com.dzirbel.kotify.ui.components.CheckboxWithLabel
import com.dzirbel.kotify.ui.components.HorizontalDivider
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.components.ToggleButtonGroup
import com.dzirbel.kotify.ui.components.TriStateCheckboxWithLabel
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.SortableProperty
import com.dzirbel.kotify.ui.components.adapter.compareByNullable
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.groupToggleState
import com.dzirbel.kotify.util.capitalize
import com.dzirbel.kotify.util.collections.plusOrMinus
import com.dzirbel.kotify.util.coroutines.MergedMutex
import com.dzirbel.kotify.util.coroutines.lockedState
import com.dzirbel.kotify.util.coroutines.mergeFlows
import com.dzirbel.kotify.util.time.formatShortDuration
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.runningFold

@Composable
fun RepositoryTab() {
    val repositoryLogs = persistentListOf(
        LocalArtistRepository.current.log,
        LocalArtistAlbumsRepository.current.log,
        LocalAlbumRepository.current.log,
        LocalAlbumTracksRepository.current.log,
        LocalPlaylistRepository.current.log,
        LocalPlaylistTracksRepository.current.log,
        LocalTrackRepository.current.log,
        LocalUserRepository.current.log,
    )

    val savedRepositoryLogs = persistentListOf(
        LocalSavedAlbumRepository.current.log,
        LocalSavedArtistRepository.current.log,
        LocalSavedPlaylistRepository.current.log,
        LocalSavedTrackRepository.current.log,
    )

    val otherRepositoryLogs = persistentListOf(LocalRatingRepository.current.log, LocalPlayer.current.log)

    val allLogs = repositoryLogs + savedRepositoryLogs + otherRepositoryLogs

    val logMutex = remember { MergedMutex(allLogs.map { it.writeLock }) }
    val enabledLogs = remember { mutableStateOf(allLogs.toPersistentSet()) }
    val selectedDataSources = remember { mutableStateOf(persistentSetOf<DataSource>()) }

    // do not filter if all or none are selected (no-op filter)
    val filterDataSource = selectedDataSources.value.size in 1 until DataSource.entries.size

    LogList(
        logs = allLogs,
        logMutex = logMutex,
        display = RepositoryLogEventDisplay,
        sortProperties = persistentListOf(
            RepositoryEventDatabaseTimeProperty,
            RepositoryEventRemoteTimeProperty,
            RepositoryEventOverheadTimeProperty,
        ),
        filter = if (filterDataSource) {
            { it.log in enabledLogs.value && it.event.data.source in selectedDataSources.value }
        } else {
            { it.log in enabledLogs.value }
        },
        filterKey = Pair(enabledLogs.value, selectedDataSources.value),
        onResetFilter = {
            enabledLogs.value = allLogs.toPersistentSet()
            selectedDataSources.value = persistentSetOf()
        },
        canResetFilter = enabledLogs.value.size < allLogs.size || selectedDataSources.value.isNotEmpty(),
    ) { eventCleared ->
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space2), modifier = Modifier.padding(Dimens.space2)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.space1)) {
                LogListToggle(
                    logs = repositoryLogs,
                    enabledLogs = enabledLogs.value,
                    onSetEnabledLogs = { enabledLogs.value = it },
                    eventCleared = eventCleared,
                    title = "Repositories",
                    modifier = Modifier.weight(1f),
                )

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.space4)) {
                    LogListToggle(
                        logs = savedRepositoryLogs,
                        enabledLogs = enabledLogs.value,
                        onSetEnabledLogs = { enabledLogs.value = it },
                        eventCleared = eventCleared,
                        title = "Saved",
                    )

                    LogListToggle(
                        logs = otherRepositoryLogs,
                        enabledLogs = enabledLogs.value,
                        onSetEnabledLogs = { enabledLogs.value = it },
                        eventCleared = eventCleared,
                        title = "Other",
                    )
                }
            }

            ToggleButtonGroup(
                elements = DataSource.entries.toImmutableList(),
                selectedElements = selectedDataSources.value,
                onSelectElements = { selectedDataSources.value = it },
                content = { dataSource ->
                    CachedIcon(name = dataSource.iconName, size = Dimens.iconSmall)

                    HorizontalSpacer(Dimens.space2)

                    val scope = rememberCoroutineScope()
                    val name = dataSource.name.lowercase().capitalize()
                    val count: Int? = remember(eventCleared) {
                        logMutex.lockedState(
                            scope = scope,
                            initializeWithLock = {
                                allLogs.sumOf { log ->
                                    log.events.count { event ->
                                        !eventCleared(event) && event.data.source == dataSource
                                    }
                                }
                            },
                        ) { initial ->
                            allLogs.mergeFlows { it.eventsFlow }
                                .runningFold(initial) { count, event ->
                                    if (!eventCleared(event) && event.data.source == dataSource) count + 1 else count
                                }
                        }
                    }
                        .collectAsState()
                        .value

                    Text("$name [$count]", maxLines = 1)
                },
            )
        }
    }
}

@Composable
private fun <T> LogListToggle(
    logs: ImmutableList<Log<T>>,
    enabledLogs: PersistentSet<Log<T>>,
    onSetEnabledLogs: (PersistentSet<Log<T>>) -> Unit,
    eventCleared: (Log.Event<T>) -> Boolean,
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        val state = logs.groupToggleState(enabledLogs)
        TriStateCheckboxWithLabel(
            state = state,
            onClick = {
                when (state) {
                    ToggleableState.On -> onSetEnabledLogs(enabledLogs.removeAll(logs))
                    ToggleableState.Off, ToggleableState.Indeterminate -> onSetEnabledLogs(enabledLogs.addAll(logs))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(title, fontWeight = FontWeight.Bold)
        }

        HorizontalDivider()
        VerticalSpacer(Dimens.space3)

        for (log in logs) {
            val scope = rememberCoroutineScope()
            val count: Int? = remember(log, eventCleared) {
                log.writeLock.lockedState(
                    scope = scope,
                    initializeWithLock = {
                        log.events.count { !eventCleared(it) }
                    },
                ) { initial ->
                    log.eventsFlow.runningFold(initial) { count, event ->
                        if (eventCleared(event)) count else count + 1
                    }
                }
            }
                .collectAsState()
                .value

            CheckboxWithLabel(
                checked = log in enabledLogs,
                onCheckedChange = { checked -> onSetEnabledLogs(enabledLogs.plusOrMinus(log, checked)) },
                label = {
                    val name = log.name.removeSuffix("Repository")
                    Text("$name [$count]", maxLines = 1)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

val DataSource.iconName: String
    get() {
        return when (this) {
            DataSource.MEMORY -> "data-table"
            DataSource.DATABASE -> "database"
            DataSource.REMOTE -> "cloud-download"
        }
    }

private object RepositoryLogEventDisplay : LogEventDisplay<Repository.LogData> {
    @Suppress("MagicNumber")
    override fun content(event: Log.Event<Repository.LogData>): String {
        return buildString {
            event.data.timeInDb?.let { appendLine("In database: ${it.formatShortDuration(decimals = 3)}") }
            event.data.timeInRemote?.let { appendLine("In remote: ${it.formatShortDuration(decimals = 3)}") }

            // do not show overhead if it is just equal to the event duration
            if (event.data.timeInDb != null || event.data.timeInRemote != null) {
                event.duration
                    ?.let { event.data.overhead(totalDuration = it) }
                    ?.let { appendLine("Overhead: ${it.formatShortDuration(decimals = 3)}") }
            }

            event.content?.let { append(it) }
        }
    }

    @Composable
    override fun Icon(event: Log.Event<Repository.LogData>, modifier: Modifier) {
        CachedIcon(name = event.data.source.iconName, modifier = modifier, tint = event.type.iconColor)
    }
}

private object RepositoryEventDatabaseTimeProperty : SortableProperty<Log.Event<Repository.LogData>> {
    override val title = "Time in database"
    override val defaultSortOrder = SortOrder.DESCENDING
    override val terminalSort = true

    override fun compare(
        sortOrder: SortOrder,
        first: Log.Event<Repository.LogData>,
        second: Log.Event<Repository.LogData>,
    ): Int {
        return sortOrder.compareByNullable(first, second) { it.data.timeInDb }
    }
}

private object RepositoryEventRemoteTimeProperty : SortableProperty<Log.Event<Repository.LogData>> {
    override val title = "Time in remote"
    override val defaultSortOrder = SortOrder.DESCENDING
    override val terminalSort = true

    override fun compare(
        sortOrder: SortOrder,
        first: Log.Event<Repository.LogData>,
        second: Log.Event<Repository.LogData>,
    ): Int {
        return sortOrder.compareByNullable(first, second) { it.data.timeInRemote }
    }
}

private object RepositoryEventOverheadTimeProperty : SortableProperty<Log.Event<Repository.LogData>> {
    override val title = "Time in overhead"
    override val defaultSortOrder = SortOrder.DESCENDING
    override val terminalSort = true

    override fun compare(
        sortOrder: SortOrder,
        first: Log.Event<Repository.LogData>,
        second: Log.Event<Repository.LogData>,
    ): Int {
        return sortOrder.compareByNullable(first, second) { event ->
            event.duration?.let { event.data.overhead(totalDuration = it) }
        }
    }
}
