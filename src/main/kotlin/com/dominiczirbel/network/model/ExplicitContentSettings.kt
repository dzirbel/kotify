package com.dominiczirbel.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExplicitContentSettings(
    /** When true, indicates that explicit content should not be played. */
    @SerialName("filter_enabled") val filterEnabled: Boolean,

    /** When true, indicates that the explicit content setting is locked and can’t be changed by the user. */
    @SerialName("filter_locked") val filterLocked: Boolean
)
