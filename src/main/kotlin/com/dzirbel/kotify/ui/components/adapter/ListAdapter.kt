package com.dzirbel.kotify.ui.components.adapter

import androidx.compose.runtime.Immutable
import com.dzirbel.kotify.ui.components.adapter.ListAdapter.ElementData
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import java.util.SortedMap
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
@Immutable
class ListAdapter<E> private constructor(
    /**
     * Holds the [ElementData] for each element [E] in its canonical order (i.e. the order initially provided in the
     * constructor).
     *
     * May be null to support cases in which the data has not yet been loaded, but [sorts], etc. can still be
     * configured.
     *
     * External usages should access elements only via [divisions], which includes logic for sorting, dividing, and
     * filtering.
     */
    private val elements: List<ElementData<E>>?,

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
     * This is retained as a field to properly sort newly added elements (e.g. [plusElements]).
     */
    val sorts: PersistentList<Sort<E>>?,

    /**
     * The currently applied [Divider] determining how elements are split into groupings.
     *
     * Null when no divisions should be applied.
     *
     * This is retained as a field to properly retain divisions for newly added elements (e.g. [plusElements]).
     */
    val divider: Divider<E>?,

    /**
     * The currently applied filtering predicate determining which elements should be displayed.
     *
     * Null when no filter is applied, i.e. all elements should be shown.
     *
     * This is retained as a field in order to properly filter new added elements (e.g. [plusElements]).
     */
    private val filter: ((E) -> Boolean)?,

    /**
     * An optional string associated with the [filter], typically a string which must match some property on elements.
     *
     * Retained as a field for convenience to encapsulate the state of elements for external users.
     */
    val filterString: String?,
) : Iterable<E> {
    private data class ElementData<E>(
        val element: E,

        /**
         * Whether this element should be displayed according to the currently applied filter; true if it should be
         * included, false if not.
         */
        val filtered: Boolean,

        /**
         * The division key assigned to this element by the currently applied [divider] which determines its placement
         * in [divisions], or null if there is no currently applied divider.
         */
        val division: Any?,
    )

    /**
     * Whether this adapter has initialized data; if not [divisions] will be empty and a loading state could be
     * displayed.
     */
    val hasElements: Boolean = elements != null

    /**
     * The number of elements (including filtered elements) in the adapter, or zero if the adapter has not yet been
     * initialized with elements.
     */
    val size: Int = elements?.size ?: 0

    /**
     * Lazily computes the external view of elements in this [ListAdapter], as a map from division key to the sorted,
     * filtered elements in that division.
     *
     * If there are no divisions applied, the returned map will contain a single null key with all the elements.
     * Otherwise the map will be a [SortedMap] sorted according to the currently applied [divider]'s ordering of
     * divisions, so the map's iteration order should be used. Note that if the adapter has no data (i.e. [hasElements]
     * is false), a map with no elements will be returned (but it may still have a single, empty division).
     *
     * While sorting is pre-computed every time a new sort order is applied, the division arrangement is recalculated
     * for each new [ListAdapter] on the first call to [divisions]. This is (as far as I can tell) necessary at least
     * in cases when either the divider or sort order are changed, but perhaps could be avoided for changes to just the
     * filter, etc.
     */
    @Suppress("UnsafeCallOnNullableType")
    val divisions: SortedMap<Any?, out List<IndexedValue<E>>> by lazy {
        val indices = sortIndexes ?: elements?.indices ?: emptyList()
        if (divider == null) {
            sortedMapOf(
                { _, _ -> 0 }, // no-op comparator since we have a single elements (of type Nothing?)
                null to indices.mapNotNull { index ->
                    elements!![index]
                        .takeIf { it.filtered }
                        ?.element
                        ?.let { IndexedValue(index, it) }
                },
            )
        } else {
            val map = TreeMap<Any?, MutableList<IndexedValue<E>>>(divider.divisionComparator)

            indices.forEach { index ->
                val elementData = elements!![index]
                if (elementData.filtered) {
                    val indexedValue = IndexedValue(index, elementData.element)
                    map.compute(elementData.division) { _, list ->
                        list?.apply { add(indexedValue) } ?: mutableListOf(indexedValue)
                    }
                }
            }

            map
        }
    }

    /**
     * Returns the element at the given [index] in the canonical order.
     */
    operator fun get(index: Int): E? = elements?.get(index)?.element

    /**
     * Returns the division for the element at the given [index] in the canonical order.
     */
    fun divisionOf(index: Int): Any? = elements?.get(index)?.division

    /**
     * Retrieves the currently applied [SortOrder] associated with the given [sortableProperty] in this adapter's
     * [sorts].
     *
     * Note that [SortableProperty.sortTitle] is used to determine equality of [SortableProperty]s, so the same
     * [SortableProperty] object as is present in [sorts] does not need to be used.
     */
    fun sortOrderFor(sortableProperty: SortableProperty<E>?): SortOrder? {
        return sortableProperty?.let { _ ->
            sorts?.find { it.sortableProperty.sortTitle == sortableProperty.sortTitle }?.sortOrder
        }
    }

    override fun iterator(): Iterator<E> {
        return object : Iterator<E> {
            private var index: Int = 0

            override fun hasNext(): Boolean = index < (elements?.size ?: 0)

            override fun next(): E {
                return elements?.getOrNull(index++)?.element
                    ?: throw NoSuchElementException("")
            }
        }
    }

    /**
     * Returns a copy of this [ListAdapter] with the given [filter] applied, i.e. only elements which satisfy [filter]
     * will be included in its [divisions].
     */
    fun withFilter(filterString: String? = null, filter: ((element: E) -> Boolean)?): ListAdapter<E> {
        return ListAdapter(
            elements = elements?.map {
                it.copy(filtered = filter?.invoke(it.element) != false)
            },
            sortIndexes = sortIndexes,
            sorts = sorts,
            divider = divider,
            filter = filter,
            filterString = filterString,
        )
    }

    /**
     * Returns a copy of this [ListAdapter], filtering only elements whose [elementProperty] contains [filterString],
     * optionally ignoring case.
     */
    fun withFilterByString(
        filterString: String?,
        ignoreCase: Boolean = true,
        elementProperty: (E) -> String,
    ): ListAdapter<E> {
        return withFilter(
            filterString = filterString,
            filter = filterString?.let {
                { element ->
                    elementProperty(element).contains(filterString, ignoreCase = ignoreCase)
                }
            },
        )
    }

    /**
     * Returns a copy of this [ListAdapter] with the given [divider] applied, i.e. elements will be grouped according to
     * [DividableProperty.divisionFor] in its [divisions], or undivided if [divider] is null.
     */
    fun withDivider(divider: Divider<E>?): ListAdapter<E> {
        return ListAdapter(
            elements = elements?.map {
                it.copy(division = divider?.dividableProperty?.divisionFor(it.element))
            },
            sortIndexes = sortIndexes,
            sorts = sorts,
            divider = divider,
            filter = filter,
            filterString = filterString,
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
    fun withSort(sorts: PersistentList<Sort<E>>?): ListAdapter<E> {
        if (sorts == this.sorts) return this

        return ListAdapter(
            elements = elements,
            sortIndexes = sorts?.let { sortIndexes(sorts = sorts, elements = elements) },
            sorts = sorts,
            divider = divider,
            filter = filter,
            filterString = filterString,
        )
    }

    /**
     * Returns a copy of this [ListAdapter] with the given [newElements] added at the end of the canonical order. This
     * is more efficient than building an entire new [ListAdapter] with the concatenated element list since it retains
     * divisions and filtering of previous elements (but recomputes sorting from scratch).
     */
    fun plusElements(newElements: List<E>): ListAdapter<E> {
        val newElementData = newElements.map { element ->
            ElementData(
                element = element,
                filtered = filter?.invoke(element) != false,
                division = divider?.dividableProperty?.divisionFor(element),
            )
        }

        val elementData = this.elements.orEmpty().plus(newElementData)

        return ListAdapter(
            elements = elementData,
            sortIndexes = sorts?.let { sortIndexes(sorts = sorts, elements = elementData) },
            sorts = sorts,
            divider = divider,
            filter = filter,
            filterString = filterString,
        )
    }

    /**
     * Returns a copy of this [ListAdapter] with the given [elements] as its data, retaining sorts, filters, etc.
     */
    fun withElements(elements: List<E>?): ListAdapter<E> {
        val elementData = elements?.map { element ->
            ElementData(
                element = element,
                filtered = this.filter?.invoke(element) != false,
                division = this.divider?.dividableProperty?.divisionFor(element),
            )
        }

        return ListAdapter(
            elements = elementData,
            sortIndexes = sorts?.let { sortIndexes(sorts = sorts, elements = elementData) },
            sorts = sorts,
            divider = divider,
            filter = filter,
            filterString = filterString,
        )
    }

    private fun sortIndexes(sorts: List<Sort<E>>, elements: List<ElementData<E>>?): List<Int>? {
        if (elements == null) return null
        if (elements.size <= 1) return elements.indices.toList()

        // retain current sort order as the baseline unless the number of elements has changed
        val baseIndices = sortIndexes?.takeIf { it.size == elements.size }

        val inputArray = Array(elements.size) { i ->
            val index = baseIndices?.get(i) ?: i
            IndexedValue(index, elements[index].element)
        }

        val sortComparator = sorts.asComparator()
        val indexedComparator = Comparator<IndexedValue<E>> { o1, o2 ->
            sortComparator.compare(o1.value, o2.value)
        }
        inputArray.sortWith(indexedComparator)

        return inputArray.map { it.index }
    }

    companion object {
        /**
         * Creates a new [ListAdapter] with uninitialized elements, optionally applying the given [defaultSort].
         *
         * This is useful to create a [ListAdapter] without any data, but which can be used to store sorts, dividers,
         * etc. before elements are loaded.
         */
        fun <E> empty(
            defaultSort: SortableProperty<E>? = null,
            defaultFilter: ((E) -> Boolean)? = null,
        ): ListAdapter<E> {
            return ListAdapter(
                elements = null,
                sortIndexes = null,
                sorts = defaultSort?.let { persistentListOf(Sort(it)) },
                divider = null,
                filter = defaultFilter,
                filterString = null,
            )
        }

        /**
         * Creates a new [ListAdapter] from the given [elements].
         */
        fun <E> of(elements: List<E>?): ListAdapter<E> {
            return ListAdapter(
                elements = elements?.map { element ->
                    ElementData(element = element, filtered = true, division = null)
                },
                sortIndexes = null,
                sorts = null,
                divider = null,
                filter = null,
                filterString = null,
            )
        }
    }
}
