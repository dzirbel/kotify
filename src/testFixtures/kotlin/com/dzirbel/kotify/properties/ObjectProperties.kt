package com.dzirbel.kotify.properties

import com.dzirbel.kotify.isNullIf
import com.dzirbel.kotify.network.model.SpotifyObject
import com.google.common.truth.Truth

abstract class ObjectProperties(
    private val type: String,
    private val hrefNull: Boolean = false,
    private val uriNull: Boolean = false
) {
    abstract val id: String?
    abstract val name: String

    protected fun check(obj: SpotifyObject) {
        Truth.assertThat(obj.id).isEqualTo(id)
        Truth.assertThat(obj.name).isEqualTo(name)
        Truth.assertThat(obj.type).isEqualTo(type)
        Truth.assertThat(obj.href).isNullIf(hrefNull)
        Truth.assertThat(obj.uri).isNullIf(uriNull)
    }
}
