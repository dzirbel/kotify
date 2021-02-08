package com.dominiczirbel.network.model

import kotlinx.serialization.Serializable

@Serializable
data class Copyright(
    /** The copyright text for this album. */
    val text: String,

    /** The type of copyright: C = the copyright, P = the sound recording (performance) copyright. */
    val type: String
)
