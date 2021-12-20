package com.dzirbel.kotify.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class ComparatorExtensionsTest {
    @Test
    fun testCompareInOrder() {
        // first comparator is always equal
        val comparator1 = Comparator<String> { _, _ -> 0 }

        // second comparator compares on string length
        val comparator2 = Comparator<String> { s1, s2 -> s1.length.compareTo(s2.length) }

        // third comparator compares on the first character
        val comparator3 = Comparator<String> { s1, s2 -> s1[0].compareTo(s2[0]) }

        val merged = listOf(comparator1, comparator2, comparator3).compareInOrder()

        assertThat(merged.compare("a", "a")).isEqualTo(0) // a = a
        assertThat(merged.compare("ab", "ac")).isEqualTo(0) // ab = ac since only the first char is checked

        assertThat(merged.compare("a", "aa")).isLessThan(0) // a < aa due to c2
        assertThat(merged.compare("aa", "a")).isGreaterThan(0) // aa > a due to c2

        assertThat(merged.compare("a", "b")).isLessThan(0) // a < b due to c3
        assertThat(merged.compare("b", "a")).isGreaterThan(0) // b > a due to c3
    }
}
