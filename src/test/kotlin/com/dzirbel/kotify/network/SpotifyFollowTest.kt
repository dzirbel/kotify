package com.dzirbel.kotify.network

import com.dzirbel.kotify.Fixtures
import com.dzirbel.kotify.TAG_NETWORK
import com.google.common.truth.Truth.assertThat
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
            Spotify.Follow.isFollowing(type = "artist", ids = Fixtures.followingArtists.map { it.first })
        }

        assertThat(followedArtists).isEqualTo(Fixtures.followingArtists.map { it.second })
    }

    @Test
    fun isFollowingUsers() {
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
        val artists = runBlocking {
            Spotify.Follow.getFollowedArtists(limit = 50)
                .fetchAllCustom { Spotify.get<Spotify.ArtistsCursorPagingModel>(it).artists }
        }

        assertThat(artists).isNotEmpty()

        Fixtures.followingArtists.forEach { (artistId, following) ->
            assertThat(artists.any { it.id == artistId }).isEqualTo(following)
        }
    }

    @Test
    fun followAndUnfollowArtist() {
        assertThat(runBlocking { Spotify.Follow.isFollowing(type = "artist", ids = Fixtures.testFollowingArtists) })
            .containsExactlyElementsIn(Fixtures.testFollowingArtists.map { false })
            .inOrder()

        runBlocking { Spotify.Follow.follow(type = "artist", ids = Fixtures.testFollowingArtists) }

        assertThat(runBlocking { Spotify.Follow.isFollowing(type = "artist", ids = Fixtures.testFollowingArtists) })
            .containsExactlyElementsIn(Fixtures.testFollowingArtists.map { true })
            .inOrder()

        runBlocking { Spotify.Follow.unfollow(type = "artist", ids = Fixtures.testFollowingArtists) }

        assertThat(runBlocking { Spotify.Follow.isFollowing(type = "artist", ids = Fixtures.testFollowingArtists) })
            .containsExactlyElementsIn(Fixtures.testFollowingArtists.map { false })
            .inOrder()
    }

    @Test
    fun followAndUnfollowPlaylist() {
        assertCurrentUserIsFollowingPlaylist(playlistId = Fixtures.testFollowingPlaylist, following = false)

        runBlocking { Spotify.Follow.followPlaylist(Fixtures.testFollowingPlaylist) }

        assertCurrentUserIsFollowingPlaylist(playlistId = Fixtures.testFollowingPlaylist, following = true)

        runBlocking { Spotify.Follow.unfollowPlaylist(Fixtures.testFollowingPlaylist) }

        assertCurrentUserIsFollowingPlaylist(playlistId = Fixtures.testFollowingPlaylist, following = false)
    }

    private fun assertCurrentUserIsFollowingPlaylist(playlistId: String, following: Boolean) {
        assertThat(
            runBlocking {
                Spotify.Follow.isFollowingPlaylist(playlistId = playlistId, userIds = listOf(Fixtures.userId))
            }
        ).containsExactly(following)
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun playlistFollows() = Fixtures.followingPlaylists
    }
}
