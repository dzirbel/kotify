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
import com.dzirbel.kotify.repository.ratingRepositories
import com.dzirbel.kotify.repository.repositories
import com.dzirbel.kotify.repository.repositoryLogs
import com.dzirbel.kotify.repository.savedRepositories
import com.dzirbel.kotify.ui.CachedIcon
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
import com.dzirbel.kotify.util.coroutines.mapIn
import com.dzirbel.kotify.util.time.formatShortDuration
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentSet

// TODO persist filter/sort/scroll even if tab is not in UI
@Composable
fun RepositoryTab() {
    val enabledLogs = remember { mutableStateOf(repositoryLogs.toSet().toPersistentSet()) }
    val selectedDataSources = remember { mutableStateOf(persistentSetOf<DataSource>()) }

    // do not filter if all or none are selected (no-op filter)
    val filterDataSource = selectedDataSources.value.size in 1 until DataSource.entries.size

    LogList(
        logs = repositoryLogs,
        display = RepositoryLogEventDisplay,
        sortProperties = persistentListOf(
            RepositoryEventDatabaseTimeProperty,
            RepositoryEventRemoteTimeProperty,
            RepositoryEventOverheadTimeProperty,
        ),
        // hack: use let{} to ensure a new lambda object is produced on change
        filter = enabledLogs.value.let { logs ->
            selectedDataSources.value.let { dataSources ->
                if (filterDataSource) {
                    { it.log in logs && it.event.data.source in dataSources }
                } else {
                    { it.log in logs }
                }
            }
        },
    ) { eventsFlow ->
        val scope = rememberCoroutineScope()
        val (countsByLog, countsByDataSource) = remember(eventsFlow) {
            eventsFlow.mapIn(scope) { events ->
                val countsByLog = mutableMapOf<Log<*>, Int>()
                val countsByDataSource = IntArray(DataSource.entries.size)
                for (logAndEvent in events) {
                    countsByDataSource[logAndEvent.event.data.source.ordinal]++
                    countsByLog.compute(logAndEvent.log) { _, count -> (count ?: 0) + 1 }
                }

                countsByLog.toImmutableMap() to countsByDataSource
            }
        }
            .collectAsState()
            .value

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space2), modifier = Modifier.padding(Dimens.space2)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.space1)) {
                LogListToggle(
                    logs = remember { repositories.map { it.log }.toImmutableList() },
                    countsByLog = countsByLog,
                    enabledLogs = enabledLogs.value,
                    onSetEnabledLogs = { enabledLogs.value = it },
                    title = "Repositories",
                    modifier = Modifier.weight(1f),
                )

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.space4)) {
                    LogListToggle(
                        logs = remember { savedRepositories.map { it.log }.toImmutableList() },
                        countsByLog = countsByLog,
                        enabledLogs = enabledLogs.value,
                        onSetEnabledLogs = { enabledLogs.value = it },
                        title = "Saved",
                    )

                    LogListToggle(
                        logs = remember { ratingRepositories.map { it.log }.toImmutableList() },
                        countsByLog = countsByLog,
                        enabledLogs = enabledLogs.value,
                        onSetEnabledLogs = { enabledLogs.value = it },
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

                    val name = dataSource.name.lowercase().capitalize()
                    val count = countsByDataSource[dataSource.ordinal]
                    Text("$name [$count]")
                },
            )
        }
    }
}

@Composable
private fun <T> LogListToggle(
    logs: ImmutableList<Log<T>>,
    countsByLog: ImmutableMap<Log<*>, Int>,
    enabledLogs: PersistentSet<Log<T>>,
    onSetEnabledLogs: (PersistentSet<Log<T>>) -> Unit,
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
            CheckboxWithLabel(
                checked = log in enabledLogs,
                onCheckedChange = { checked -> onSetEnabledLogs(enabledLogs.plusOrMinus(log, checked)) },
                label = {
                    val name = log.name.removeSuffix("Repository")
                    val count = countsByLog[log] ?: 0
                    Text("$name [$count]")
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val DataSource.iconName: String
    get() {
        return when (this) {
            DataSource.MEMORY -> "data-table"
            DataSource.DATABASE -> "database"
            DataSource.REMOTE -> "cloud-download"
        }
    }

private object RepositoryLogEventDisplay : LogEventDisplay<Repository.LogData> {
    override fun hasContent(event: Log.Event<Repository.LogData>): Boolean {
        return super.hasContent(event) || event.data.timeInDb != null || event.data.timeInRemote != null
    }

    @Suppress("MagicNumber")
    override fun content(event: Log.Event<Repository.LogData>): String {
        return buildString {
            event.data.timeInDb?.let { appendLine("In database: ${it.formatShortDuration(decimals = 3)}") }
            event.data.timeInRemote?.let { appendLine("In remote: ${it.formatShortDuration(decimals = 3)}") }
            event.duration
                ?.let { event.data.overhead(totalDuration = it) }
                ?.let { appendLine("Overhead: ${it.formatShortDuration(decimals = 3)}") }

            event.content?.let { append(it) }
        }
    }

    @Composable
    override fun Icon(event: Log.Event<Repository.LogData>, modifier: Modifier) {
        CachedIcon(name = event.data.source.iconName, modifier = modifier, tint = event.type.color)
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
