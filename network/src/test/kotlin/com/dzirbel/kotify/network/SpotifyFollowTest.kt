package com.dzirbel.kotify.network

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.util.containsExactlyElementsOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Tag(TAG_NETWORK)
class SpotifyFollowTest {
    @Test
    fun isFollowingArtists() {
        val followedArtists = runBlocking {
            Spotify.Follow.isFollowing(type = "artist", ids = NetworkFixtures.followingArtists.map { it.first })
        }

        assertThat(followedArtists).containsExactlyElementsOf(NetworkFixtures.followingArtists.map { it.second })
    }

    @Test
    fun isFollowingUsers() {
        val followedUsers = runBlocking {
            Spotify.Follow.isFollowing(type = "user", ids = NetworkFixtures.followingUsers.map { it.first })
        }

        assertThat(followedUsers).containsExactlyElementsOf(NetworkFixtures.followingUsers.map { it.second })
    }

    @ParameterizedTest
    @MethodSource("playlistFollows")
    fun isFollowingPlaylist(data: Pair<String, Map<String, Boolean>>) {
        val (playlistId, followersMap) = data
        val followersList = followersMap.toList()
        val followingState = runBlocking {
            Spotify.Follow.isFollowingPlaylist(playlistId = playlistId, userIds = followersList.map { it.first })
        }

        assertThat(followingState).containsExactlyElementsOf(followersList.map { it.second })
    }

    @Test
    fun getFollowedArtists() {
        val artists = runBlocking { Spotify.Follow.getFollowedArtists(limit = 50).asFlow().toList() }

        assertThat(artists).isNotEmpty()

        NetworkFixtures.followingArtists.forEach { (artistId, following) ->
            assertThat(artists.any { it.id == artistId }).isEqualTo(following)
        }
    }

    @Test
    fun followAndUnfollowArtist() {
        runBlocking {
            assertThat(Spotify.Follow.isFollowing(type = "artist", ids = NetworkFixtures.testFollowingArtists))
                .containsExactlyElementsOf(NetworkFixtures.testFollowingArtists.map { false })

            Spotify.Follow.follow(type = "artist", ids = NetworkFixtures.testFollowingArtists)

            assertThat(Spotify.Follow.isFollowing(type = "artist", ids = NetworkFixtures.testFollowingArtists))
                .containsExactlyElementsOf(NetworkFixtures.testFollowingArtists.map { true })

            Spotify.Follow.unfollow(type = "artist", ids = NetworkFixtures.testFollowingArtists)

            assertThat(Spotify.Follow.isFollowing(type = "artist", ids = NetworkFixtures.testFollowingArtists))
                .containsExactlyElementsOf(NetworkFixtures.testFollowingArtists.map { false })
        }
    }

    @Test
    fun followAndUnfollowPlaylist() {
        assertCurrentUserIsFollowingPlaylist(playlistId = NetworkFixtures.testFollowingPlaylist, following = false)

        runBlocking { Spotify.Follow.followPlaylist(NetworkFixtures.testFollowingPlaylist) }

        assertCurrentUserIsFollowingPlaylist(playlistId = NetworkFixtures.testFollowingPlaylist, following = true)

        runBlocking { Spotify.Follow.unfollowPlaylist(NetworkFixtures.testFollowingPlaylist) }

        assertCurrentUserIsFollowingPlaylist(playlistId = NetworkFixtures.testFollowingPlaylist, following = false)
    }

    private fun assertCurrentUserIsFollowingPlaylist(playlistId: String, following: Boolean) {
        assertThat(
            runBlocking {
                Spotify.Follow.isFollowingPlaylist(playlistId = playlistId, userIds = listOf(NetworkFixtures.userId))
            },
        ).containsExactly(following)
    }

    companion object {
        @JvmStatic
        fun playlistFollows() = NetworkFixtures.followingPlaylists
    }
}
