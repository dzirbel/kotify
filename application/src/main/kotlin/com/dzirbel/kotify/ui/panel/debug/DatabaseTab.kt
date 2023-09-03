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
import com.dzirbel.kotify.DatabaseLogger
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.ui.components.AppliedTextField
import com.dzirbel.kotify.ui.components.CheckboxWithLabel
import com.dzirbel.kotify.ui.theme.Dimens

@Composable
fun DatabaseTab() {
    val groupByTransaction = remember { mutableStateOf(true) }
    LogList(log = if (groupByTransaction.value) DatabaseLogger.transactionLog else DatabaseLogger.statementLog) {
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
        }
    }
}
