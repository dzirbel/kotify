package com.dzirbel.kotify.ui.framework

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.repository.Player
import com.dzirbel.kotify.repository.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/**
 * Wraps the common setup for testing a [Presenter].
 */
fun <S, E, P : Presenter<S, E>> testPresenter(
    createPresenter: CoroutineScope.() -> P,
    beforeOpen: (suspend (P) -> Unit)? = null,
    @Suppress("SuspendFunWithCoroutineScopeReceiver")
    block: suspend TestScope.(P) -> Unit,
) {
    KotifyDatabase.withSynchronousTransactions {
        runTest {
            Repository.withRepositoryScope(this) {
                Player.withPlayerScope(this) {
                    val presenter = createPresenter()

                    beforeOpen?.invoke(presenter)

                    val job = launch { presenter.open() }
                    advanceUntilIdle()

                    block(presenter)

                    job.cancel()
                }
            }
        }
    }
}

/**
 * Convenience function which emits [event] and then advances this [TestScope] until idle, i.e. processing of the event
 * has finished.
 */
context(TestScope)
suspend fun <E : Any> Presenter<*, E>.emitAndIdle(event: E) {
    emit(event)
    advanceUntilIdle()
}

/**
 * Asserts that the current state of this [Presenter] is equal to the given [state] and there have been no
 * [Presenter.errors].
 */
fun <S> Presenter<S, *>.assertStateEquals(state: S) {
    assertThat(errors).isEmpty()
    assertThat(testState.stateOrThrow).isEqualTo(state)
}
