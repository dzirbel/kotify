package com.dzirbel.kotify.ui.panel.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.capitalize
import com.dzirbel.kotify.util.collections.plusOrMinus
import com.dzirbel.kotify.util.time.formatShortDuration
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Duration

// TODO persist filter/sort/scroll even if tab is not in UI
@Composable
fun RepositoryTab() {
    val enabledLogs = remember { mutableStateOf(repositoryLogs.toSet()) }
    val selectedDataSources = remember { mutableStateOf(persistentSetOf<DataSource>()) }

    val display = object : LogEventDisplay<Repository.LogData> {
        override fun hasContent(event: Log.Event<Repository.LogData>): Boolean {
            return super.hasContent(event) || event.data.timeInDb != null || event.data.timeInRemote != null
        }

        @Suppress("MagicNumber")
        override fun content(event: Log.Event<Repository.LogData>): String {
            return buildString {
                event.data.timeInDb?.let {
                    appendLine("In database: ${it.formatShortDuration(decimals = 3)}")
                }

                event.data.timeInRemote?.let {
                    appendLine("In remote: ${it.formatShortDuration(decimals = 3)}")
                }

                val duration = event.duration
                if (duration != null && (event.data.timeInDb != null || event.data.timeInRemote != null)) {
                    val total = (event.data.timeInDb ?: Duration.ZERO) + (event.data.timeInRemote ?: Duration.ZERO)
                    appendLine("Overhead: ${(duration - total).formatShortDuration(decimals = 3)}")
                }

                event.content?.let { append(it) }
            }
        }

        @Composable
        override fun Icon(event: Log.Event<Repository.LogData>, modifier: Modifier) {
            CachedIcon(name = event.data.source.iconName, modifier = modifier, tint = event.type.color)
        }
    }

    LogList(
        logs = repositoryLogs,
        display = display,
        sortProperties = persistentListOf(
            RepositoryEventDatabaseTimeProperty,
            RepositoryEventRemoteTimeProperty,
            RepositoryEventOverheadTimeProperty,
        ),
        filter = enabledLogs.value.let { logs ->
            selectedDataSources.value.let { dataSources ->
                // do not filter if all or none are selected (no-op filter)
                val filterDataSource = dataSources.size in 1 until DataSource.entries.size
                if (filterDataSource) {
                    { it.log in logs && it.event.data.source in dataSources }
                } else {
                    { it.log in logs }
                }
            }
        },
    ) {
        // TODO add event counts to repository names and data sources
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space2), modifier = Modifier.padding(Dimens.space2)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.space1)) {
                LogListToggle(
                    logs = remember { repositories.map { it.log }.toImmutableList() },
                    enabledLogs = enabledLogs,
                    title = "Repositories",
                    modifier = Modifier.weight(1f),
                )

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.space4)) {
                    LogListToggle(
                        logs = remember { savedRepositories.map { it.log }.toImmutableList() },
                        enabledLogs = enabledLogs,
                        title = "Saved",
                    )

                    LogListToggle(
                        logs = remember { ratingRepositories.map { it.log }.toImmutableList() },
                        enabledLogs = enabledLogs,
                        title = "Rating",
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
                    Text(dataSource.name.lowercase().capitalize())
                },
            )
        }
    }
}

@Suppress("MutableParams") // allow use of MutableState - hacky, but very convenient
@Composable
private fun <T> LogListToggle(
    logs: ImmutableList<Log<T>>,
    enabledLogs: MutableState<Set<Log<T>>>,
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        val state = enabledLogs.value.toggleableState(logs)
        TriStateCheckboxWithLabel(
            state = state,
            onClick = {
                when (state) {
                    ToggleableState.On -> enabledLogs.mutate { minus(logs.toSet()) }
                    ToggleableState.Off, ToggleableState.Indeterminate -> enabledLogs.mutate { plus(logs.toSet()) }
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
                checked = log in enabledLogs.value,
                onCheckedChange = { checked -> enabledLogs.mutate { plusOrMinus(log, checked) } },
                label = { Text(log.name.removeSuffix("Repository")) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// TODO extract
private fun <T> Set<T>.toggleableState(values: Iterable<T>): ToggleableState {
    var any = false
    var all = true
    for (element in values) {
        if (element in this) {
            any = true
            if (!all) return ToggleableState.Indeterminate
        } else {
            all = false
            if (any) return ToggleableState.Indeterminate
        }
    }

    return if (all) ToggleableState.On else ToggleableState.Off
}

private val DataSource.iconName: String
    get() {
        return when (this) {
            DataSource.MEMORY -> "data-table"
            DataSource.DATABASE -> "database"
            DataSource.REMOTE -> "cloud-download"
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
            event.duration?.let { duration ->
                duration - (event.data.timeInDb ?: Duration.ZERO) + (event.data.timeInRemote ?: Duration.ZERO)
            }
        }
    }
}
