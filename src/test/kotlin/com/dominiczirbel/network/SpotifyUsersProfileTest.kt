package com.dominiczirbel.network

import com.dominiczirbel.Secrets
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class SpotifyUsersProfileTest {
    @Test
    fun getCurrentUser() {
        val user = runBlocking { Spotify.UsersProfile.getCurrentUser() }

        assertThat(user.displayName).isEqualTo("djynth")
        assertThat(user.email).isEqualTo("dominiczirbel@gmail.com")
        // TODO test the rest of the object
    }

    companion object {
        @BeforeAll
        @JvmStatic
        @Suppress("unused")
        fun setup() {
            Secrets.load()
            Secrets.authenticate()
        }
    }
}
