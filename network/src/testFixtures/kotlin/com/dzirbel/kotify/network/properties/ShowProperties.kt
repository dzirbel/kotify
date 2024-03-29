package com.dzirbel.kotify.network.properties

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.dzirbel.kotify.network.model.SpotifySavedShow
import com.dzirbel.kotify.network.model.SpotifyShow

data class ShowProperties(
    override val id: String,
    override val name: String,
    val description: String,
    val explicit: Boolean = false,
    val saved: Boolean,
    val addedAt: String? = null,
    private val languages: List<String>? = null,
    private val mediaType: String = "audio",
) : ObjectProperties(type = "show") {
    fun check(show: SpotifyShow) {
        super.check(show)

        assertThat(show.description).isEqualTo(description)
        assertThat(show.explicit).isEqualTo(explicit)
        assertThat(show.mediaType).isEqualTo(mediaType)

        languages?.let { assertThat(show.languages).isEqualTo(it) }
    }

    fun check(savedShow: SpotifySavedShow) {
        check(savedShow.show)

        assertThat(addedAt).isNotNull()
        assertThat(savedShow.addedAt).isEqualTo(addedAt)
    }
}
