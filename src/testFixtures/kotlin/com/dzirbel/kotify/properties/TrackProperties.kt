package com.dzirbel.kotify.properties

import com.dzirbel.kotify.network.model.PlaylistTrack
import com.dzirbel.kotify.network.model.SavedTrack
import com.dzirbel.kotify.network.model.Track
import com.google.common.truth.Truth.assertThat

data class TrackProperties(
    override val id: String?,
    override val name: String,
    val artistNames: Set<String>,
    val discNumber: Int = 1,
    val explicit: Boolean = false,
    val isLocal: Boolean = false,
    val trackNumber: Int,
    val addedBy: String? = null,
    val addedAt: String? = null
) : ObjectProperties(type = "track", hrefNull = isLocal) {
    fun check(track: Track) {
        super.check(track)

        assertThat(track.artists.map { it.name }).containsExactlyElementsIn(artistNames)
        assertThat(track.trackNumber).isEqualTo(trackNumber)
        assertThat(track.discNumber).isEqualTo(discNumber)
        assertThat(track.durationMs).isAtLeast(0)
        assertThat(track.explicit).isEqualTo(explicit)
        assertThat(track.isLocal).isEqualTo(isLocal)
        assertThat(track.externalUrls).isNotNull()
    }

    fun check(playlistTrack: PlaylistTrack) {
        check(playlistTrack.track)

        assertThat(playlistTrack.isLocal).isEqualTo(isLocal)
        addedBy?.let { assertThat(playlistTrack.addedBy.id).isEqualTo(it) }
        addedAt?.let { assertThat(playlistTrack.addedAt).isEqualTo(it) }
    }

    fun check(savedTrack: SavedTrack) {
        check(savedTrack.track)

        assertThat(addedAt).isNotNull()
        assertThat(savedTrack.addedAt).isEqualTo(addedAt)
    }
}
