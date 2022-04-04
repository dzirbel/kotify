package com.dzirbel.kotify.ui.properties

import androidx.compose.runtime.State
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.ui.components.adapter.SortOrder

object ArtistNameProperty : PropertyByString<Artist>(title = "Name") {
    override fun toString(item: Artist) = item.name
}

object ArtistPopularityProperty : PropertyByNumber<Artist>(
    title = "Popularity",
    divisionRange = POPULARITY_DIVISION_RANGE,
) {
    override val defaultSortOrder = SortOrder.DESCENDING
    override val defaultDivisionSortOrder = SortOrder.DESCENDING

    override fun toNumber(item: Artist): Int? = item.popularity
}

class ArtistRatingProperty(ratings: Map<String, List<State<Rating?>>?>) : PropertyByAverageRating<Artist>(ratings) {
    override fun idOf(element: Artist) = element.id.value
}
