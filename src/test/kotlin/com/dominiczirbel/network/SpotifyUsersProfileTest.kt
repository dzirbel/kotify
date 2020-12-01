package com.dominiczirbel.network

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class SpotifyUsersProfileTest {
    @Test
    fun getCurrentUser() {
        val user = runBlocking { Spotify.UsersProfile.getCurrentUser() }

        assertThat(user.displayName).isEqualTo("djynth")
        assertThat(user.email).isEqualTo("dominiczirbel@gmail.com")
        // TODO test the rest of the object
    }
}
