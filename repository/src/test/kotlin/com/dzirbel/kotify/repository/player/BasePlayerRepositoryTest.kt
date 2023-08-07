package com.dzirbel.kotify.repository.player

import com.dzirbel.kotify.network.Spotify
import io.mockk.confirmVerified
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * Base class extended by tests of [PlayerRepository], in order to split them across smaller files for better
 * organization.
 *
 * TODO test remaining methods
 */
abstract class BasePlayerRepositoryTest {
    @BeforeEach
    fun setup() {
        mockkObject(Spotify.Player)
    }

    @AfterEach
    fun cleanup() {
        confirmVerified(Spotify.Player)
        unmockkObject(Spotify.Player)
    }
}
