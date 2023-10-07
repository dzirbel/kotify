package com.dzirbel.kotify.ui.components.star

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.repository.rating.AverageRating
import com.dzirbel.kotify.repository.rating.Rating
import com.dzirbel.kotify.ui.LocalRatingRepository
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.instrumentation.instrument
import com.dzirbel.kotify.util.coroutines.Computation
import kotlinx.coroutines.Dispatchers

@Composable
fun AverageStarRating(
    averageRating: AverageRating?,
    maxRating: Int = Rating.DEFAULT_MAX_AVERAGE_RATING,
    starSize: Dp = Dimens.iconSmall,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.space1),
        modifier = Modifier.instrument(),
    ) {
        val stars = averageRating?.averagePercent?.let { it * maxRating }
        StarRow(
            getStarRating = { stars },
            stars = maxRating,
            starSize = starSize,
            enabled = false,
        )

        if (stars != null) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                Text(text = "%.1f (%d)".format(stars, averageRating.numRatings))
            }
        }
    }
}

@Composable
fun AverageArtistRating(
    artistId: String,
    maxRating: Int = Rating.DEFAULT_MAX_AVERAGE_RATING,
    starSize: Dp = Dimens.iconSmall,
) {
    val scope = rememberCoroutineScope { Dispatchers.Computation }
    val ratingRepository = LocalRatingRepository.current
    val averageRating = remember(artistId) {
        ratingRepository.averageRatingStateOfArtist(artistId = artistId, scope = scope)
    }
        .collectAsState()
        .value

    AverageStarRating(averageRating = averageRating, maxRating = maxRating, starSize = starSize)
}

@Composable
fun AverageAlbumRating(
    albumId: String,
    maxRating: Int = Rating.DEFAULT_MAX_AVERAGE_RATING,
    starSize: Dp = Dimens.iconSmall,
) {
    val scope = rememberCoroutineScope { Dispatchers.Computation }
    val ratingRepository = LocalRatingRepository.current
    val averageRating = remember(albumId) {
        ratingRepository.averageRatingStateOfAlbum(albumId = albumId, scope = scope)
    }
        .collectAsState()
        .value

    AverageStarRating(averageRating = averageRating, maxRating = maxRating, starSize = starSize)
}
