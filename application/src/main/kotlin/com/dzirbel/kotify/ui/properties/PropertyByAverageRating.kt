package com.dzirbel.kotify.ui.properties

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.dzirbel.kotify.repository2.rating.AverageRating
import com.dzirbel.kotify.repository2.rating.Rating
import com.dzirbel.kotify.ui.components.adapter.DividableProperty
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.SortableProperty
import com.dzirbel.kotify.ui.components.adapter.compareNullable
import com.dzirbel.kotify.ui.components.star.AverageStarRating
import com.dzirbel.kotify.ui.components.table.Column
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import kotlin.math.floor

interface RatingDividableProperty<E> : DividableProperty<E> {
    fun ratingOf(element: E): Double?

    override fun compareDivisions(sortOrder: SortOrder, first: Any?, second: Any?): Int {
        return sortOrder.compareNullable(first as? Double, second as? Double)
    }

    override fun divisionFor(element: E): Double? {
        return ratingOf(element)?.let { rating ->
            floor(rating * DIVIDING_FRACTION) / DIVIDING_FRACTION
        }
    }

    override fun divisionTitle(division: Any?): String {
        return (division as? Double)?.let { String.format(Locale.getDefault(), "%.1f", it) }
            ?: "Unrated"
    }

    companion object {
        /**
         * Determines what ranges ratings are grouped into, inverted. E.g. with a fraction of 4 ranges will be
         * 0.0 - 0.25, 0.25 - 0.5, 0.5 - 0.75, 0.75 - 1.0, etc. Note that the rounding of the header strings may make
         * some groupings unclear.
         */
        private const val DIVIDING_FRACTION = 2
    }
}

abstract class PropertyByAverageRating<E>(
    private val ratings: Map<String, StateFlow<AverageRating>>,
    private val maxRating: Int = Rating.DEFAULT_MAX_AVERAGE_RATING,
    override val title: String = "Rating",
) : SortableProperty<E>, RatingDividableProperty<E>, Column<E> {
    override val defaultDivisionSortOrder = SortOrder.DESCENDING
    override val defaultSortOrder = SortOrder.DESCENDING
    override val terminalSort = true

    abstract fun idOf(element: E): String

    override fun compare(sortOrder: SortOrder, first: E, second: E): Int {
        return sortOrder.compareNullable(ratingOf(first), ratingOf(second))
    }

    @Composable
    override fun Item(item: E) {
        val averageRating = ratings[idOf(item)]?.collectAsState()?.value
        AverageStarRating(averageRating = averageRating, maxRating = maxRating)
    }

    override fun ratingOf(element: E): Double? {
        return ratings[idOf(element)]?.value?.averagePercent?.let { it * maxRating }
    }
}
