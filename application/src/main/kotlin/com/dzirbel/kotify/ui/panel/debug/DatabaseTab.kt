package com.dzirbel.kotify.ui.panel.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.DatabaseLogger
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.ui.components.AppliedTextField
import com.dzirbel.kotify.ui.components.CheckboxWithLabel
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.mutate

private data class DatabaseSettings(
    val groupByTransaction: Boolean = true,
)

private val databaseSettings = mutableStateOf(DatabaseSettings())

@Composable
fun DatabaseTab() {
    val groupByTransaction = databaseSettings.value.groupByTransaction
    LogList(log = if (groupByTransaction) DatabaseLogger.transactionLog else DatabaseLogger.statementLog) {
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
                checked = groupByTransaction,
                onCheckedChange = { databaseSettings.mutate { copy(groupByTransaction = it) } },
                label = { Text("Group by transaction") },
            )
        }
    }
}
