package com.dzirbel.kotify.network.properties

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.dzirbel.kotify.network.NetworkFixtures
import com.dzirbel.kotify.network.model.FullSpotifyPlaylist
import com.dzirbel.kotify.network.model.SpotifyPlaylist
import com.dzirbel.kotify.network.model.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

data class PlaylistProperties(
    override val id: String,
    override val name: String,
    val description: String,
    val tracks: List<TrackProperties>? = null,
    val public: Boolean? = false,
    val owner: String = NetworkFixtures.userDisplayName,
) : ObjectProperties(type = "playlist") {
    fun check(playlist: SpotifyPlaylist) {
        super.check(playlist)

        assertThat(playlist.description).isEqualTo(description)
        // assertThat(playlist.public).isEqualTo(public) TODO appears to be incorrectly reported by spotify
        assertThat(playlist.owner.displayName).isEqualTo(owner)
        if (tracks != null && playlist is FullSpotifyPlaylist) {
            val allItems = runBlocking { playlist.tracks.asFlow().toList() }
            tracks.zip(allItems).forEach { (trackProperties, playlistTrack) ->
                trackProperties.check(playlistTrack)
            }
        }
    }
}
