package com.dzirbel.kotify.repository2.util

object ReorderCalculator {
    /**
     * A single operation which moves the segment of a list from [rangeStart] with [rangeLength] items to be inserted
     * before the index [insertBefore].
     */
    data class ReorderOperation(val rangeStart: Int, val rangeLength: Int, val insertBefore: Int) {
        init {
            require(rangeLength > 0)
            require(rangeStart >= 0)
            require(insertBefore >= 0)
        }

        /**
         * Convenience constructor which wraps the common case of a single item moving from [fromIndex] to [toIndex].
         */
        constructor(fromIndex: Int, toIndex: Int) : this(
            rangeStart = fromIndex,
            rangeLength = 1,
            insertBefore = toIndex,
        )
    }

    /**
     * Calculates a sequence of [ReorderOperation]s which will sort [list] according to [comparator] when applied
     * in-order.
     *
     * This currently uses a simple algorithm which moves items into place one-by-one, starting with the item that
     * should end in index 0, then index 1, etc. For example, if reordering [d, b, c, a] alphabetically:
     * - first move a to index 0, i.e. index 3 -> 0
     * - then move b to index 1, i.e. index 2 -> 1. Note that since a has been moved to the start b is no longer at
     *   index 1 but now at index 2
     * - then move c to index 2, i.e. index 3 -> 2. Again c is now at the end of the list since a was moved to the start
     * - finally move d to index 3, i.e. index 3 -> 3. The returned list will omit this operation since it is a no-op
     *
     * TODO optimize to reduce number of operations when possible, i.e. merge neighboring moves that can be done in a
     *  single operation
     */
    fun <T> calculateReorderOperations(list: List<T>, comparator: Comparator<T>): List<ReorderOperation> {
        if (list.size <= 1) return emptyList()

        val indexedComparator = Comparator<IndexedValue<T>> { o1, o2 -> comparator.compare(o1.value, o2.value) }
        val result = mutableListOf<ReorderOperation>()
        val removedIndices = mutableSetOf<Int>()
        for (indexedValue in list.withIndex().sortedWith(indexedComparator).withIndex()) {
            val toIndex = indexedValue.index
            val fromIndex = indexedValue.value.index

            val adjusted = fromIndex + removedIndices.count { it > fromIndex }
            if (adjusted != toIndex) {
                result.add(ReorderOperation(fromIndex = adjusted, toIndex = toIndex))
            }

            removedIndices.add(fromIndex)
        }

        return result
    }
}
