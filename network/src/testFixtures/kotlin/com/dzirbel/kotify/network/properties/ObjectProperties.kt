package com.dzirbel.kotify.network.properties

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.dzirbel.kotify.network.model.SpotifyObject
import com.dzirbel.kotify.util.isNullIf

abstract class ObjectProperties(
    private val type: String,
    private val hrefNull: Boolean = false,
    private val uriNull: Boolean = false,
) {
    abstract val id: String?
    abstract val name: String

    protected fun check(obj: SpotifyObject) {
        assertThat(obj.id).isEqualTo(id)
        assertThat(obj.name).isEqualTo(name)
        assertThat(obj.type).isEqualTo(type)
        assertThat(obj.href).isNullIf(hrefNull)
        assertThat(obj.uri).isNullIf(uriNull)
    }
}
