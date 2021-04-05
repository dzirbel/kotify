package com.dominiczirbel.ui

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

internal class PresenterTest {
    private data class State(val field: String)
    private data class Event(val param: String, val delay: Long? = null)

    private class TestPresenter(
        eventMergeStrategy: EventMergeStrategy = EventMergeStrategy.MERGE,
        startingEvents: List<Event>? = null
    ) : Presenter<State, Event>(
        eventMergeStrategy = eventMergeStrategy,
        startingEvents = startingEvents,
        scope = GlobalScope,
        initialState = State("initial")
    ) {

        override suspend fun reactTo(event: Event) {
            event.delay?.let { delay(it) }

            mutateState {
                it.copy(field = "${it.field} | ${event.param}")
            }
        }
    }

    private fun wrapPresenterOpen(
        presenter: TestPresenter,
        beforeOpen: (suspend (TestPresenter) -> Unit)? = null,
        block: suspend TestCoroutineScope.(TestPresenter) -> Unit
    ) {
        runBlockingTest {
            beforeOpen?.invoke(presenter)

            val job = launch { presenter.open() }

            block(presenter)

            job.cancel()
        }
    }

    @Test
    fun testEventBeforeOpen() {
        wrapPresenterOpen(
            presenter = TestPresenter(),
            beforeOpen = { presenter ->
                presenter.events.emit(Event("e1"))
            }
        ) { presenter ->
            assertThat(presenter.state).isEqualTo(State("initial"))
        }
    }

    @Test
    fun testStartingEvents() {
        wrapPresenterOpen(TestPresenter(startingEvents = listOf(Event("e1"), Event("e2")))) { presenter ->
            assertThat(presenter.state).isEqualTo(State("initial | e1 | e2"))
        }
    }

    @RepeatedTest(10)
    fun testMerge() {
        wrapPresenterOpen(TestPresenter(eventMergeStrategy = Presenter.EventMergeStrategy.MERGE)) { presenter ->
            assertThat(presenter.state).isEqualTo(State("initial"))

            launch { presenter.events.emit(Event("e1", delay = 10)) }

            assertThat(presenter.state).isEqualTo(State("initial"))
            delay(5)
            assertThat(presenter.state).isEqualTo(State("initial"))
            delay(10)
            assertThat(presenter.state).isEqualTo(State("initial | e1"))

            launch { presenter.events.emit(Event("e2", delay = 10)) }
            launch { presenter.events.emit(Event("e3", delay = 50)) }

            assertThat(presenter.state).isEqualTo(State("initial | e1"))
            delay(5)
            assertThat(presenter.state).isEqualTo(State("initial | e1"))
            delay(10)
            assertThat(presenter.state).isEqualTo(State("initial | e1 | e2"))
            delay(20)
            assertThat(presenter.state).isEqualTo(State("initial | e1 | e2"))
            delay(20)
            assertThat(presenter.state).isEqualTo(State("initial | e1 | e2 | e3"))
        }
    }

    @RepeatedTest(10)
    fun testLatest() {
        wrapPresenterOpen(TestPresenter(eventMergeStrategy = Presenter.EventMergeStrategy.LATEST)) { presenter ->
            assertThat(presenter.state).isEqualTo(State("initial"))

            launch { presenter.events.emit(Event("e1", delay = 10)) }

            assertThat(presenter.state).isEqualTo(State("initial"))
            delay(5)
            assertThat(presenter.state).isEqualTo(State("initial"))
            delay(10)
            assertThat(presenter.state).isEqualTo(State("initial | e1"))

            launch { presenter.events.emit(Event("e2", delay = 10)) }
            launch { presenter.events.emit(Event("e3", delay = 50)) }

            assertThat(presenter.state).isEqualTo(State("initial | e1"))
            delay(5)
            assertThat(presenter.state).isEqualTo(State("initial | e1"))
            delay(10)
            assertThat(presenter.state).isEqualTo(State("initial | e1"))
            delay(20)
            assertThat(presenter.state).isEqualTo(State("initial | e1"))
            delay(20)
            assertThat(presenter.state).isEqualTo(State("initial | e1 | e3"))
        }
    }
}
