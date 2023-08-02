package com.dzirbel.kotify.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.dzirbel.kotify.repository.SavedRepository
import com.dzirbel.kotify.repository.artist.ArtistTracksRepository
import com.dzirbel.kotify.repository.rating.RatingRepository

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
        @Suppress("LabeledExpression") // use labeled expression to ensure value is returned (and thus remembered)
        return@remember if (entities.any()) {
            savedStatesOf(ids = entities.map(toId))
        } else {
            null
        }
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
        @Suppress("LabeledExpression") // use labeled expression to ensure value is returned (and thus remembered)
        return@remember if (entities.any()) {
            ratingStatesOf(ids = entities.map(toId))
        } else {
            null
        }
    }
}

/**
 * Remembers the [ArtistTracksRepository.artistTracksStatesOf] for the given [artistIds] at this point in the
 * composition.
 *
 * This ensures that only a single batched call is made to retrieve the artist tracks states, and subsequent calls to
 * individual states will reuse this batched call. In particular, the call is made synchronously via [remember] rather
 * than an async [LaunchedEffect] in order to guarantee it is made before any individual calls below it in the
 * composition.
 */
@Composable
fun ArtistTracksRepository.rememberArtistTracksStates(artistIds: Iterable<String>?) {
    remember(artistIds) {
        @Suppress("LabeledExpression") // use labeled expression to ensure value is returned (and thus remembered)
        return@remember if (artistIds?.any() == true) {
            artistTracksStatesOf(artistIds = artistIds)
        } else {
            null
        }
    }
}
