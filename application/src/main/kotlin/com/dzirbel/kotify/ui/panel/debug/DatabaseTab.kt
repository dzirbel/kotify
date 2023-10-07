package com.dzirbel.kotify.ui.panel.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.DatabaseLogger
import com.dzirbel.kotify.db.DB
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.AppliedTextField
import com.dzirbel.kotify.ui.components.CheckboxWithLabel
import com.dzirbel.kotify.ui.components.ToggleButtonGroup
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.util.coroutines.Computation
import com.dzirbel.kotify.util.coroutines.lockedState
import com.dzirbel.kotify.util.takingIf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.runningFold

@Composable
fun DatabaseTab() {
    val groupByTransaction = remember { mutableStateOf(true) }

    val log = if (groupByTransaction.value) DatabaseLogger.transactionLog else DatabaseLogger.statementLog

    val selectedDBs = remember { mutableStateOf(persistentSetOf<DB>()) }

    // do not filter if all or none are selected (no-op filter)
    val filterDBs = selectedDBs.value.size in 1 until DB.entries.size

    LogList(
        log = log,
        filter = takingIf(filterDBs) {
            @Suppress("Wrapping") // ktlint false positive; fixed by https://github.com/pinterest/ktlint/pull/2127
            { it.event.data.db in selectedDBs.value }
        },
        filterKey = selectedDBs.value,
        canResetFilter = selectedDBs.value.isNotEmpty(),
        onResetFilter = { selectedDBs.value = persistentSetOf() },
    ) { eventCleared ->
        Column(Modifier.padding(Dimens.space3), verticalArrangement = Arrangement.spacedBy(Dimens.space2)) {
            AppliedTextField(
                value = KotifyDatabase.transactionDelayMs.toString(),
                label = "Transaction delay (ms)",
                modifier = Modifier.fillMaxWidth(),
                applyValue = { value ->
                    val valueLong = value.toLongOrNull()
                    valueLong?.let { KotifyDatabase.transactionDelayMs = it }
                    valueLong != null
                },
            )

            CheckboxWithLabel(
                modifier = Modifier.fillMaxWidth(),
                checked = groupByTransaction.value,
                onCheckedChange = { groupByTransaction.value = it },
                label = { Text("Group by transaction") },
            )

            ToggleButtonGroup(
                elements = DB.entries.toImmutableList(),
                selectedElements = selectedDBs.value,
                onSelectElements = { selectedDBs.value = it },
                content = { db ->
                    CachedIcon(name = "database", size = Dimens.iconSmall)

                    Spacer(Modifier.width(Dimens.space2))

                    val scope = rememberCoroutineScope { Dispatchers.Computation }
                    val count: Int? = remember(groupByTransaction.value, eventCleared) {
                        log.writeLock.lockedState(
                            scope = scope,
                            initializeWithLock = {
                                log.events.count { event ->
                                    !eventCleared(event) && event.data.db == db
                                }
                            },
                        ) { initial ->
                            log.eventsFlow
                                .runningFold(initial) { count, event ->
                                    if (!eventCleared(event) && event.data.db == db) count + 1 else count
                                }
                        }
                    }
                        .collectAsState()
                        .value

                    Text("${db.databaseName} [$count]", maxLines = 1)
                },
            )
        }
    }
}
