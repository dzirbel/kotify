package com.dzirbel.kotify.ui.properties

import com.dzirbel.kotify.db.model.PlaylistTrack
import com.dzirbel.kotify.util.formatDateTime

object PlaylistTrackAddedAtProperty : PropertyByNumber<PlaylistTrack>(title = "Added") {
    override fun toNumber(item: PlaylistTrack): Long? = item.addedAtInstant?.toEpochMilli()

    override fun toString(item: PlaylistTrack): String {
        return toNumber(item)
            ?.let { formatDateTime(timestamp = it, includeTime = false) }
            .orEmpty()
    }
}

object PlaylistTrackIndexProperty : PropertyByNumber<PlaylistTrack>(title = "#") {
    override fun toNumber(item: PlaylistTrack) = item.indexOnPlaylist + 1
}
