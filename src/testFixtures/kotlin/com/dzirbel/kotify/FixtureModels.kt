package com.dzirbel.kotify

import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.network.model.FullSpotifyArtist
import com.dzirbel.kotify.network.model.SpotifyArtist
import com.dzirbel.kotify.network.model.SpotifyExternalUrl
import com.dzirbel.kotify.network.model.SpotifyFollowers

/**
 * A collection of convenience functions which construct various model classes with mocked data for use in tests.
 */
object FixtureModels {
    fun databaseArtists(count: Int): List<Artist> {
        val networkArtists = networkArtists(count = count)
        return testTransaction {
            networkArtists.map { requireNotNull(Artist.from(it)) }
        }
    }

    fun networkArtists(count: Int): List<SpotifyArtist> {
        return List(count) { index ->
            networkArtist(id = "artist-$index", name = "Artist $index")
        }
    }

    fun networkArtist(
        id: String = "artist",
        name: String = "Artist",
        followers: Int = 100,
        popularity: Int = 50,
    ): SpotifyArtist {
        return FullSpotifyArtist(
            id = id,
            name = name,
            externalUrls = SpotifyExternalUrl(),
            href = "href",
            type = "artist",
            uri = "uri",
            followers = SpotifyFollowers(total = followers),
            genres = listOf("genre"),
            images = listOf(),
            popularity = popularity,
        )
    }
}
