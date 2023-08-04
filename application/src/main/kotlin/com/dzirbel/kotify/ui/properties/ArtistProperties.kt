package com.dzirbel.kotify.ui.properties

import com.dzirbel.kotify.repository.artist.ArtistViewModel
import com.dzirbel.kotify.repository.rating.AverageRating
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.properties.PropertyByNumber
import com.dzirbel.kotify.ui.components.adapter.properties.PropertyByString
import kotlinx.coroutines.flow.StateFlow

object ArtistNameProperty : PropertyByString<ArtistViewModel>(title = "Name") {
    override fun toString(item: ArtistViewModel) = item.name
}

object ArtistPopularityProperty : PropertyByNumber<ArtistViewModel>(
    title = "Popularity",
    divisionRange = POPULARITY_DIVISION_RANGE,
) {
    override val defaultSortOrder = SortOrder.DESCENDING
    override val defaultDivisionSortOrder = SortOrder.DESCENDING

    override fun toNumber(item: ArtistViewModel): Int? = item.popularity
}

class ArtistRatingProperty(ratings: Map<String, StateFlow<AverageRating?>>?) :
    PropertyByAverageRating<ArtistViewModel>(ratings) {

    override fun idOf(element: ArtistViewModel) = element.id
}
