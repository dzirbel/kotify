package com.dzirbel.kotify.ui.panel.debug

import androidx.compose.foundation.ScrollState
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
import com.dzirbel.kotify.log.FlowView
import com.dzirbel.kotify.log.Log
import com.dzirbel.kotify.repository.ratingRepositories
import com.dzirbel.kotify.repository.repositories
import com.dzirbel.kotify.repository.repositoryLogs
import com.dzirbel.kotify.repository.savedRepositories
import com.dzirbel.kotify.ui.components.CheckboxWithLabel
import com.dzirbel.kotify.ui.components.HorizontalDivider
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.CurrentTime
import com.dzirbel.kotify.util.collections.plusOrMinus

// TODO add sort (by time, duration, etc) and filter?
@Composable
fun RepositoryTab(scrollState: ScrollState) {
    Column {
        // TODO persist filter (and sort?) even if tab is not in UI
        val view = remember {
            mutableStateOf(FlowView<Log.Event>(sort = Comparator.comparing { it.time }))
        }

        val enabledLogs = remember { mutableStateOf(repositoryLogs.toSet()) }

        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.space2),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space1),
        ) {
            Column {
                for (repository in repositories) {
                    LogToggle(repository.log, enabledLogs)
                }
            }

            Column {
                for (savedRepository in savedRepositories) {
                    LogToggle(savedRepository.log, enabledLogs)
                }
            }

            Column {
                for (ratingRepository in ratingRepositories) {
                    LogToggle(ratingRepository.log, enabledLogs)
                }
            }
        }

        LogList(logs = enabledLogs.value, view = view.value, modifier = Modifier.weight(1f), scrollState = scrollState)

        HorizontalDivider()

        SimpleTextButton(
            onClick = {
                val minTime = CurrentTime.millis
                view.mutate { copy(filter = { it.time > minTime }) }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Clear log")
        }
    }
}

@Suppress("MutableParams") // allow use of MutableState - hacky, but very convenient
@Composable
private fun LogToggle(log: Log<Log.Event>, enabledLogs: MutableState<Set<Log<Log.Event>>>) {
    CheckboxWithLabel(
        checked = log in enabledLogs.value,
        onCheckedChange = { checked -> enabledLogs.mutate { plusOrMinus(log, checked) } },
        label = { Text(log.name) },
    )
}
