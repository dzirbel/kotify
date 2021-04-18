package com.dzirbel.kotify.properties

import com.dzirbel.kotify.Fixtures
import com.dzirbel.kotify.network.model.FullPlaylist
import com.dzirbel.kotify.network.model.Playlist
import com.dzirbel.kotify.network.model.PlaylistTrack
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking

data class PlaylistProperties(
    override val id: String,
    override val name: String,
    val description: String,
    val tracks: List<TrackProperties>? = null,
    val public: Boolean? = false,
    val owner: String = Fixtures.userDisplayName
) : ObjectProperties(type = "playlist") {
    fun check(playlist: Playlist) {
        super.check(playlist)

        Truth.assertThat(playlist.description).isEqualTo(description)
        Truth.assertThat(playlist.public).isEqualTo(public)
        Truth.assertThat(playlist.owner.displayName).isEqualTo(owner)
        if (tracks != null && playlist is FullPlaylist) {
            val allItems = runBlocking { playlist.tracks.fetchAll<PlaylistTrack>() }
            tracks.zip(allItems).forEach { (trackProperties, playlistTrack) ->
                trackProperties.check(playlistTrack)
            }
        }
    }
}
