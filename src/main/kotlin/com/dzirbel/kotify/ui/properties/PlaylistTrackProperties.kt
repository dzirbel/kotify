package com.dzirbel.kotify.ui.properties

import com.dzirbel.kotify.db.model.PlaylistTrack
import com.dzirbel.kotify.util.formatDateTime
import java.time.Instant

object PlaylistTrackAddedAtProperty : PropertyByNumber<PlaylistTrack>(title = "Added") {
    // TODO precompute rather than re-parsing each time this is accessed
    override fun toNumber(item: PlaylistTrack): Long {
        return Instant.parse(item.addedAt.orEmpty()).toEpochMilli()
    }

    override fun toString(item: PlaylistTrack): String {
        return formatDateTime(timestamp = toNumber(item), includeTime = false)
    }
}

object PlaylistTrackIndexProperty : PropertyByNumber<PlaylistTrack>(title = "#") {
    override fun toNumber(item: PlaylistTrack) = item.indexOnPlaylist.toInt() + 1
}
