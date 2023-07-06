package com.dzirbel.kotify.repository.player

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.dzirbel.kotify.network.FullSpotifyTrack
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.SpotifyPlayback
import com.dzirbel.kotify.repository.Repository
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

// TODO finish test
class PlayerRepositoryTest {
    @Test
    fun refreshPlayback() {
        val track = FullSpotifyTrack()
        coEvery { Spotify.Player.getCurrentPlayback() } returns SpotifyPlayback(track = track)

        runTest {
            Repository.withRepositoryScope(scope = this) {
                assertThat(PlayerRepository.currentTrack.value).isNull()

                PlayerRepository.refreshPlayback()

                assertThat(PlayerRepository.currentTrack.value).isNull()

                runCurrent()

                assertThat(PlayerRepository.currentTrack.value).isEqualTo(track)
                // TODO very other properties
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getCurrentPlayback() }
    }
}
