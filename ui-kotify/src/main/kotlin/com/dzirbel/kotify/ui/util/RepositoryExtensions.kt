package com.dzirbel.kotify.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.dzirbel.kotify.repository2.SavedRepository
import com.dzirbel.kotify.repository2.rating.RatingRepository

/**
 * Remembers the [SavedRepository.savedStatesOf] for the given [entities] with the given ID extractor function [toId] at
 * this point in the composition.
 *
 * This ensures that only a single batched call is made to retrieve the saved states, and subsequent calls to individual
 * states will reuse this batched call. In particular, the call is made synchronously via [remember] rather than an
 * async [LaunchedEffect] in order to guarantee it is made before any individual calls below it in the composition.
 */
@Composable
fun <T> SavedRepository.rememberSavedStates(entities: Iterable<T>, toId: (T) -> String) {
    remember(entities) {
        savedStatesOf(ids = entities.map(toId))
    }
}

/**
 * Remembers the [RatingRepository.ratingStatesOf] for the given [entities] with the given ID extractor function [toId]
 * at this point in the composition.
 *
 * This ensures that only a single batched call is made to retrieve the rating states, and subsequent calls to
 * individual states will reuse this batched call. In particular, the call is made synchronously via [remember] rather
 * than an async [LaunchedEffect] in order to guarantee it is made before any individual calls below it in the
 * composition.
 */
@Composable
fun <T> RatingRepository.rememberRatingStates(entities: Iterable<T>, toId: (T) -> String) {
    remember(entities) {
        ratingStatesOf(ids = entities.map(toId))
    }
}
