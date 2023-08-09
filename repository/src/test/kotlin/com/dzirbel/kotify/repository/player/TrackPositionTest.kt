package com.dzirbel.kotify.repository.player

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.dzirbel.kotify.util.CurrentTime
import org.junit.jupiter.api.Test

class TrackPositionTest {
    @Test
    fun `playing position has live position`() {
        val position = TrackPosition.Fetched(fetchedTimestamp = 50, fetchedPositionMs = 10, playing = true)
        CurrentTime.mocked(55) { assertThat(position.currentPositionMs).isEqualTo(15) }
        CurrentTime.mocked(56) { assertThat(position.currentPositionMs).isEqualTo(16) }
    }

    @Test
    fun `pause from play retains position at pause time`() {
        val playingPosition = TrackPosition.Fetched(fetchedTimestamp = 50, fetchedPositionMs = 10, playing = true)

        CurrentTime.mocked(55) { assertThat(playingPosition.currentPositionMs).isEqualTo(15) }

        val pausedPosition = playingPosition.pause(pauseTimestamp = 65)

        CurrentTime.mocked(65) { assertThat(pausedPosition.currentPositionMs).isEqualTo(25) }
        CurrentTime.mocked(75) { assertThat(pausedPosition.currentPositionMs).isEqualTo(25) }
    }

    @Test
    fun `play from pause resumes live position`() {
        val pausedPosition = TrackPosition.Fetched(fetchedTimestamp = 50, fetchedPositionMs = 10, playing = false)

        CurrentTime.mocked(55) { assertThat(pausedPosition.currentPositionMs).isEqualTo(10) }

        val playingPosition = pausedPosition.play(playTimestamp = 65)

        CurrentTime.mocked(65) { assertThat(playingPosition.currentPositionMs).isEqualTo(10) }
        CurrentTime.mocked(75) { assertThat(playingPosition.currentPositionMs).isEqualTo(20) }
    }
}
