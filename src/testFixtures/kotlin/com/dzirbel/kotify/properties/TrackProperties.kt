package com.dzirbel.kotify.properties

import com.dzirbel.kotify.network.model.PlaylistTrack
import com.dzirbel.kotify.network.model.SavedTrack
import com.dzirbel.kotify.network.model.Track
import com.google.common.truth.Truth

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

        Truth.assertThat(track.artists.map { it.name }).containsExactlyElementsIn(artistNames)
        Truth.assertThat(track.trackNumber).isEqualTo(trackNumber)
        Truth.assertThat(track.discNumber).isEqualTo(discNumber)
        Truth.assertThat(track.durationMs).isAtLeast(0)
        Truth.assertThat(track.explicit).isEqualTo(explicit)
        Truth.assertThat(track.isLocal).isEqualTo(isLocal)
        Truth.assertThat(track.externalUrls).isNotNull()
    }

    fun check(playlistTrack: PlaylistTrack) {
        check(playlistTrack.track)

        Truth.assertThat(playlistTrack.isLocal).isEqualTo(isLocal)
        addedBy?.let { Truth.assertThat(playlistTrack.addedBy.id).isEqualTo(it) }
        addedAt?.let { Truth.assertThat(playlistTrack.addedAt).isEqualTo(it) }
    }

    fun check(savedTrack: SavedTrack) {
        check(savedTrack.track)

        Truth.assertThat(addedAt).isNotNull()
        Truth.assertThat(savedTrack.addedAt).isEqualTo(addedAt)
    }
}
