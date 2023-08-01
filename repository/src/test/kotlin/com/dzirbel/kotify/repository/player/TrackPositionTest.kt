package com.dzirbel.kotify.repository.player

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TrackPositionTest {
    @BeforeEach
    fun setup() {
        mockkObject(TrackPosition)
    }

    @AfterEach
    fun cleanup() {
        unmockkObject(TrackPosition)
    }

    @Test
    fun `playing position has live position`() {
        val position = TrackPosition.Fetched(fetchedTimestamp = 50, fetchedPositionMs = 10, playing = true)

        every { TrackPosition.currentTime() } returns 55
        assertThat(position.currentPositionMs).isEqualTo(15)

        every { TrackPosition.currentTime() } returns 56
        assertThat(position.currentPositionMs).isEqualTo(16)
    }

    @Test
    fun `pause from play retains position at pause time`() {
        val playingPosition = TrackPosition.Fetched(fetchedTimestamp = 50, fetchedPositionMs = 10, playing = true)

        every { TrackPosition.currentTime() } returns 55
        assertThat(playingPosition.currentPositionMs).isEqualTo(15)

        val pausedPosition = playingPosition.pause(pauseTimestamp = 65)

        every { TrackPosition.currentTime() } returns 65
        assertThat(pausedPosition.currentPositionMs).isEqualTo(25)

        every { TrackPosition.currentTime() } returns 75
        assertThat(pausedPosition.currentPositionMs).isEqualTo(25)
    }

    @Test
    fun `play from pause resumes live position`() {
        val pausedPosition = TrackPosition.Fetched(fetchedTimestamp = 50, fetchedPositionMs = 10, playing = false)

        every { TrackPosition.currentTime() } returns 55
        assertThat(pausedPosition.currentPositionMs).isEqualTo(10)

        val playingPosition = pausedPosition.play(playTimestamp = 65)

        every { TrackPosition.currentTime() } returns 65
        assertThat(playingPosition.currentPositionMs).isEqualTo(10)

        every { TrackPosition.currentTime() } returns 75
        assertThat(playingPosition.currentPositionMs).isEqualTo(20)
    }
}
