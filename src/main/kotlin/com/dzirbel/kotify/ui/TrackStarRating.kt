package com.dzirbel.kotify.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.cache.SpotifyCache
import com.dzirbel.kotify.ui.components.StarRating

private const val STARS = 5

/**
 * Star rating component for a track with the given [trackId].
 */
@Composable
fun TrackStarRating(trackId: String?, modifier: Modifier = Modifier) {
    val state = trackId?.let { SpotifyCache.Ratings.ratingState(trackId = trackId) }

    StarRating(
        rating = state?.value?.obj as? Int,
        stars = STARS,
        modifier = modifier,
        ratedTimestamp = state?.value?.cacheTime,
        enabled = trackId != null,
        onRate = { star ->
            trackId?.let { SpotifyCache.Ratings.setRating(trackId = trackId, rating = star) }
        },
        onClearRating = if (trackId != null && state?.value != null) {
            { SpotifyCache.Ratings.clearRating(trackId = trackId) }
        } else {
            null
        },
    )
}
