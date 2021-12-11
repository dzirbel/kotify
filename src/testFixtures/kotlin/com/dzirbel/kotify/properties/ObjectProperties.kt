package com.dzirbel.kotify.properties

import com.dzirbel.kotify.isNullIf
import com.dzirbel.kotify.network.model.SpotifyObject
import com.google.common.truth.Truth.assertThat

abstract class ObjectProperties(
    private val type: String,
    private val hrefNull: Boolean = false,
    private val uriNull: Boolean = false
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
