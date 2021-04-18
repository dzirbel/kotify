package com.dzirbel.kotify.properties

import com.dzirbel.kotify.network.model.SavedShow
import com.dzirbel.kotify.network.model.Show
import com.google.common.truth.Truth

data class ShowProperties(
    override val id: String,
    override val name: String,
    val description: String,
    val explicit: Boolean = false,
    val saved: Boolean,
    val addedAt: String? = null,
    private val languages: List<String> = listOf("en-US"),
    private val mediaType: String = "audio"
) : ObjectProperties(type = "show") {
    fun check(show: Show) {
        super.check(show)

        Truth.assertThat(show.description).isEqualTo(description)
        Truth.assertThat(show.explicit).isEqualTo(explicit)
        Truth.assertThat(show.languages).isEqualTo(languages)
        Truth.assertThat(show.mediaType).isEqualTo(mediaType)
    }

    fun check(savedShow: SavedShow) {
        check(savedShow.show)

        Truth.assertThat(addedAt).isNotNull()
        Truth.assertThat(savedShow.addedAt).isEqualTo(addedAt)
    }
}
