package com.dominiczirbel.network

import com.dominiczirbel.Fixtures
import com.dominiczirbel.network.model.FullArtist
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class SpotifyFollowsTest {
    @Test
    fun isFollowing() {
        val followedArtists = runBlocking {
            Spotify.Follow.isFollowing(type = "artist", ids = Fixtures.followingArtists.map { it.first })
        }
        assertThat(followedArtists).isEqualTo(Fixtures.followingArtists.map { it.second })

        val followedUsers = runBlocking {
            Spotify.Follow.isFollowing(type = "user", ids = Fixtures.followingUsers.map { it.first })
        }
        assertThat(followedUsers).isEqualTo(Fixtures.followingUsers.map { it.second })
    }

    @ParameterizedTest
    @MethodSource("playlistFollows")
    fun isFollowingPlaylist(data: Pair<String, Map<String, Boolean>>) {
        val (playlistId, followersMap) = data
        val followersList = followersMap.toList()
        val followingState = runBlocking {
            Spotify.Follow.isFollowingPlaylist(playlistId = playlistId, userIds = followersList.map { it.first })
        }

        assertThat(followingState).isEqualTo(followersList.map { it.second })
    }

    @Test
    fun getFollowedArtists() {
        val allArtists = mutableListOf<FullArtist>()
        var artists = runBlocking { Spotify.Follow.getFollowedArtists(limit = 50) }
        allArtists.addAll(artists.items)

        while (artists.cursors.after != null) {
            artists = runBlocking { Spotify.Follow.getFollowedArtists(limit = 50, after = artists.cursors.after) }
            allArtists.addAll(artists.items)
        }

        assertThat(allArtists).isNotEmpty()

        Fixtures.followingArtists.forEach { (artistId, following) ->
            assertThat(allArtists.any { it.id == artistId }).isEqualTo(following)
        }
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun playlistFollows() = Fixtures.followingPlaylists
    }
}
