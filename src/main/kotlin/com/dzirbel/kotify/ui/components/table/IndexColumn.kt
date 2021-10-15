package com.dzirbel.kotify.ui.components.table

import com.dzirbel.kotify.network.model.PlaylistTrack

/**
 * A standard [Column] which displays a 1-based index for each row.
 */
object IndexColumn : ColumnByNumber<PlaylistTrack>(header = "#", sortable = false) {
    override fun toNumber(item: PlaylistTrack, index: Int) = index + 1
}
