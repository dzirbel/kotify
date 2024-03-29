package com.dzirbel.kotify.network.model

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyCopyright(
    /** The copyright text for this album. */
    val text: String,

    /** The type of copyright: C = the copyright, P = the sound recording (performance) copyright. */
    val type: String,
)
