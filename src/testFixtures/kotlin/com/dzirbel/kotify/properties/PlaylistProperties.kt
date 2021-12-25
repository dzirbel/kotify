package com.dzirbel.kotify.properties

import com.dzirbel.kotify.Fixtures
import com.dzirbel.kotify.network.model.FullSpotifyPlaylist
import com.dzirbel.kotify.network.model.SpotifyPlaylist
import com.dzirbel.kotify.network.model.SpotifyPlaylistTrack
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking

data class PlaylistProperties(
    override val id: String,
    override val name: String,
    val description: String,
    val tracks: List<TrackProperties>? = null,
    val public: Boolean? = false,
    val owner: String = Fixtures.userDisplayName
) : ObjectProperties(type = "playlist") {
    fun check(playlist: SpotifyPlaylist) {
        super.check(playlist)

        assertThat(playlist.description).isEqualTo(description)
        assertThat(playlist.public).isEqualTo(public)
        assertThat(playlist.owner.displayName).isEqualTo(owner)
        if (tracks != null && playlist is FullSpotifyPlaylist) {
            val allItems = runBlocking { playlist.tracks.fetchAll<SpotifyPlaylistTrack>() }
            tracks.zip(allItems).forEach { (trackProperties, playlistTrack) ->
                trackProperties.check(playlistTrack)
            }
        }
    }
}
