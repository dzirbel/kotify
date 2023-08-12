package com.dzirbel.kotify.ui.util

import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import com.dzirbel.kotify.util.coroutines.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.scan

/**
 * Returns a Flow of the index of the first [InteractionSource] in this list that is hovered, or -1 if none are hovered.
 */
fun List<InteractionSource>.hoveredIndex(): Flow<Int> {
    return this
        // map each InteractionSource to a Flow<Boolean> of whether it is hovered
        .map { interactionSource ->
            // note: library implementations keep a list of references to the specific HoverInteractions and add/remove
            // from that list, but a simple counter seems sufficient
            interactionSource.interactions
                .runningFold(initial = 0) { total, interaction ->
                    when (interaction) {
                        is HoverInteraction.Enter -> total + 1
                        is HoverInteraction.Exit -> total - 1
                        else -> total
                    }
                }
                .map { it > 0 }
        }
        // find the index of the first one which is hovered
        .combine { hoveredStates ->
            hoveredStates.indexOfFirst { it }
        }
        // scan to keep the last hovered index if none are hovered
        .scan(initial = -1) { previous, current ->
            if (current != -1) current else previous
        }
}
