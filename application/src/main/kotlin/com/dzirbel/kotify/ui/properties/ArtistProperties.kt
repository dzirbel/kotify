package com.dzirbel.kotify.ui.properties

import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.repository2.rating.AverageRating
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.properties.PropertyByNumber
import com.dzirbel.kotify.ui.components.adapter.properties.PropertyByString
import kotlinx.coroutines.flow.StateFlow

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

class ArtistRatingProperty(ratings: Map<String, StateFlow<AverageRating>>) : PropertyByAverageRating<Artist>(ratings) {
    override fun idOf(element: Artist) = element.id.value
}
