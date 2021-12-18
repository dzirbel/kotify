package com.dzirbel.kotify.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.cache.SpotifyCache
import com.dzirbel.kotify.ui.components.StarRating

private const val DEFAULT_MAX_RATING = 10

/**
 * Star rating component for a track with the given [trackId].
 */
@Composable
fun TrackStarRating(trackId: String?, modifier: Modifier = Modifier) {
    val state = remember(trackId) {
        trackId?.let { SpotifyCache.Ratings.ratingState(trackId = trackId) }
    }

    val rating = state?.value?.obj as? SpotifyCache.GlobalObjects.TrackRating
    val maxRating = rating?.maxRating ?: DEFAULT_MAX_RATING

    StarRating(
        rating = rating?.rating,
        stars = maxRating,
        modifier = modifier,
        ratedTimestamp = state?.value?.cacheTime,
        enabled = trackId != null,
        onRate = { star ->
            trackId?.let {
                SpotifyCache.Ratings.setRating(
                    trackId = trackId,
                    rating = SpotifyCache.GlobalObjects.TrackRating(
                        trackId = trackId,
                        rating = star,
                        maxRating = maxRating,
                    ),
                )
            }
        },
        onClearRating = if (trackId != null && state?.value != null) {
            { SpotifyCache.Ratings.clearRating(trackId = trackId) }
        } else {
            null
        },
    )
}
