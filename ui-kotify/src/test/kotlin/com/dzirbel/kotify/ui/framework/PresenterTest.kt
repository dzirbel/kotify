package com.dzirbel.kotify.ui.framework

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class PresenterTest {
    private data class ViewModel(val field: String)

    sealed class Event {
        abstract val param: String
        abstract val delay: Long?
        abstract val throwable: Throwable?
        abstract val block: (suspend () -> Unit)?

        data class A(
            override val param: String,
            override val delay: Long? = null,
            override val throwable: Throwable? = null,
            override val block: (suspend () -> Unit)? = null,
        ) : Event()

        data class B(
            override val param: String,
            override val delay: Long? = null,
            override val throwable: Throwable? = null,
            override val block: (suspend () -> Unit)? = null,
        ) : Event()
    }

    private class TestPresenter(
        scope: CoroutineScope,
        eventMergeStrategy: EventMergeStrategy = EventMergeStrategy.MERGE,
        startingEvents: List<Event>? = null,
    ) : Presenter<ViewModel, Event>(
        eventMergeStrategy = eventMergeStrategy,
        startingEvents = startingEvents,
        scope = scope,
        initialState = ViewModel("initial"),
    ) {
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
                presenter.emit(Event.A("e1"))
            },
        ) { presenter ->
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial"))
        }
    }

    @Test
    fun testStartingEvents() {
        testPresenter(
            { TestPresenter(scope = this, startingEvents = listOf(Event.A("e1"), Event.A("e2"))) },
        ) { presenter ->
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1 | e2"))
        }
    }

    @ParameterizedTest
    @EnumSource(Presenter.EventMergeStrategy::class)
    fun testException(eventMergeStrategy: Presenter.EventMergeStrategy) {
        val throwable = Throwable()
        testPresenter({ TestPresenter(scope = this, eventMergeStrategy = eventMergeStrategy) }) { presenter ->
            assertThat(presenter.errors).isEmpty()

            presenter.emit(Event.A(param = "1", throwable = throwable))
            advanceUntilIdle()

            assertThrows<Throwable> { presenter.testState.stateOrThrow }
            assertThat(presenter.testState.safeState).isEqualTo(ViewModel("initial"))
            assertThat(presenter.errors).hasSize(1)

            presenter.emit(Event.A(param = "2"))
            advanceUntilIdle()

            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | 2"))
            assertThat(presenter.errors).hasSize(1)

            presenter.emit(Event.A(param = "3", throwable = throwable))
            advanceUntilIdle()

            assertThrows<Throwable> { presenter.testState.stateOrThrow }
            assertThat(presenter.testState.safeState).isEqualTo(ViewModel("initial | 2"))
            assertThat(presenter.errors).hasSize(2)

            presenter.emit(Event.A(param = "4"))
            advanceUntilIdle()

            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | 2 | 4"))
            assertThat(presenter.errors).hasSize(2)
        }
    }

    @ParameterizedTest
    @EnumSource(Presenter.EventMergeStrategy::class)
    @Suppress("TooGenericExceptionThrown")
    fun testAsyncException(eventMergeStrategy: Presenter.EventMergeStrategy) {
        testPresenter({ TestPresenter(scope = this, eventMergeStrategy = eventMergeStrategy) }) { presenter ->
            presenter.emit(
                Event.A(
                    param = "1",
                    block = { throw Throwable() },
                ),
            )
            advanceUntilIdle()
            assertThrows<Throwable> { presenter.testState.stateOrThrow }

            presenter.emit(Event.A(param = "1"))
            advanceUntilIdle()
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | 1"))

            presenter.emit(
                Event.A(
                    param = "1",
                    block = {
                        coroutineScope {
                            launch { throw Throwable() }
                        }
                    },
                ),
            )
            advanceUntilIdle()
            assertThrows<Throwable> { presenter.testState.stateOrThrow }
        }
    }

    @RepeatedTest(10)
    fun testMerge() {
        testPresenter(
            { TestPresenter(scope = this, eventMergeStrategy = Presenter.EventMergeStrategy.MERGE) },
        ) { presenter ->
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial"))

            presenter.emit(Event.A("e1", delay = 10))

            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial"))
            delay(5)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial"))
            delay(10)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1"))

            presenter.emit(Event.A("e2", delay = 10))
            presenter.emit(Event.A("e3", delay = 50))

            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1"))
            delay(5)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1"))
            delay(10)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1 | e2"))
            delay(20)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1 | e2"))
            delay(20)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1 | e2 | e3"))
        }
    }

    @RepeatedTest(10)
    fun testLatest() {
        testPresenter(
            { TestPresenter(scope = this, eventMergeStrategy = Presenter.EventMergeStrategy.LATEST) },
        ) { presenter ->
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial"))

            presenter.emit(Event.A("e1", delay = 10))

            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial"))
            delay(5)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial"))
            delay(10)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1"))

            presenter.emit(Event.A("e2", delay = 10))
            presenter.emit(Event.A("e3", delay = 50))

            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1"))
            delay(5)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1"))
            delay(10)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1"))
            delay(20)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1"))
            delay(20)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1 | e3"))
        }
    }

    @RepeatedTest(10)
    fun testLatestByClass() {
        testPresenter(
            { TestPresenter(scope = this, eventMergeStrategy = Presenter.EventMergeStrategy.LATEST_BY_CLASS) },
        ) { presenter ->
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial"))

            presenter.emit(Event.A("e1", delay = 10))

            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial"))
            delay(5)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial"))
            delay(10)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1"))

            // events of the same class only use the latest
            presenter.emit(Event.A("e2", delay = 10))
            presenter.emit(Event.A("e3", delay = 50))

            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1"))
            delay(5)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1"))
            delay(10)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1"))
            delay(20)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1"))
            delay(20)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1 | e3"))

            // events of different classes will process both in parallel
            presenter.emit(Event.A("e4", delay = 10))
            presenter.emit(Event.B("e5", delay = 50))

            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1 | e3"))
            delay(5)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1 | e3"))
            delay(10)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1 | e3 | e4"))
            delay(20)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1 | e3 | e4"))
            delay(20)
            assertThat(presenter.testState.stateOrThrow).isEqualTo(ViewModel("initial | e1 | e3 | e4 | e5"))
        }
    }
}
