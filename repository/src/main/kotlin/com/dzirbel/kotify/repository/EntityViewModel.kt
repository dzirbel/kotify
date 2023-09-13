package com.dzirbel.kotify.repository

import java.time.Instant

interface EntityViewModel {
    val id: String
    val uri: String?
    val name: String

    val updatedTime: Instant
    val fullUpdatedTime: Instant?
}
