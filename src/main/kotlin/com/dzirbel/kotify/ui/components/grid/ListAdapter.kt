package com.dzirbel.kotify.ui.components.grid

import com.dzirbel.kotify.ui.components.grid.ListAdapter.ElementData
import com.dzirbel.kotify.ui.components.sort.Sort
import com.dzirbel.kotify.ui.components.sort.asComparator
import java.util.TreeMap

/**
 * Wraps a list of [elements] of type [E] in an adapter which controls common UI patterns of sorting (ordering of
 * elements), filtering (inclusion/exclusion of some elements), and dividing (grouping of elements in categories).
 *
 * This class mainly exists to ensure that these operations are handled efficiently, especially when combined. For
 * example, if changing the filter it should not be necessary to recompute the sorts or divisions. It does this by
 * attaching [ElementData] to each element to hold its filter/division state and keeping a [sortIndexes] map for the
 * sort order. Nevertheless, the divisions must be (partially) recomputed in [divisions] for each modified adapter.
 */
class ListAdapter<E> private constructor(
    /**
     * Holds the [ElementData] for each element [E] in its canonical order (i.e. the order initially provided in the
     * constructor).
     *
     * External usages should access elements only via [divisions], which includes logic for sorting, dividing, and
     * filtering.
     */
    private val elements: List<ElementData<E>>,

    /**
     * Maps element indexes from the current applied [sorts], i.e. element i in [sortIndexes] is the index of in
     * elements which should be placed at index i when sorted.
     *
     * Null when [sorts] is null, i.e. the canonical order should be used.
     */
    private val sortIndexes: List<Int>?,

    /**
     * The currently applied [Sort]s determining the order of elements.
     *
     * Null when the elements should retain their canonical order.
     *
     * Keeping this as a field isn't necessary for the operation of [ListAdapter] but is convenient to encapsulate the
     * state of the elements for external users.
     */
    val sorts: List<Sort<E>>?,

    /**
     * The currently applied [GridDivider] determining how elements are grouped into [divisions].
     *
     * Null when the elements should not have any divisions, i.e. a single null key in [divisions].
     *
     * Like [sorts], keeping this as a field is convenient to encapsulate the state of elements for external users, but
     * it also is required to determine the [GridDivider.divisionComparator] of [divisions].
     */
    val divider: GridDivider<E>?,
) {
    private data class ElementData<E>(
        val element: E,

        /**
         * Whether this element should be displayed according to the currently applied filter; true if it should be
         * included, false if not.
         */
        val filtered: Boolean,

        /**
         * The division key assigned to this element by the currently applied [GridDivider] which determines its
         * placement in [divisions], or null if there is no currently applied divider.
         */
        val division: String?,
    )

    /**
     * Lazily computes the external view of elements in this [ListAdapter], as a map from division key to the sorted,
     * filtered elements in that division.
     *
     * If there are no divisions applied, the returned map will contain a single null key with all the elements.
     * Otherwise the map will be a [java.util.SortedMap] sorted according to the currently applied [GridDivider]'s
     * ordering of divisions, so the map's iteration order should be used.
     *
     * While sorting is pre-computed every time a new sort order is applied, the division arrangement is recalculated
     * for each new [ListAdapter] on the first call to [divisions]. This is (as far as I can tell) necessary at least
     * in cases when either the divider or sort order are changed, but perhaps could be avoided for changes to just the
     * filter, etc.
     */
    val divisions: Map<out String?, List<E>> by lazy {
        val indices = sortIndexes ?: elements.indices
        if (divider == null) {
            mutableMapOf(
                null to indices.mapNotNull { index ->
                    elements[index].takeIf { it.filtered }?.element
                }
            )
        } else {
            val map = TreeMap<String, MutableList<E>>(divider.divisionComparator)

            indices.forEach { index ->
                val elementData = elements[index]
                if (elementData.filtered) {
                    val division = requireNotNull(elementData.division) { "null division with divider" }
                    val element = elementData.element
                    map.compute(division) { _, list ->
                        list?.apply { add(element) } ?: mutableListOf(element)
                    }
                }
            }

            map
        }
    }

    // TODO accept sorts, divider, filter as well?
    constructor(elements: List<E>) : this(
        elements = elements.map { element ->
            ElementData(element = element, filtered = true, division = null)
        },
        sortIndexes = null,
        sorts = null,
        divider = null,
    )

    /**
     * Returns a copy of this [ListAdapter] with the given [filter] applied, i.e. only elements which satisfy [filter]
     * will be included in its [divisions].
     */
    fun withFilter(filter: (element: E) -> Boolean): ListAdapter<E> {
        return ListAdapter(
            elements = elements.map {
                it.copy(filtered = filter(it.element))
            },
            sortIndexes = sortIndexes,
            sorts = sorts,
            divider = divider,
        )
    }

    /**
     * Returns a copy of this [ListAdapter] with the given [divider] applied, i.e. elements will be grouped according
     * to [divisionFor] in its [divisions], or undivided if [divider] is null.
     *
     * TODO merge divisionFor into the divider itself
     */
    fun withDivisions(divisionFor: ((element: E) -> String)?, divider: GridDivider<E>?): ListAdapter<E> {
        return ListAdapter(
            elements = elements.map {
                it.copy(division = divisionFor?.invoke(it.element))
            },
            sortIndexes = sortIndexes,
            sorts = sorts,
            divider = divider,
        )
    }

    /**
     * Returns a copy of this [ListAdapter] with the given [sorts] used to determine its sort order, i.e. elements in
     * each of its [divisions] will be ordered by [sorts], or sorted according to the canonical order if [sorts] is
     * null.
     *
     * Note that the new sort order is stable relative to the previous sort order (not the canonical order), preserving
     * order of equal elements according to the last sort order when a new one is applied.
     */
    fun withSort(sorts: List<Sort<E>>?): ListAdapter<E> {
        val sortIndexes = sorts?.let {
            val sortComparator = sorts.asComparator()
            val mappedComparator = Comparator<IndexedValue<ElementData<E>>> { o1, o2 ->
                // TODO wasteful creation of IndexedValue objects
                sortComparator.compare(
                    IndexedValue(o1.index, o1.value.element),
                    IndexedValue(o2.index, o2.value.element),
                )
            }

            // order by existing sortIndexes to preserve existing order under stable sorting
            // TODO avoid creating extra lists in double mapping?
            (sortIndexes ?: elements.indices)
                .map { IndexedValue(it, elements[it]) }
                .sortedWith(mappedComparator)
                .map { it.index }
        }

        return ListAdapter(
            elements = elements,
            sortIndexes = sortIndexes,
            sorts = sorts,
            divider = divider,
        )
    }
}
