package com.dzirbel.kotify.repository.artist

import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistAlbum
import com.dzirbel.kotify.repository.LazyTransactionStateFlow

data class ArtistViewModel(
    val id: String,
    val uri: String?,
    val name: String,
    val popularity: Int?,
    val largestImage: LazyTransactionStateFlow<String>, // TODO ImageViewModel
    val genres: LazyTransactionStateFlow<List<String>>, // TODO GenreViewModel
    val albums: LazyTransactionStateFlow<List<ArtistAlbum>>, // TODO ArtistAlbumViewModel
) {
    constructor(artist: Artist) : this(
        id = artist.id.value,
        uri = artist.uri,
        name = artist.name,
        popularity = artist.popularity,
        largestImage = LazyTransactionStateFlow(
            transactionName = "load artist ${artist.id.value} largest image",
            initialValue = artist.largestImage.cachedOrNull?.url,
        ) {
            artist.largestImage.value?.url
        },
        genres = LazyTransactionStateFlow(
            transactionName = "load artist ${artist.id.value} genres",
            initialValue = artist.genres.cachedOrNull?.map { it.name },
        ) {
            artist.genres.live.map { it.name }
        },
        albums = LazyTransactionStateFlow(
            transactionName = "load artist ${artist.id.value} albums",
            initialValue = artist.artistAlbums.cachedOrNull,
        ) {
            artist.artistAlbums.live.onEach { artistAlbum ->
                // TODO loadToCache
                artistAlbum.album.loadToCache()
                artistAlbum.album.live.largestImage.loadToCache()
            }
        },
    )
}
