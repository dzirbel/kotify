package com.dzirbel.kotify.properties

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isNotNull
import com.dzirbel.kotify.containsExactlyElementsOfInAnyOrder
import com.dzirbel.kotify.network.model.SpotifyPlaylistTrack
import com.dzirbel.kotify.network.model.SpotifySavedTrack
import com.dzirbel.kotify.network.model.SpotifyTrack

data class TrackProperties(
    override val id: String?,
    override val name: String,
    val artistNames: Set<String>,
    val discNumber: Int = 1,
    val explicit: Boolean = false,
    val isLocal: Boolean = false,
    val trackNumber: Int,
    val addedBy: String? = null,
    val addedAt: String? = null,
) : ObjectProperties(type = "track", hrefNull = isLocal) {
    fun check(track: SpotifyTrack) {
        super.check(track)

        assertThat(track.artists.map { it.name }).containsExactlyElementsOfInAnyOrder(artistNames)
        assertThat(track.trackNumber).isEqualTo(trackNumber)
        assertThat(track.discNumber).isEqualTo(discNumber)
        assertThat(track.durationMs).isGreaterThanOrEqualTo(0)
        assertThat(track.explicit).isEqualTo(explicit)
        assertThat(track.isLocal).isEqualTo(isLocal)
        assertThat(track.externalUrls).isNotNull()
    }

    fun check(playlistTrack: SpotifyPlaylistTrack) {
        check(requireNotNull(playlistTrack.track))

        assertThat(playlistTrack.isLocal).isEqualTo(isLocal)
        addedBy?.let { assertThat(playlistTrack.addedBy.id).isEqualTo(it) }
        addedAt?.let { assertThat(playlistTrack.addedAt).isEqualTo(it) }
    }

    fun check(savedTrack: SpotifySavedTrack) {
        check(savedTrack.track)

        assertThat(addedAt).isNotNull()
        assertThat(savedTrack.addedAt).isEqualTo(addedAt)
    }
}
