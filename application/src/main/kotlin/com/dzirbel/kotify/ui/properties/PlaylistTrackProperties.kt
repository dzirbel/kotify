package com.dzirbel.kotify.ui.properties

import com.dzirbel.kotify.repository.playlist.PlaylistTrackViewModel
import com.dzirbel.kotify.ui.components.adapter.properties.PropertyByNumber
import com.dzirbel.kotify.util.formatDateTime
import java.util.concurrent.TimeUnit

object PlaylistTrackAddedAtProperty : PropertyByNumber<PlaylistTrackViewModel>(
    title = "Added",
    divisionRange = TimeUnit.DAYS.toMillis(1).toInt(), // divide into divisions per day
) {
    override fun toNumber(item: PlaylistTrackViewModel): Long? = item.addedAtInstant?.toEpochMilli()

    override fun toString(item: PlaylistTrackViewModel): String {
        return toNumber(item)
            ?.let { formatDateTime(timestamp = it, includeTime = false) }
            .orEmpty()
    }
}

object PlaylistTrackIndexProperty :
    PropertyByNumber<PlaylistTrackViewModel>(title = "#", divisionRange = TRACK_INDEX_DIVISION_RANGE) {
    override fun toNumber(item: PlaylistTrackViewModel) = item.indexOnPlaylist + 1
}
