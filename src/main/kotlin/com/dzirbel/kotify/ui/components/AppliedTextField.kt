package com.dzirbel.kotify.ui.components

import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged

/**
 * A wrapper around [OutlinedTextField] which calls [applyValue] whenever a new value should be used, and marks the text
 * field as an error state if it returns false.
 */
@Composable
fun AppliedTextField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    applyValue: (String) -> Boolean,
) {
    val textFieldValue = remember(value) { mutableStateOf(value) }
    val appliedValue = remember { mutableStateOf(true) }

    OutlinedTextField(
        modifier = modifier
            .onFocusChanged { focusState ->
                if (!focusState.hasFocus) {
                    appliedValue.value = applyValue(textFieldValue.value)
                }
            },
        value = textFieldValue.value,
        singleLine = true,
        isError = !appliedValue.value,
        onValueChange = { newValue ->
            textFieldValue.value = newValue
            appliedValue.value = applyValue(newValue)
        },
        label = {
            Text(label, style = MaterialTheme.typography.overline)
        },
    )
}
