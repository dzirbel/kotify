package com.dzirbel.kotify.properties

import com.dzirbel.kotify.network.model.SavedShow
import com.dzirbel.kotify.network.model.Show
import com.google.common.truth.Truth.assertThat

data class ShowProperties(
    override val id: String,
    override val name: String,
    val description: String,
    val explicit: Boolean = false,
    val saved: Boolean,
    val addedAt: String? = null,
    private val languages: List<String>? = null,
    private val mediaType: String = "audio"
) : ObjectProperties(type = "show") {
    fun check(show: Show) {
        super.check(show)

        assertThat(show.description).isEqualTo(description)
        assertThat(show.explicit).isEqualTo(explicit)
        assertThat(show.languages).isEqualTo(languages)
        assertThat(show.mediaType).isEqualTo(mediaType)

        languages?.let { assertThat(show.languages).isEqualTo(it) }
    }

    fun check(savedShow: SavedShow) {
        check(savedShow.show)

        assertThat(addedAt).isNotNull()
        assertThat(savedShow.addedAt).isEqualTo(addedAt)
    }
}
