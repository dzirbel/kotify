package com.dominiczirbel.ui

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/**
 * Wraps the common setup for testing a [Presenter].
 */
fun <S, E, P : Presenter<S, E>> testPresenter(
    createPresenter: CoroutineScope.() -> P,
    beforeOpen: (suspend (P) -> Unit)? = null,
    block: suspend TestCoroutineScope.(P) -> Unit
) {
    runBlockingTest {
        val presenter = createPresenter()

        beforeOpen?.invoke(presenter)

        val job = launch { presenter.open() }

        block(presenter)

        presenter.close()

        job.cancel()
    }
}

internal class PresenterTest {
    private data class State(val field: String)
    private data class Event(
        val param: String,
        val delay: Long? = null,
        val throwable: Throwable? = null,
        val block: (suspend () -> Unit)? = null
    )

    private class TestPresenter(
        scope: CoroutineScope,
        eventMergeStrategy: EventMergeStrategy = EventMergeStrategy.MERGE,
        startingEvents: List<Event>? = null
    ) : Presenter<State, Event>(
        eventMergeStrategy = eventMergeStrategy,
        startingEvents = startingEvents,
        scope = scope,
        initialState = State("initial")
    ) {

        // no-op logging for testing
        override fun log(message: String) {}
        override fun logException(throwable: Throwable) {}

        override suspend fun reactTo(event: Event) {
            event.delay?.let { delay(it) }

            event.throwable?.let { throw it }

            event.block?.invoke()

            mutateState {
                it.copy(field = "${it.field} | ${event.param}")
            }
        }
    }

    @Test
    fun testEventBeforeOpen() {
        testPresenter(
            createPresenter = { TestPresenter(this) },
            beforeOpen = { presenter ->
                presenter.emit(Event("e1"))
            }
        ) { presenter ->
            assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial"))
        }
    }

    @Test
    fun testStartingEvents() {
        testPresenter({ TestPresenter(scope = this, startingEvents = listOf(Event("e1"), Event("e2"))) }) { presenter ->
            assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial | e1 | e2"))
        }
    }

    @ParameterizedTest
    @EnumSource(Presenter.EventMergeStrategy::class)
    fun testException(eventMergeStrategy: Presenter.EventMergeStrategy) {
        val throwable = Throwable()
        testPresenter({ TestPresenter(scope = this, eventMergeStrategy = eventMergeStrategy) }) { presenter ->
            assertThat(presenter.errors).isEmpty()

            coroutineScope { presenter.emit(Event(param = "1", throwable = throwable)) }
            assertThrows<Throwable> { presenter.testState.stateOrThrow }
            assertThat(presenter.testState.safeState).isEqualTo(State("initial"))
            assertThat(presenter.errors).hasSize(1)

            coroutineScope { presenter.emit(Event(param = "2")) }
            assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial | 2"))
            assertThat(presenter.errors).hasSize(1)

            coroutineScope { presenter.emit(Event(param = "3", throwable = throwable)) }
            assertThrows<Throwable> { presenter.testState.stateOrThrow }
            assertThat(presenter.testState.safeState).isEqualTo(State("initial | 2"))
            assertThat(presenter.errors).hasSize(2)

            coroutineScope { presenter.emit(Event(param = "4")) }
            assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial | 2 | 4"))
            assertThat(presenter.errors).hasSize(2)
        }
    }

    @ParameterizedTest
    @EnumSource(Presenter.EventMergeStrategy::class)
    @Suppress("TooGenericExceptionThrown")
    fun testAsyncException(eventMergeStrategy: Presenter.EventMergeStrategy) {
        testPresenter({ TestPresenter(scope = this, eventMergeStrategy = eventMergeStrategy) }) { presenter ->
            coroutineScope {
                presenter.emit(
                    Event(
                        param = "1",
                        block = { throw Throwable() }
                    )
                )

                assertThrows<Throwable> { presenter.testState.stateOrThrow }

                presenter.emit(Event(param = "1"))
                assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial | 1"))

                presenter.emit(
                    Event(
                        param = "1",
                        block = {
                            coroutineScope {
                                launch { throw Throwable() }
                            }
                        }
                    )
                )

                assertThrows<Throwable> { presenter.testState.stateOrThrow }
            }
        }
    }

    @RepeatedTest(10)
    fun testMerge() {
        testPresenter(
            { TestPresenter(scope = this, eventMergeStrategy = Presenter.EventMergeStrategy.MERGE) }
        ) { presenter ->
            assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial"))

            launch { presenter.emit(Event("e1", delay = 10)) }

            assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial"))
            delay(5)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial"))
            delay(10)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial | e1"))

            launch { presenter.emit(Event("e2", delay = 10)) }
            launch { presenter.emit(Event("e3", delay = 50)) }

            assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial | e1"))
            delay(5)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial | e1"))
            delay(10)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial | e1 | e2"))
            delay(20)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial | e1 | e2"))
            delay(20)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial | e1 | e2 | e3"))
        }
    }

    @RepeatedTest(10)
    fun testLatest() {
        testPresenter(
            { TestPresenter(scope = this, eventMergeStrategy = Presenter.EventMergeStrategy.LATEST) }
        ) { presenter ->
            assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial"))

            launch { presenter.emit(Event("e1", delay = 10)) }

            assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial"))
            delay(5)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial"))
            delay(10)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial | e1"))

            launch { presenter.emit(Event("e2", delay = 10)) }
            launch { presenter.emit(Event("e3", delay = 50)) }

            assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial | e1"))
            delay(5)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial | e1"))
            delay(10)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial | e1"))
            delay(20)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial | e1"))
            delay(20)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(State("initial | e1 | e3"))
        }
    }
}
