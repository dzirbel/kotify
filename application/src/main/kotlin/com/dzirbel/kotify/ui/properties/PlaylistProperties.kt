package com.dzirbel.kotify.ui.properties

import com.dzirbel.kotify.repository.playlist.PlaylistViewModel
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.SortableProperty
import com.dzirbel.kotify.ui.components.adapter.compareBy
import com.dzirbel.kotify.ui.components.adapter.compareByNullable

object PlaylistNameProperty : SortableProperty<PlaylistViewModel> {
    override val title = "Name"

    override fun compare(sortOrder: SortOrder, first: PlaylistViewModel, second: PlaylistViewModel): Int {
        return sortOrder.compareBy(first, second) { it.name }
    }
}

object PlaylistLibraryOrderProperty : SortableProperty<PlaylistViewModel> {
    override val title = "Custom Order"

    override fun compare(sortOrder: SortOrder, first: PlaylistViewModel, second: PlaylistViewModel): Int {
        return sortOrder.compareByNullable(first, second) { it.libraryOrder }
    }
}
