package com.dzirbel.kotify.repository2.util

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.random.Random

internal class ReorderCalculatorTest {
    data class ApplyToCase(
        val input: List<Char> = alphaList,
        val operation: ReorderCalculator.ReorderOperation,
        val expected: List<Char>,
    )

    data class OrderCase(
        val input: List<Char>,
        val expectedOperationsCount: Int,
        val comparator: Comparator<Char> = Comparator { o1, o2 -> o1.compareTo(o2) },
    )

    @ParameterizedTest
    @MethodSource
    fun testApplyTo(case: ApplyToCase) {
        assertThat(case.operation.applyTo(case.input)).isEqualTo(case.expected)
    }

    @ParameterizedTest
    @MethodSource
    fun testOrder(case: OrderCase) {
        val changes = ReorderCalculator.calculateReorderOperations(case.input, case.comparator)
        assertThat(changes).hasSize(case.expectedOperationsCount)
        assertThat(changes.fold(case.input) { acc, orderChange -> orderChange.applyTo(acc) })
            .isEqualTo(case.input.sortedWith(case.comparator))
    }

    /**
     * Applies this [ReorderCalculator.ReorderOperation] to the given [list] and returns the result. This is very
     * helpful for simulating the result of [ReorderCalculator.calculateReorderOperations] but the logic is non-trivial
     * so it is also itself tested in this class.
     */
    private fun <T> ReorderCalculator.ReorderOperation.applyTo(list: List<T>): List<T> {
        val rangeEnd = rangeStart + rangeLength

        require(rangeLength > 0)
        require(rangeStart in list.indices)
        require(rangeEnd in list.indices.plus(list.size))
        require(insertBefore in list.indices.plus(list.size)) // allow inserting at the end of the list

        if (insertBefore == rangeStart) return list

        return if (insertBefore < rangeStart) {
            val combined = list.subList(0, insertBefore) +
                list.subList(rangeStart, rangeEnd) +
                list.subList(insertBefore, rangeStart) +
                list.subList(rangeEnd, list.size)

            ArrayList(combined)
        } else {
            val combined = list.subList(0, rangeStart) +
                list.subList(rangeEnd, insertBefore) +
                list.subList(rangeStart, rangeEnd) +
                list.subList(insertBefore, list.size)

            ArrayList(combined)
        }
    }

    companion object {
        val alphaList = "abcdefghij".toList()

        @JvmStatic
        fun testApplyTo(): List<ApplyToCase> {
            return listOf(
                ApplyToCase(
                    operation = ReorderCalculator.ReorderOperation(rangeStart = 0, rangeLength = 1, insertBefore = 0),
                    expected = alphaList,
                ),

                ApplyToCase(
                    operation = ReorderCalculator.ReorderOperation(rangeStart = 0, rangeLength = 1, insertBefore = 1),
                    expected = alphaList,
                ),

                ApplyToCase(
                    operation = ReorderCalculator.ReorderOperation(rangeStart = 0, rangeLength = 1, insertBefore = 2),
                    expected = "bacdefghij".toList(),
                ),

                ApplyToCase(
                    operation = ReorderCalculator.ReorderOperation(rangeStart = 4, rangeLength = 2, insertBefore = 2),
                    expected = "abefcdghij".toList(),
                ),

                ApplyToCase(
                    operation = ReorderCalculator.ReorderOperation(rangeStart = 4, rangeLength = 2, insertBefore = 8),
                    expected = "abcdghefij".toList(),
                ),
            )
        }

        @JvmStatic
        fun testOrder(): List<OrderCase> {
            val rand = Random(0)

            return listOf(
                OrderCase("abcd".toList(), expectedOperationsCount = 0),
                OrderCase("dbca".toList(), expectedOperationsCount = 3),
                OrderCase("acbd".toList(), expectedOperationsCount = 1),
                OrderCase("bcad".toList(), expectedOperationsCount = 1),
                OrderCase(alphaList, expectedOperationsCount = 0),
                OrderCase(alphaList.reversed(), expectedOperationsCount = alphaList.size - 1),
                OrderCase(alphaList.shuffled(rand), expectedOperationsCount = 9),
                OrderCase(alphaList.shuffled(rand), expectedOperationsCount = 8),
                OrderCase(alphaList.shuffled(rand), expectedOperationsCount = 8),
                OrderCase(alphaList.shuffled(rand), expectedOperationsCount = 8),
                OrderCase(alphaList.shuffled(rand), expectedOperationsCount = 6),
            )
        }
    }
}
