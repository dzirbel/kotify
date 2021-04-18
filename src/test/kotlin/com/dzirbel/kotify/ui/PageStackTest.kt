package com.dzirbel.kotify.ui

import com.dzirbel.kotify.ui.common.Page
import com.dzirbel.kotify.ui.common.PageStack
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class PageStackTest {
    private data class TestPage(val id: Int) : Page

    @Test
    fun testSingleton() {
        val page = TestPage(1)
        val stack = PageStack(TestPage(1))

        assertThat(stack.currentIndex).isEqualTo(0)
        assertThat(stack.pages).containsExactly(page)
        assertThat(stack.current).isEqualTo(page)
        assertThat(stack.previous).isNull()
        assertThat(stack.hasPrevious).isFalse()
        assertThat(stack.next).isNull()
        assertThat(stack.hasNext).isFalse()
    }

    @Test
    fun testStack() {
        val page1 = TestPage(1)
        val page2 = TestPage(2)
        val page3 = TestPage(3)
        val page4 = TestPage(4)
        val page5 = TestPage(5)

        var stack = PageStack(page1)
        stack = stack.to(page2)
        stack = stack.to(page3)
        stack = stack.to(page4)
        stack = stack.to(page5)

        assertThat(stack.currentIndex).isEqualTo(4)
        assertThat(stack.current).isEqualTo(page5)
        assertThat(stack.hasPrevious).isTrue()
        assertThat(stack.previous).isEqualTo(page4)
        assertThat(stack.next).isNull()
        assertThat(stack.hasNext).isFalse()

        stack = stack.toPrevious()

        assertThat(stack.currentIndex).isEqualTo(3)
        assertThat(stack.current).isEqualTo(page4)
        assertThat(stack.hasPrevious).isTrue()
        assertThat(stack.previous).isEqualTo(page3)
        assertThat(stack.next).isEqualTo(page5)
        assertThat(stack.hasNext).isTrue()

        stack = stack.toNext()

        assertThat(stack.currentIndex).isEqualTo(4)
        assertThat(stack.current).isEqualTo(page5)
        assertThat(stack.hasPrevious).isTrue()
        assertThat(stack.previous).isEqualTo(page4)
        assertThat(stack.next).isNull()
        assertThat(stack.hasNext).isFalse()

        stack = stack.toPrevious()
        stack = stack.toPrevious()
        stack = stack.toPrevious()

        assertThat(stack.currentIndex).isEqualTo(1)
        assertThat(stack.current).isEqualTo(page2)
        assertThat(stack.hasPrevious).isTrue()
        assertThat(stack.previous).isEqualTo(page1)
        assertThat(stack.next).isEqualTo(page3)
        assertThat(stack.hasNext).isTrue()

        stack = stack.to(page5)

        assertThat(stack.currentIndex).isEqualTo(2)
        assertThat(stack.current).isEqualTo(page5)
        assertThat(stack.hasPrevious).isTrue()
        assertThat(stack.previous).isEqualTo(page2)
        assertThat(stack.next).isNull()
        assertThat(stack.hasNext).isFalse()

        stack = stack.toIndex(index = 1)

        assertThat(stack.currentIndex).isEqualTo(1)
        assertThat(stack.current).isEqualTo(page2)
        assertThat(stack.hasPrevious).isTrue()
        assertThat(stack.previous).isEqualTo(page1)
        assertThat(stack.next).isEqualTo(page5)
        assertThat(stack.hasNext).isTrue()
    }

    @Test
    fun testDuplicates() {
        val page1 = TestPage(1)
        val page1b = TestPage(1)

        var stack = PageStack(page1)

        assertThat(stack.to(page1, allowDuplicate = false)).isSameInstanceAs(stack)
        assertThat(stack.to(page1b, allowDuplicate = false)).isSameInstanceAs(stack)

        stack = stack.to(page1, allowDuplicate = true)
        assertThat(stack.currentIndex).isEqualTo(1)
        assertThat(stack.current).isEqualTo(page1)
        assertThat(stack.previous).isEqualTo(page1)
    }
}
