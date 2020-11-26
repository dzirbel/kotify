package com.dominiczirbel.network

import com.dominiczirbel.network.model.Artist
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout

fun assertThat(actual: Artist): ArtistSubject {
    return assertAbout { metadata, factoryActual: Artist -> ArtistSubject(metadata, factoryActual) }
        .that(actual)
}

class ArtistSubject(failureMetadata: FailureMetadata, private val actual: Artist) :
    Subject(failureMetadata, actual) {

}
