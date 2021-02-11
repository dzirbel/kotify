package com.dominiczirbel.network

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class SpotifyUsersProfileTest {
    @Test
    fun getCurrentUser() {
        val user = runBlocking { Spotify.UsersProfile.getCurrentUser() }

        assertThat(user.displayName).isEqualTo("Test")
        assertThat(user.email).isEqualTo("dominiczirbel+test@gmail.com")
        assertThat(user.id).isEqualTo("34m1o83qloqkyzdt4z3qbveoy")
        assertThat(user.country).isEqualTo("US")
        assertThat(user.product).isEqualTo("open")
        assertThat(user.explicitContent.filterEnabled).isFalse()
        assertThat(user.explicitContent.filterLocked).isFalse()
    }

    @Test
    fun getUser() {
        val user = runBlocking { Spotify.UsersProfile.getUser("34m1o83qloqkyzdt4z3qbveoy") }

        assertThat(user.displayName).isEqualTo("Test")
        assertThat(user.id).isEqualTo("34m1o83qloqkyzdt4z3qbveoy")
    }
}
