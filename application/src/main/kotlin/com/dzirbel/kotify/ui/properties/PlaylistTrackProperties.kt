package com.dzirbel.kotify.ui.properties

import com.dzirbel.kotify.db.model.PlaylistTrack
import com.dzirbel.kotify.ui.components.adapter.properties.PropertyByNumber
import com.dzirbel.kotify.util.formatDateTime
import java.util.concurrent.TimeUnit

object PlaylistTrackAddedAtProperty : PropertyByNumber<PlaylistTrack>(
    title = "Added",
    divisionRange = TimeUnit.DAYS.toMillis(1).toInt(), // divide into divisions per day
) {
    override fun toNumber(item: PlaylistTrack): Long? = item.addedAtInstant?.toEpochMilli()

    override fun toString(item: PlaylistTrack): String {
        return toNumber(item)
            ?.let { formatDateTime(timestamp = it, includeTime = false) }
            .orEmpty()
    }
}

object PlaylistTrackIndexProperty :
    PropertyByNumber<PlaylistTrack>(title = "#", divisionRange = TRACK_INDEX_DIVISION_RANGE) {
    override fun toNumber(item: PlaylistTrack) = item.indexOnPlaylist + 1
}
