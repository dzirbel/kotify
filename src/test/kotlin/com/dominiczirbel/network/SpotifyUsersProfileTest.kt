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
        assertThat(user.id).isEqualTo("djynth")
        assertThat(user.country).isEqualTo("US")
        assertThat(user.product).isEqualTo("premium")
        assertThat(user.explicitContent.filterEnabled).isFalse()
        assertThat(user.explicitContent.filterLocked).isFalse()
    }

    @Test
    fun getUser() {
        val user = runBlocking { Spotify.UsersProfile.getUser("djynth") }

        assertThat(user.displayName).isEqualTo("djynth")
        assertThat(user.id).isEqualTo("djynth")
    }
}
